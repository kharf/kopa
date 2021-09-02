package io.kharf.kopa.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import io.kharf.kopa.core.AppContainer
import io.kharf.kopa.core.Path
import kotlinx.coroutines.runBlocking

class Kopa : CliktCommand() {
    override fun run() = Unit
}

class Init : CliktCommand(help = "Create a new project (container)") {
    private val path by argument()
    override fun run() = runBlocking {
        AppContainer.init(Path(path))
        echo("created container in $path")
    }
}

class Build : CliktCommand(help = "Compile a container") {
    private val path by argument().optional()
    override fun run() = runBlocking {
        AppContainer.build(path?.let { Path(it) } ?: Path(""))
        echo("built container in $path")
    }
}

fun main(args: Array<String>) = Kopa()
    .subcommands(Init())
    .subcommands(Build())
    .main(args)
