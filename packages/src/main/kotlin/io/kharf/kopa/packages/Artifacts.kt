package io.kharf.kopa.packages

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.io.File
import java.nio.ByteBuffer

private val logger = KotlinLogging.logger { }

@JvmInline
value class Location(val location: String)
class Artifacts(private val artifacts: List<Artifact>) : List<Artifact> by artifacts
data class Artifact(val location: Location)

interface ArtifactStorage {
    val path: String
    suspend fun store(artifactContent: Flow<ByteBuffer>, artifactName: String): Location
}

class FileSystemArtifactStorage(
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) : ArtifactStorage {
    override val path: String by lazy {
        val dir = File("${System.getProperty("user.home")}/.kopa/packages")
        fileSystem.createDirectories(dir.toOkioPath())
        dir.absolutePath
    }

    override suspend fun store(artifactContent: Flow<ByteBuffer>, artifactName: String): Location {
        logger.info { "----- storing $artifactName on file system" }
        val artifactFile = File("$path/$artifactName")
        fileSystem.write(artifactFile.toOkioPath()) {
            artifactContent.collect { buffer ->
                logger.trace { "collecting $buffer" }
                withContext(Dispatchers.IO) {
                    this@write.write(buffer)
                }
                logger.trace { "----- stored ${artifactFile.length()}" }
            }
        }
        return Location(artifactFile.absolutePath)
    }
}
