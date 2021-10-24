package io.kharf.kopa.core

import failgood.describe
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.get
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo

@ExperimentalSerializationApi
@Testable
class AppContainerTest {
    val context = describe(AppContainer::class) {
        val subject = AppContainer
        val path = Path("build/testsample")
        describe(AppContainer::init.toString()) {
            it("should create a simple project") {
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
            }
        }
        describe(AppContainer::build.toString()) {
            it("should build a simple project") {
                subject.build(path)
            }
        }
    }
}
