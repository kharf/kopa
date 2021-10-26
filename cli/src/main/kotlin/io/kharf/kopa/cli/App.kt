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
import io.kharf.kopa.core.Path
import kotlinx.coroutines.runBlocking

class Kopa : CliktCommand() {
    override fun run() = Unit
}

class Init(private val container: Container) : CliktCommand(help = "Create a new project (container)") {
    private val path by argument()
    override fun run() {
        runBlocking {
            container.init(Path(path))
        }
    }
}

class Build(private val container: Container) : CliktCommand(help = "Compile a container") {
    private val path by argument().optional()
    override fun run() {
        runBlocking {
            container.build(path?.let { Path(it) } ?: Path(""))
        }
    }
}

fun main(args: Array<String>) {
    val container = AppContainer(
        KotlinJvmBuilder(
            manifestInterpreter = FileManifestInterpreter()
        )
    )
    Kopa()
        .subcommands(Init(container))
        .subcommands(Build(container))
        .main(args)
}
