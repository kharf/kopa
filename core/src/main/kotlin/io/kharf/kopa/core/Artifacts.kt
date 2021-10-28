package io.kharf.kopa.core

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.util.toByteArray
import mu.KotlinLogging
import okio.ExperimentalFileSystem
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.io.File
import java.net.URL

private val logger = KotlinLogging.logger { }

@ExperimentalFileSystem
class MavenArtifactResolver(
    private val fileSystem: FileSystem = FileSystem.SYSTEM
) : ArtifactResolver {
    override suspend fun resolve(dependencies: Dependencies): Artifacts {
        logger.info { "resolving dependencies" }
        val client = HttpClient(CIO)
        val kopaDir = File("${System.getProperty("user.home")}/.kopa")
        fileSystem.createDirectories(kopaDir.toOkioPath())
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
            val response: HttpResponse =
                client.get {
                    url(URL(artifactPath))
                }
            val artifactFile = File("${kopaDir.absolutePath}/$artifact")
            fileSystem.write(artifactFile.toOkioPath()) {
                this.write(response.content.toByteArray())
            }
            Artifact(artifactFile.absolutePath)
        }
        logger.info { "resolved dependencies" }
        return Artifacts(artifacts)
    }
}

interface ArtifactResolver {
    suspend fun resolve(dependencies: Dependencies): Artifacts
}

class Artifacts(private val artifacts: List<Artifact>) : List<Artifact> by artifacts
data class Artifact(val location: String)
