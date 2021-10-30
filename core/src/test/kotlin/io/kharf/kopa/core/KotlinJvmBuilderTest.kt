package io.kharf.kopa.core

import failgood.describe
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.ExperimentalSerializationApi
import okio.ExperimentalFileSystem
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.io.File
import java.nio.ByteBuffer
import kotlin.io.path.Path

class FakeArtifactResolver : DependencyResolver {
    override suspend fun resolve(
        dependencies: Dependencies,
        store: suspend (Flow<ByteBuffer>, String) -> Location
    ): Artifacts = Artifacts(emptyList())
}

@ExperimentalSerializationApi
@Testable
class KotlinJvmBuilderTest {
    @ExperimentalFileSystem
    val context = describe(KotlinJvmBuilder::class) {
        val subject = KotlinJvmBuilder
        describe(KotlinJvmBuilder::build.toString()) {
            it("should build a simple container") {
                val path = File("build/builder-testsample")
                path.mkdir()
                val mainFilePath = "${path.path}/src/Main.kt"
                val srcPath = File("${path.path}/src/")
                srcPath.mkdir()
                File(mainFilePath).writeText(
                    "fun main() {\n" +
                        "}"
                )
                val code = subject.build(
                    path.toPath(),
                    FakeArtifactResolver().resolve(Dependencies(emptyList())) { _, _ ->
                        Location("")
                    },
                )
                path.deleteRecursively()
                expectThat(code).isEqualTo(ExitCode.OK)
            }

            it("should return an erroneous ExitCode if a container could not be built") {
                val path = Path("build/non-existing-builder-testsample")
                val code = subject.build(
                    path,
                    FakeArtifactResolver().resolve(Dependencies(emptyList())) { _, _ ->
                        Location("")
                    },
                )
                expectThat(code).isEqualTo(ExitCode.COMPILATION_ERROR)
            }
        }
    }
}
