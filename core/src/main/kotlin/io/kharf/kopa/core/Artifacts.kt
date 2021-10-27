package io.kharf.kopa.core

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import java.io.File
import java.net.URL

object MavenArtifactResolver : ArtifactResolver {
    override suspend fun resolve(dependencies: Dependencies): KopaArtifact {
        val client = HttpClient(CIO)
        val response: HttpResponse =
            client.get {
                url(URL("https://search.maven.org/classic/remotecontent?filepath=org/jetbrains/kotlin/kotlin-stdlib/1.6.0-RC/kotlin-stdlib-1.6.0-RC.jar"))
            }

        File("/home/kharf/code/kopa/.tmp/hello/build").mkdirs()
        File("/home/kharf/code/kopa/.tmp/hello/build/classpath").mkdirs()
        response.content.copyAndClose(File("/home/kharf/code/kopa/.tmp/hello/build/classpath/kotlin-stdlib-1.6.0-RC.jar").writeChannel())
        return KopaArtifact("bla")
    }
}

interface ArtifactResolver {
    suspend fun resolve(dependencies: Dependencies): KopaArtifact
}

data class KopaArtifact(val name: String)
