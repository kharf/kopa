package io.kharf.kopa.core

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import java.io.File

data class Interpretation(
    val classpath: String
)

interface ManifestInterpreter<T> {
    fun interpret(manifest: T): Interpretation
}

enum class ExitCode(val code: Int) {
    OK(0),
    COMPILATION_ERROR(1),
    INTERNAL_ERROR(2),
    SCRIPT_EXECUTION_ERROR(3);

    companion object {
        fun of(code: Int): ExitCode = values().first { it.code == code }
    }
}

interface Builder {
    suspend fun build(path: Path): ExitCode
}

object KotlinJvmBuilder : Builder {
    override suspend fun build(path: Path): ExitCode {
        val args = K2JVMCompilerArguments().apply {
            freeArgs = listOf(File("${path.path}/src/Main.kt").absolutePath)
            // TODO: read toml
            destination = File("${path.path}/build/kopa.jar").absolutePath
            classpath = "/home/kharf/code/kopa/.tmp/kotlin-stdlib-1.5.31.jar"
            skipRuntimeVersionCheck = true
            reportPerf = true
            noStdlib = true
            noReflect = true
        }
        val compilerExitCode = K2JVMCompiler().execImpl(
            messageCollector = PrintingMessageCollector(
                System.out,
                MessageRenderer.WITHOUT_PATHS,
                true
            ),
            services = Services.EMPTY,
            arguments = args
        )
        return ExitCode.valueOf(compilerExitCode.name)
    }
}
