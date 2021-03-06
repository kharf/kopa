package io.kharf.kopa.packages

import failgood.describe
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.get
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import java.nio.file.Path
import kotlin.io.path.absolutePathString

@ExperimentalSerializationApi
@Testable
class SinglePackageTest {
    val context = describe(SinglePackage::class) {
        describe(SinglePackage::init.toString()) {
            val subject = SinglePackage()
            it("should create a simple package") {
                val path = Path.of("build/testsample")
                val template = subject.init(path)
                expectThat(template).hasSize(1)
                expectThat(template[0]).isA<RootDirectory>().and {
                    get { this.file.path }.isEqualTo(path.absolutePathString())
                    get { this.children }.hasSize(2).and {
                        get(0).isA<Manifest>().and {
                            get { this.file.path }.isEqualTo("${path.absolutePathString()}/kopa.toml")
                            get { this.content.packageTable.name }.isEqualTo(PackageTableName("testsample"))
                            get { this.content.packageTable.version }.isEqualTo(PackageTableVersion("0.1.0"))
                            get { this.content.dependencyTable.dependencies }.hasSize(1)
                            get { this.content.dependencyTable.dependencies[0] }.isEqualTo(
                                Dependency(
                                    name = "kotlin-stdlib",
                                    group = "org.jetbrains.kotlin",
                                    version = "1.6.10",
                                    type = Dependency.Type.CLASSES
                                )
                            )
                        }
                        get(1).isA<SourceDirectory>().and {
                            get { this.file.path }.isEqualTo("${path.absolutePathString()}/src")
                            get { this.children }.hasSize(1).and {
                                get(0).isA<SourceFile>().and {
                                    get { this.file.path }.isEqualTo("${path.absolutePathString()}/src/Main.kt")
                                }
                            }
                        }
                    }
                }
                path.toFile().deleteRecursively()
            }
        }
    }
}
