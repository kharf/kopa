package io.kharf.kopa.compiler

import failgood.Test
import failgood.describe
import io.kharf.kopa.packages.Artifact
import io.kharf.kopa.packages.Artifacts
import io.kharf.kopa.packages.Location
import io.kharf.kopa.test.FakeDependencyResolver
import io.kharf.kopa.test.FakeFileManifestInterpreter
import kotlinx.serialization.ExperimentalSerializationApi
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.io.File
import kotlin.io.path.Path

@ExperimentalSerializationApi
@Test
class KotlinJvmBuilderTest {
    val context = describe(KotlinJvmBuilder::class) {
        val subject = KotlinJvmBuilder(
            FakeFileManifestInterpreter(),
            FakeDependencyResolver(
                Artifacts(
                    listOf(
                        Artifact(Location(this::class.java.classLoader.getResource("testDependencies/kopa.jar").path), Artifact.Type.CLASSES),
                        Artifact(Location("${System.getProperty("user.home")}/.kopa/artifacts/kotlin-stdlib-1.6.10.jar"), Artifact.Type.CLASSES)
                    )
                )
            )
        )
        describe(KotlinJvmBuilder::build.toString()) {
            it("should build a simple package") {
                val path = File("build/builder-testsample")
                path.mkdirs()
                val mainFilePath = "${path.path}/src/Main.kt"
                val srcPath = File("${path.path}/src/")
                srcPath.mkdirs()
                File(mainFilePath).writeText(
                    "fun main() {\n" +
                        "}"
                )
                val code = subject.build(
                    path.toPath()
                )
                path.deleteRecursively()
                expectThat(code).isEqualTo(ExitCode.OK)
            }

            it("should build a package with a dependency") {
                val path = File("build/builder-testsample2")
                path.mkdirs()
                val mainFilePath = "${path.path}/src/Main.kt"
                val srcPath = File("${path.path}/src/")
                srcPath.mkdirs()
                File(mainFilePath).writeText(
                    "fun main() {\n" +
                        "println(\"Hello World\")\n" +
                        "Main().helloWorld()" +
                        "}"
                )
                val code = subject.build(
                    path.toPath()
                )
                path.deleteRecursively()
                expectThat(code).isEqualTo(ExitCode.OK)
            }

            it("should return an erroneous ExitCode if a package could not be built") {
                val path = Path("build/non-existing-builder-testsample")
                val code = subject.build(
                    path
                )
                expectThat(code).isEqualTo(ExitCode.COMPILATION_ERROR)
            }
        }
    }
}
