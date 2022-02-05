package io.kharf.kopa.compiler

import io.kharf.kopa.packages.DependencyResolver
import io.kharf.kopa.packages.ManifestInterpreter
import mu.KotlinLogging
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString

private val logger = KotlinLogging.logger { }

enum class ExitCode {
    OK,
    COMPILATION_ERROR,
    INTERNAL_ERROR,
    SCRIPT_EXECUTION_ERROR;
}

interface Builder {
    suspend fun build(packageDirPath: Path): ExitCode
}

class KotlinJvmBuilder(
    private val manifestInterpreter: ManifestInterpreter<File>,
    private val dependencyResolver: DependencyResolver
) : Builder {
    override suspend fun build(packageDirPath: Path): ExitCode {
        logger.info { "----- building package on path ${packageDirPath.absolutePathString()}" }
        val interpretation = manifestInterpreter.interpret(File("${packageDirPath.absolutePathString()}/kopa.toml"))
        val artifacts = dependencyResolver.resolve(interpretation.dependencies)
        val args = K2JVMCompilerArguments().apply {
            freeArgs = listOf(File("${packageDirPath.absolutePathString()}/src/Main.kt").absolutePath)
            destination = File("${packageDirPath.absolutePathString()}/build/kopa.jar").absolutePath
            classpath = artifacts.joinToString(":") {
                it.location.location
            }
            skipRuntimeVersionCheck = true
            reportPerf = false
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
