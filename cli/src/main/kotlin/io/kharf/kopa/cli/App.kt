@file:Suppress("EXPERIMENTAL_API_USAGE")

package io.kharf.kopa.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import io.kharf.kopa.core.FileManifestInterpreter
import io.kharf.kopa.core.FileSystemArtifactStorage
import io.kharf.kopa.core.KotlinJvmBuilder
import io.kharf.kopa.core.LocalDependencyResolver
import io.kharf.kopa.core.MavenDependencyResolver
import io.kharf.kopa.core.Package
import io.kharf.kopa.core.SinglePackage
import kotlinx.coroutines.runBlocking
import java.nio.file.Path

class Kopa : CliktCommand() {
    override fun run() = Unit
}

class Init(private val pack: Package) : CliktCommand(help = "Create a new project (package)") {
    private val path by argument()
    override fun run() {
        runBlocking {
            pack.init(Path.of(path))
        }
    }
}

class Build(private val pack: Package) : CliktCommand(help = "Compile a package") {
    private val path by argument().optional()
    override fun run() {
        runBlocking {
            pack.build(path?.let { Path.of(it) } ?: Path.of(""))
        }
    }
}

fun main(args: Array<String>) {
    val artifactStorage = FileSystemArtifactStorage()
    val pack = SinglePackage(
        manifestInterpreter = FileManifestInterpreter(),
        builder = KotlinJvmBuilder,
        dependencyResolver = LocalDependencyResolver(artifactStorage.path, MavenDependencyResolver()),
        artifactStorage = artifactStorage
    )
    Kopa()
        .subcommands(Init(pack))
        .subcommands(Build(pack))
        .main(args)
}
