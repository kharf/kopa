package io.kharf.kopa.core

import failgood.describe
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.get
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class FakeBuilder(private val code: Int) : Builder {
    override suspend fun build(
        packageDirPath: Path,
        artifacts: Artifacts
    ): ExitCode = ExitCode.of(code)
}

class FakeDependencyResolver : DependencyResolver {
    override suspend fun resolve(
        dependencies: Dependencies,
        store: suspend (Flow<ByteBuffer>, String) -> Location
    ): Artifacts = Artifacts(emptyList())
}

class FakeArtifactStorage : ArtifactStorage {
    override val path: String = ""

    override suspend fun store(artifactContent: Flow<ByteBuffer>, artifactName: String): Location = Location("")
}

class FakeFileManifestInterpreter : ManifestInterpreter<File> {
    override fun interpret(manifest: File): ManifestInterpretation {
        return ManifestInterpretation(Dependencies(emptyList()))
    }
}

@ExperimentalSerializationApi
@Testable
class SinglePackageTest {
    val context = describe(SinglePackage::class) {
        describe(SinglePackage::init.toString()) {
            val subject = SinglePackage(
                FakeFileManifestInterpreter(),
                FakeBuilder(0),
                FakeDependencyResolver(),
                FakeArtifactStorage()
            )
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
        describe(SinglePackage::build.toString()) {
            val successfulBuilder = FakeBuilder(0)
            val erroneousBuilder = FakeBuilder(1)
            val path = Path.of("build/testsample")
            it("should return a positive result when a package could be built") {
                val subject = SinglePackage(
                    FakeFileManifestInterpreter(), successfulBuilder, FakeDependencyResolver(),
                    FakeArtifactStorage()
                )
                val result = subject.build(path)
                expectThat(result).isA<BuildResult.Ok>()

                path.toFile().deleteRecursively()
            }

            it("should return a negative result when a package could not be built") {
                val subject = SinglePackage(
                    FakeFileManifestInterpreter(), erroneousBuilder, FakeDependencyResolver(),
                    FakeArtifactStorage()
                )
                val result = subject.build(path)
                expectThat(result).isA<BuildResult.Error>()
            }
        }
    }
}
