package io.kharf.kopa.core

import failgood.describe
import org.junit.platform.commons.annotation.Testable

@Testable
class ArtifactResolverTest {
    val context = describe(ArtifactResolver::class) {
        describe(MavenArtifactResolver::resolve.toString()) {
            it("should resolve a dependency") {
                MavenArtifactResolver.resolve(Dependencies(listOf(Dependency("bla", "bla", "bla"))))
            }
        }
    }
}
