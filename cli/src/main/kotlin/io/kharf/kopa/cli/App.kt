@file:Suppress("EXPERIMENTAL_API_USAGE")

package io.kharf.kopa.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import io.kharf.kopa.core.AppContainer
import io.kharf.kopa.core.Container
import io.kharf.kopa.core.FileManifestInterpreter
import io.kharf.kopa.core.KotlinJvmBuilder
import kotlinx.coroutines.runBlocking
import java.nio.file.Path

class Kopa : CliktCommand() {
    override fun run() = Unit
}

class Init(private val container: Container) : CliktCommand(help = "Create a new project (container)") {
    private val path by argument()
    override fun run() {
        runBlocking {
            container.init(Path.of(path))
        }
    }
}

class Build(private val container: Container) : CliktCommand(help = "Compile a container") {
    private val path by argument().optional()
    override fun run() {
        runBlocking {
            container.build(path?.let { Path.of(it) } ?: Path.of(""))
        }
    }
}

fun main(args: Array<String>) {
    val container = AppContainer(
        manifestInterpreter = FileManifestInterpreter(),
        builder = KotlinJvmBuilder
    )
    Kopa()
        .subcommands(Init(container))
        .subcommands(Build(container))
        .main(args)
}
