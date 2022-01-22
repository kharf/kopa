package io.kharf.kopa.core

import failgood.describe
import org.junit.platform.commons.annotation.Testable

@Testable
class ArtifactResolverTest {
    val context = describe(DependencyResolver::class) {
        describe(MavenDependencyResolver::resolve.toString()) {
            it("should resolve a dependency") {
                val storage = FileSystemArtifactStorage()
                MavenDependencyResolver().resolve(
                    Dependencies(
                        listOf(
                            Dependency(
                                name = "kotlin-stdlib",
                                group = "org.jetbrains.kotlin",
                                version = "1.6.0-RC"
                            )
                        )
                    ),
                    storage::store
                )
            }
        }
    }
}
