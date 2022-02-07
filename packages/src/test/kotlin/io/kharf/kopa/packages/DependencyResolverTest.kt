package io.kharf.kopa.packages

import failgood.describe
import io.kharf.kopa.test.FakeArtifactStorage
import io.kharf.kopa.test.FakeDependencyResolver
import org.junit.platform.commons.annotation.Testable
import strikt.api.expect
import strikt.assertions.endsWith
import strikt.assertions.hasSize

@Testable
class DependencyResolverTest {
    val context = describe(DependencyResolver::class) {
        describe(MavenDependencyResolver::resolve.toString()) {
            it("should resolve a dependency") {
                val artifacts = MavenDependencyResolver(artifactStorage = FakeArtifactStorage()).resolve(
                    Dependencies(
                        listOf(
                            Dependency(
                                name = "kotlin-stdlib",
                                group = "org.jetbrains.kotlin",
                                version = "1.6.0-RC",
                                type = Dependency.Type.CLASSES
                            )
                        )
                    )
                )
                expect {
                    that(artifacts).hasSize(1)
                }
            }
        }

        describe(CachedDependencyResolver::resolve.toString()) {
            it("should resolve an existing dependency") {
                val artifacts = CachedDependencyResolver(
                    artifactStorage = FakeArtifactStorage(this::class.java.classLoader.getResource("testDependencies/").path),
                    FakeDependencyResolver(
                        Artifacts(emptyList())
                    )
                ).resolve(
                    Dependencies(
                        listOf(
                            Dependency(
                                name = "kopa",
                                group = "",
                                version = "1.0",
                                type = Dependency.Type.CLASSES
                            )
                        )
                    )
                )
                expect {
                    that(artifacts).hasSize(1)
                    that(artifacts[0].location.location).endsWith("kopa-1.0.jar")
                }
            }
        }
    }
}
