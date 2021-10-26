package io.kharf.kopa.core

import failgood.describe
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.get
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import java.io.File

class FakeBuilder(val code: Int) : Builder {
    override suspend fun build(containerDirPath: java.nio.file.Path): ExitCode = ExitCode.of(code)
}

@ExperimentalSerializationApi
@Testable
class AppContainerTest {
    val context = describe(AppContainer::class) {
        describe(AppContainer::init.toString()) {
            val subject = AppContainer(FakeBuilder(0))
            it("should create a simple container") {
                val path = Path("build/testsample")
                val template = subject.init(path)
                expectThat(template).hasSize(1)
                expectThat(template[0]).isA<RootDirectory>().and {
                    get { this.file.path }.isEqualTo(path.path)
                    get { this.children }.hasSize(2).and {
                        get(0).isA<Manifest>().and {
                            get { this.file.path }.isEqualTo("${path.path}/kopa.toml")
                            get { this.content.container.name }.isEqualTo(ContainerTableName("testsample"))
                            get { this.content.container.version }.isEqualTo(ContainerTableVersion("0.1.0"))
                        }
                        get(1).isA<SourceDirectory>().and {
                            get { this.file.path }.isEqualTo("${path.path}/src")
                            get { this.children }.hasSize(1).and {
                                get(0).isA<SourceFile>().and {
                                    get { this.file.path }.isEqualTo("${path.path}/src/Main.kt")
                                }
                            }
                        }
                    }
                }
                File(path.path).deleteRecursively()
            }
        }
        describe(AppContainer::build.toString()) {
            val successfulBuilder = FakeBuilder(0)
            val erroneousBuilder = FakeBuilder(1)
            val path = Path("build/testsample")
            it("should return a positive result when a container could be built") {
                val subject = AppContainer(successfulBuilder)
                val result = subject.build(path)
                expectThat(result).isA<BuildResult.Ok>()
            }

            it("should return a negative result when a container could not be built") {
                val subject = AppContainer(erroneousBuilder)
                val result = subject.build(path)
                expectThat(result).isA<BuildResult.Error>()
            }
        }
    }
}
