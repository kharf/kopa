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
import java.io.File
import java.nio.ByteBuffer

private val logger = KotlinLogging.logger { }

@Serializable
data class Dependency(
    val name: String,
    val group: String,
    val version: String,
    val type: Type
) {
    enum class Type {
        CLASSES, SOURCES;
    }

    val fullName: String = when (type) {
        Type.CLASSES -> "$name-$version"
        Type.SOURCES -> "$name-$version-sources"
    }
    val jarName: String = "$fullName.jar"
}

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
                !File("${artifactStorage.path}/${dependency.jarName}").exists()
            }
        )
        val resolvedDependencies = dependencies.minus(unresolvedDependencies)
        val resolvedArtifacts = resolvedDependencies.map { dependency -> Artifact(Location("${artifactStorage.path}/${dependency.jarName}"), Artifact.Type.valueOf(dependency.type.name)) }
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
        val artifacts = dependencies.map { dependency ->
            logger.info { dependency.fullName }
            val filePaths = dependency.group.split(".")
            val urlPathBuilder = StringBuilder("https://search.maven.org/classic/remotecontent?filepath=")
            filePaths.forEach { path ->
                urlPathBuilder.append("$path/")
            }
            urlPathBuilder.append("${dependency.name}/${dependency.version}/")
            val artifactJarName = dependency.jarName
            urlPathBuilder.append(artifactJarName)
            val artifactPath = urlPathBuilder.toString()
            val location = artifactStorage.store(client.resolve(artifactPath), artifactJarName)
            Artifact(location, Artifact.Type.valueOf(dependency.type.name))
        }
        logger.info { "resolved maven dependencies" }
        return Artifacts(artifacts)
    }
}
