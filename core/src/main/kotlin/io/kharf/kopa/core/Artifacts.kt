package io.kharf.kopa.core

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.Url
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.io.File
import java.nio.ByteBuffer

private val logger = KotlinLogging.logger { }

interface DependencyResolverClient {
    suspend fun resolve(url: String): Flow<ByteBuffer>
}

class HttpDependencyResolverClient(
    private val client: HttpClient = HttpClient(CIO)
) : DependencyResolverClient {
    override suspend fun resolve(url: String): Flow<ByteBuffer> {
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

class MavenDependencyResolver(
    private val client: DependencyResolverClient = HttpDependencyResolverClient()
) : DependencyResolver {
    override suspend fun resolve(
        dependencies: Dependencies,
        store: suspend (Flow<ByteBuffer>, String) -> Location
    ): Artifacts {
        logger.info { "resolving dependencies" }
        val artifacts = dependencies.map { dependency ->
            logger.info { "resolving ${dependency.name}-${dependency.version}" }
            val filePaths = dependency.group.split(".")
            val urlPathBuilder = StringBuilder("https://search.maven.org/classic/remotecontent?filepath=")
            filePaths.forEach { path ->
                urlPathBuilder.append("$path/")
            }
            urlPathBuilder.append("${dependency.name}/${dependency.version}/")
            val artifact = "${dependency.name}-${dependency.version}.jar"
            urlPathBuilder.append(artifact)
            val artifactPath = urlPathBuilder.toString()
            Artifact(store(client.resolve(artifactPath), artifact))
        }
        logger.info { "resolved dependencies" }
        return Artifacts(artifacts)
    }
}

@JvmInline
value class Location(val location: String)

interface DependencyResolver {
    suspend fun resolve(dependencies: Dependencies, store: suspend (Flow<ByteBuffer>, String) -> Location): Artifacts
}

interface ArtifactStorage {
    suspend fun store(artifactContent: Flow<ByteBuffer>, artifactName: String): Location
}

class FileSystemArtifactStorage(
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) : ArtifactStorage {
    private val kopaDir: File by lazy {
        val dir = File("${System.getProperty("user.home")}/.kopa")
        fileSystem.createDirectories(dir.toOkioPath())
        dir
    }

    override suspend fun store(artifactContent: Flow<ByteBuffer>, artifactName: String): Location =
        withContext(Dispatchers.IO) {
            logger.info { "storing $artifactName on file system" }
            val artifactFile = File("${kopaDir.absolutePath}/$artifactName")
            fileSystem.write(artifactFile.toOkioPath()) {
                artifactContent.collect { buffer ->
                    logger.trace { "collecting $buffer" }
                    this.write(buffer)
                    logger.trace { "stored ${artifactFile.length()}" }
                }
            }
            Location(artifactFile.absolutePath)
        }
}

class Artifacts(private val artifacts: List<Artifact>) : List<Artifact> by artifacts
data class Artifact(val location: Location)
