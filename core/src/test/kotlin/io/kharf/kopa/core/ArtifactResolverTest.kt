package io.kharf.kopa.core

import failgood.describe
import okio.ExperimentalFileSystem
import okio.fakefilesystem.FakeFileSystem
import org.junit.platform.commons.annotation.Testable

@ExperimentalFileSystem
@Testable
class ArtifactResolverTest {
    val context = describe(ArtifactResolver::class) {
        describe(MavenArtifactResolver::resolve.toString()) {
            it("should resolve a dependency") {
                MavenArtifactResolver(FakeFileSystem()).resolve(Dependencies(listOf(Dependency("kotlin-stdlib", "org.jetbrains.kotlin", "1.6.0-RC"))))
            }
        }
    }
}
