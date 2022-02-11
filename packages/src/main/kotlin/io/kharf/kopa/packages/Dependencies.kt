package io.kharf.kopa.packages

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.Url
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import java.io.File
import java.io.StringReader
import java.nio.ByteBuffer

private val logger = KotlinLogging.logger { }

@Serializable
data class Dependency(
    val name: String,
    val group: String,
    val version: String,
    val type: Type,
    val scope: Scope
) {
    enum class Type {
        CLASSES, SOURCES, POM;
    }

    enum class Scope {
        COMPILE, RUNTIME;
    }

    val fullName: String = when (type) {
        Type.CLASSES -> "$name-$version.jar"
        Type.SOURCES -> "$name-$version-sources.jar"
        Type.POM -> "$name-$version.pom"
    }
}

fun Dependency.getGroupPath() = group.replace(".", "/")

@Serializable
data class Dependencies(val list: List<Dependency>) : List<Dependency> by list

class HttpDependencyResolverClient(private val client: HttpClient = HttpClient(CIO)) {
    suspend fun resolve(url: String): Flow<ByteBuffer> {
        logger.info { "downloading $url" }
        val response = client.get(Url(url))
        return flow<ByteBuffer> {
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong(), 0)
                while (!packet.isEmpty) {
                    val bytes = packet.readBytes()
                    val buffer = ByteBuffer.wrap(bytes)
                    logger.trace { "emitting ${bytes.size}" }
                    emit(buffer)
                }
            }
        }
    }
}

interface DependencyResolver {
    suspend fun resolve(dependencies: Dependencies): Artifacts
}

class CachedDependencyResolver(private val artifactStorage: ArtifactStorage, private val dependencyResolver: DependencyResolver) :
    DependencyResolver {
    override suspend fun resolve(dependencies: Dependencies): Artifacts {
        logger.info { "resolving local dependencies" }
        val unresolvedDependencies = Dependencies(
            dependencies.filter { dependency ->
                !File("${artifactStorage.path}/${dependency.getGroupPath()}/${dependency.fullName}").exists()
            }
        )
        val resolvedDependencies = dependencies.minus(unresolvedDependencies)
        val resolvedArtifacts = resolvedDependencies.map { dependency ->
            Artifact(
                Location("${artifactStorage.path}/${dependency.getGroupPath()}/${dependency.fullName}"),
                Artifact.Type.valueOf(dependency.type.name)
            )
        }
        val newArtifacts = if (!unresolvedDependencies.isEmpty()) dependencyResolver.resolve(unresolvedDependencies) else emptyList()
        logger.info { "resolved local dependencies" }
        return Artifacts(newArtifacts.plus(resolvedArtifacts))
    }
}

class MavenDependencyResolver(
    private val client: HttpDependencyResolverClient = HttpDependencyResolverClient(),
    private val artifactStorage: ArtifactStorage
) : DependencyResolver {
    override suspend fun resolve(dependencies: Dependencies): Artifacts {
        logger.info { "resolving maven dependencies" }
        val artifacts = dependencies.flatMap { dependency ->
            logger.info { dependency.fullName }
            val groupPath = dependency.group.replace(".", "/")
            val urlPathBuilder = StringBuilder("https://search.maven.org/classic/remotecontent?filepath=")
            urlPathBuilder.append("$groupPath/")
            urlPathBuilder.append("${dependency.name}/${dependency.version}/")
            val artifactFullName = dependency.fullName
            urlPathBuilder.append(artifactFullName)
            val artifactPath = urlPathBuilder.toString()
            val artifactContent = client.resolve(artifactPath)
            val transitiveArtifacts = if (dependency.type == Dependency.Type.POM) {
                val pomBuilder = StringBuilder()
                artifactContent.collect {
                    pomBuilder.append(String(it.array()))
                }
                val pom = pomBuilder.toString()
                val reader = MavenXpp3Reader()
                val model = reader.read(StringReader(pom))
                val transitiveDependencies = Dependencies(
                    model.dependencies.flatMap {
                        listOf(
                            Dependency(
                                name = it.artifactId,
                                group = it.groupId,
                                version = it.version,
                                scope = when (it.scope.lowercase()) {
                                    "compile" -> Dependency.Scope.COMPILE
                                    "runtime" -> Dependency.Scope.RUNTIME
                                    else -> Dependency.Scope.COMPILE
                                },
                                type = Dependency.Type.CLASSES
                            ),
                            Dependency(
                                name = it.artifactId,
                                group = it.groupId,
                                version = it.version,
                                scope = Dependency.Scope.COMPILE,
                                type = Dependency.Type.SOURCES
                            ),
                            Dependency(
                                name = it.artifactId,
                                group = it.groupId,
                                version = it.version,
                                scope = Dependency.Scope.COMPILE,
                                type = Dependency.Type.POM
                            )
                        )
                    }
                )
                resolve(transitiveDependencies)
            } else emptyList()
            val location = artifactStorage.store(artifactContent, artifactFullName, groupPath)
            transitiveArtifacts.plus(Artifact(location, Artifact.Type.valueOf(dependency.type.name)))
        }
        logger.info { "resolved maven dependencies" }
        return Artifacts(artifacts)
    }
}
