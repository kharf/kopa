package io.kharf.kopa.packages

import failgood.describe
import io.kharf.kopa.test.FakeArtifactStorage
import org.junit.platform.commons.annotation.Testable
import strikt.api.expect
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
                                type = Dependency.Type.CLASSES,
                                scope = Dependency.Scope.COMPILE
                            )
                        )
                    )
                )
                expect {
                    that(artifacts).hasSize(1)
                }
            }
        }
    }
}
