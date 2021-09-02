package io.kharf.koship.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import io.kharf.koship.core.LibKontainer
import io.kharf.koship.core.Path
import kotlinx.coroutines.runBlocking

class Koship : CliktCommand() {
    override fun run() = Unit
}

class Init : CliktCommand(help = "Create a new project (kontainer)") {
    private val path by argument()
    override fun run() = runBlocking {
        LibKontainer().init(Path(path))
        echo("created $path kontainer")
    }
}

fun main(args: Array<String>) = Koship()
    .subcommands(Init())
    .main(args)
