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
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import java.io.File
import java.io.StringReader
import java.nio.ByteBuffer
import org.apache.maven.model.Dependency as MavenModelDependency

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

fun List<Dependency>.toDependencies(): Dependencies = Dependencies(this)

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

class MavenDependencyResolver(
    private val client: HttpDependencyResolverClient = HttpDependencyResolverClient(),
    private val artifactStorage: ArtifactStorage
) : DependencyResolver {
    override suspend fun resolve(dependencies: Dependencies): Artifacts {
        logger.info { "resolving maven dependencies" }
        val artifacts = resolveUnlocked(dependencies, emptyMap())
        logger.info { "resolved maven dependencies" }
        return Artifacts(artifacts.values.toList())
    }

    private suspend fun resolveUnlocked(
        dependencies: Dependencies,
        lockedArtifactsByGroupAndName: Map<String, Artifact>
    ): Map<String, Artifact> {
        val newLockedArtifacts =
            dependencies.filterNot { it.type == Dependency.Type.POM }.fold(lockedArtifactsByGroupAndName.toMutableMap()) { map, dependency ->
                val dependencyPath = "${artifactStorage.path}/${dependency.getGroupPath()}/${dependency.fullName}"
                val location = if (File(dependencyPath).exists()) {
                    Location(dependencyPath)
                } else {
                    val artifactContent = fetch(dependency)
                    artifactStorage.store(artifactContent, dependency.fullName, dependency.getGroupPath())
                }
                val artifact = Artifact(
                    name = dependency.name,
                    group = dependency.group,
                    version = dependency.version,
                    location = location,
                    type = Artifact.Type.valueOf(dependency.type.name)
                )
                map.putIfAbsent(
                    "${dependency.group}.${dependency.name}",
                    artifact
                )
                map
            }
        return resolveTransitive(dependencies, newLockedArtifacts)
    }

    private suspend fun resolveTransitive(
        dependencies: Dependencies,
        lockedArtifactsByGroupAndName: Map<String, Artifact>
    ): Map<String, Artifact> {
        val newLockedArtifacts = mutableMapOf<String, Artifact>()
        dependencies.filter { it.type == Dependency.Type.POM }.forEach { dependency ->
            val model = fetchPom(dependency)
            val parentPomModel = if (model.parent != null) {
                fetchPom(
                    Dependency(
                        name = model.parent.artifactId,
                        group = model.parent.groupId,
                        version = model.parent.version,
                        type = Dependency.Type.POM,
                        scope = Dependency.Scope.COMPILE
                    )
                )
            } else null
            val transitiveDependencies = Dependencies(
                model.dependencies.filterNot { it.scope != null && it.scope == "test" }
                    .filter { lockedArtifactsByGroupAndName["${it.groupId}.${it.artifactId}"] == null }.flatMap {
                        val scope = it.scope?.lowercase()
                        val version = if (it.version == null) {
                            parentPomModel!!.dependencyManagement.dependencies.find { parentPomDep -> parentPomDep.groupId == it.groupId && parentPomDep.artifactId == it.artifactId }!!.version
                        } else if (it.version == "\${project.version}") {
                            parentPomModel!!.version
                        } else if (it.version.startsWith("\${")) {
                            model.properties.getProperty(it.version.removePrefix("\${").removeSuffix("}"))
                        } else {
                            it.version
                        }
                        val groupId = if (it.groupId == "\${project.groupId}") {
                            parentPomModel!!.groupId
                        } else {
                            it.groupId
                        }
                        listOf(
                            Dependency(
                                name = it.artifactId,
                                group = groupId,
                                version = version,
                                scope = when (scope) {
                                    "compile", null -> Dependency.Scope.COMPILE
                                    "runtime" -> Dependency.Scope.RUNTIME
                                    else -> Dependency.Scope.COMPILE
                                },
                                type = Dependency.Type.CLASSES
                            ),
                            Dependency(
                                name = it.artifactId,
                                group = groupId,
                                version = version,
                                scope = Dependency.Scope.COMPILE,
                                type = Dependency.Type.SOURCES
                            ),
                            Dependency(
                                name = it.artifactId,
                                group = groupId,
                                version = version,
                                scope = Dependency.Scope.COMPILE,
                                type = Dependency.Type.POM
                            )
                        )
                    }
            )
            newLockedArtifacts.putAll(resolveUnlocked(transitiveDependencies, lockedArtifactsByGroupAndName))
        }
        return newLockedArtifacts
    }

    private suspend fun fetchPom(dependency: Dependency): Model {
        val pomContent = fetch(dependency)
        artifactStorage.store(pomContent, dependency.fullName, dependency.getGroupPath())
        val pomBuilder = StringBuilder()
        pomContent.collect {
            pomBuilder.append(String(it.array()))
        }
        val pomString = pomBuilder.toString()
        val pomReader = MavenXpp3Reader()
        return pomReader.read(StringReader(pomString))
    }

    private suspend fun resolveVersion(pom: Model, parentPom: Model?, dependency: MavenModelDependency): String {
        return if (dependency.version == null) {
            val parentPomDependency =
                parentPom!!.dependencyManagement.dependencies.find { parentPomDep -> parentPomDep.groupId == dependency.groupId && parentPomDep.artifactId == dependency.artifactId }
            val parentParentPom = parentPom.parent?.let {
                fetchPom(
                    Dependency(
                        name = it.artifactId,
                        group = it.groupId,
                        version = it.version,
                        type = Dependency.Type.POM,
                        scope = Dependency.Scope.COMPILE
                    )
                )
            }
            resolveVersion(
                parentPom,
                parentParentPom,
                parentPomDependency!!
            )
        } else if (dependency.version == "\${project.version}") {
            pom.version ?: parentPom!!.version
        } else if (dependency.version.startsWith("\${")) {
            val removeSuffix = dependency.version.removePrefix("\${").removeSuffix("}")
            pom.properties.getProperty(removeSuffix) ?: parentPom!!.properties.getProperty()
        } else {
            dependency.version
        }
    }

    private suspend fun fetch(dependency: Dependency): Flow<ByteBuffer> {
        logger.info { dependency.fullName }
        val groupPath = dependency.getGroupPath()
        val urlPathBuilder = StringBuilder("https://search.maven.org/classic/remotecontent?filepath=")
        urlPathBuilder.append("$groupPath/")
        urlPathBuilder.append("${dependency.name}/${dependency.version}/")
        val artifactFullName = dependency.fullName
        urlPathBuilder.append(artifactFullName)
        val artifactPath = urlPathBuilder.toString()
        return client.resolve(artifactPath)
    }
}
