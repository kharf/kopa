@file:Suppress("EXPERIMENTAL_API_USAGE")

package io.kharf.kopa.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import io.kharf.kopa.compiler.Builder
import io.kharf.kopa.compiler.ExitCode
import io.kharf.kopa.compiler.KotlinJvmBuilder
import io.kharf.kopa.packages.CachedDependencyResolver
import io.kharf.kopa.packages.FileManifestInterpreter
import io.kharf.kopa.packages.FileSystemArtifactStorage
import io.kharf.kopa.packages.MavenDependencyResolver
import io.kharf.kopa.packages.Package
import io.kharf.kopa.packages.SinglePackage
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.nio.file.Path

private val logger = KotlinLogging.logger { }

class Kopa : CliktCommand() {
    override fun run() = Unit
}

class Init(private val pack: Package) : CliktCommand(help = "Create a new project (package)") {
    private val path by argument()
    override fun run() {
        runBlocking {
            pack.init(Path.of(path))
            logger.info { "----- INIT SUCCESSFUL" }
        }
    }
}

class Build(private val builder: Builder) : CliktCommand(help = "Compile a package") {
    private val path by argument().optional()
    override fun run() {
        runBlocking {
            when (builder.build(path?.let { Path.of(it) } ?: Path.of(""))) {
                ExitCode.OK ->
                    logger.info { "----- BUILD SUCCESSFUL" }
                ExitCode.COMPILATION_ERROR, ExitCode.INTERNAL_ERROR, ExitCode.SCRIPT_EXECUTION_ERROR ->
                    logger.error { "----- BUILD ERROR" }
            }
        }
    }
}

fun main(args: Array<String>) {
    val artifactStorage = FileSystemArtifactStorage()
    val pack = SinglePackage()
    val builder = KotlinJvmBuilder(
        manifestInterpreter = FileManifestInterpreter(),
        dependencyResolver = CachedDependencyResolver(artifactStorage, MavenDependencyResolver(artifactStorage = artifactStorage))
    )
    Kopa()
        .subcommands(Init(pack))
        .subcommands(Build(builder))
        .main(args)
}
