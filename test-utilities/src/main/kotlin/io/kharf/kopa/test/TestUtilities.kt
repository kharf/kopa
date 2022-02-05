package io.kharf.kopa.test

import io.kharf.kopa.packages.ArtifactStorage
import io.kharf.kopa.packages.Artifacts
import io.kharf.kopa.packages.Dependencies
import io.kharf.kopa.packages.DependencyResolver
import io.kharf.kopa.packages.Location
import io.kharf.kopa.packages.ManifestInterpretation
import io.kharf.kopa.packages.ManifestInterpreter
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.nio.ByteBuffer

class FakeDependencyResolver(val artifacts: Artifacts) : DependencyResolver {
    override suspend fun resolve(dependencies: Dependencies): Artifacts = artifacts
}

class FakeArtifactStorage(override val path: String = "") : ArtifactStorage {
    override suspend fun store(artifactContent: Flow<ByteBuffer>, artifactName: String): Location = Location("")
}

class FakeFileManifestInterpreter : ManifestInterpreter<File> {
    override fun interpret(manifest: File): ManifestInterpretation {
        return ManifestInterpretation(Dependencies(emptyList()))
    }
}
