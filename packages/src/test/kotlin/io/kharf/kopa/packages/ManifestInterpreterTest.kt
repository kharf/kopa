package io.kharf.kopa.packages

import failgood.describe
import kotlinx.serialization.ExperimentalSerializationApi
import okio.Path.Companion.toOkioPath
import okio.fakefilesystem.FakeFileSystem
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.get
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import java.io.File

@ExperimentalSerializationApi
@Testable
class ManifestInterpreterTest {
    val context = describe(ManifestInterpreter::class) {
        describe(StringManifestInterpreter::interpret.toString()) {
            val subject = StringManifestInterpreter
            it("interpretes a simple manifest") {
                val interpretation = subject.interpret(
                    """
                       [package]
                       name = "builder-testsample"
                       version = "0.1.0"

                       [dependencies]
                       "org.jetbrains.kotlin.kotlin-stdlib" = "1.5.32"
                       "dev.failgood.failgood"              = "1.0.0"
                    """.trimIndent()
                )
                expectThat(interpretation) {
                    get { dependencies }.hasSize(4)[0].and {
                        get { name }.isEqualTo("kotlin-stdlib")
                        get { version }.isEqualTo("1.5.32")
                        get { group }.isEqualTo("org.jetbrains.kotlin")
                        get { type }.isEqualTo(Dependency.Type.CLASSES)
                    }
                    get { dependencies }.hasSize(4)[1].and {
                        get { name }.isEqualTo("kotlin-stdlib")
                        get { version }.isEqualTo("1.5.32")
                        get { group }.isEqualTo("org.jetbrains.kotlin")
                        get { type }.isEqualTo(Dependency.Type.SOURCES)
                    }
                    get { dependencies }[2].and {
                        get { name }.isEqualTo("failgood")
                        get { version }.isEqualTo("1.0.0")
                        get { group }.isEqualTo("dev.failgood")
                        get { type }.isEqualTo(Dependency.Type.CLASSES)
                    }
                    get { dependencies }[3].and {
                        get { name }.isEqualTo("failgood")
                        get { version }.isEqualTo("1.0.0")
                        get { group }.isEqualTo("dev.failgood")
                        get { type }.isEqualTo(Dependency.Type.SOURCES)
                    }
                }
            }
        }

        describe(FileManifestInterpreter::interpret.toString()) {
            val fileSystem = FakeFileSystem()
            val subject = FileManifestInterpreter(fileSystem)
            it("interpretes a simple manifest") {
                val file = File("build/classpath/kopa.toml")
                fileSystem.createDirectories(File("build").toOkioPath())
                fileSystem.createDirectories(File("build/classpath").toOkioPath())
                fileSystem.write(file.toOkioPath(), true) {
                    writeUtf8(
                        """
                       [package]
                       name = "builder-testsample"
                       version = "0.1.0"

                       [dependencies]
                       "org.jetbrains.kotlin.kotlin-stdlib" = "1.5.32"
                       "dev.failgood.failgood"              = "1.0.0"
                        """.trimIndent()
                    )
                }
                val interpretation = subject.interpret(
                    file
                )
                expectThat(interpretation) {
                    get { dependencies }.hasSize(4)[0].and {
                        get { name }.isEqualTo("kotlin-stdlib")
                        get { version }.isEqualTo("1.5.32")
                        get { group }.isEqualTo("org.jetbrains.kotlin")
                        get { type }.isEqualTo(Dependency.Type.CLASSES)
                    }
                    get { dependencies }.hasSize(4)[1].and {
                        get { name }.isEqualTo("kotlin-stdlib")
                        get { version }.isEqualTo("1.5.32")
                        get { group }.isEqualTo("org.jetbrains.kotlin")
                        get { type }.isEqualTo(Dependency.Type.SOURCES)
                    }
                    get { dependencies }[2].and {
                        get { name }.isEqualTo("failgood")
                        get { version }.isEqualTo("1.0.0")
                        get { group }.isEqualTo("dev.failgood")
                        get { type }.isEqualTo(Dependency.Type.CLASSES)
                    }
                    get { dependencies }[3].and {
                        get { name }.isEqualTo("failgood")
                        get { version }.isEqualTo("1.0.0")
                        get { group }.isEqualTo("dev.failgood")
                        get { type }.isEqualTo(Dependency.Type.SOURCES)
                    }
                }
            }
        }
    }
}
