package io.kharf.kopa.core

import failgood.describe
import kotlinx.serialization.ExperimentalSerializationApi
import okio.ExperimentalFileSystem
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.io.File
import kotlin.io.path.Path

class FakeFileManifestInterpreter : ManifestInterpreter<File> {
    override fun interpret(manifest: File): Interpretation {
        return Interpretation(Dependencies(emptyList()), "")
    }
}

@ExperimentalSerializationApi
@Testable
class KotlinJvmBuilderTest {
    @ExperimentalFileSystem
    val context = describe(KotlinJvmBuilder::class) {
        val subject = KotlinJvmBuilder(FakeFileManifestInterpreter())
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
                val code = subject.build(path.toPath())
                path.deleteRecursively()
                expectThat(code).isEqualTo(ExitCode.OK)
            }

            it("should return an erroneous ExitCode if a container could not be built") {
                val path = Path("build/non-existing-builder-testsample")
                val code = subject.build(path)
                expectThat(code).isEqualTo(ExitCode.COMPILATION_ERROR)
            }
        }
    }
}
