package io.kharf.kopa.core

import com.akuleshov7.ktoml.KtomlConf
import com.akuleshov7.ktoml.parsers.TomlParser
import com.akuleshov7.ktoml.parsers.node.TomlKeyValueSimple
import com.akuleshov7.ktoml.parsers.node.TomlTable
import mu.KotlinLogging
import okio.ExperimentalFileSystem
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import java.io.File

private val logger = KotlinLogging.logger { }

data class Dependency(
    val name: String,
    val value: String,
    val path: String
)

class Dependencies(list: List<Dependency>) : List<Dependency> by list

data class Interpretation(
    val dependencies: Dependencies,
    val classpath: String
)

interface ManifestInterpreter<T> {
    fun interpret(manifest: T): Interpretation
}

object StringManifestInterpreter : ManifestInterpreter<String> {
    override fun interpret(manifest: String): Interpretation {
        logger.info { "interpreting manifest string" }
        val toml = TomlParser(KtomlConf()).parseString(manifest)
        val dependencies: TomlTable =
            toml.children.find { node -> node.name == "dependencies" && node is TomlTable } as TomlTable?
                ?: throw RuntimeException("dependencies wrongly configured")
        val filteredDependencies = dependencies.children.filterIsInstance<TomlKeyValueSimple>()
        val builder = StringBuilder("")
        val deps = mutableListOf<Dependency>()
        filteredDependencies.forEachIndexed { index, dependency ->
            val path = "build/classpath/${dependency.key.content}-${dependency.value.content}.jar"
            builder.append(path)
            if (filteredDependencies.size != index + 1) builder.append(":")
            deps.add(
                Dependency(
                    name = dependency.key.content,
                    value = dependency.value.content as String,
                    path = path
                )
            )
        }
        val classpath = builder.toString()
        return Interpretation(
            classpath = classpath,
            dependencies = Dependencies(deps)
        )
    }
}

class FileManifestInterpreter @OptIn(ExperimentalFileSystem::class) constructor(
    private val fileSystem: FileSystem = FileSystem.SYSTEM
) : ManifestInterpreter<File> {
    @ExperimentalFileSystem
    override fun interpret(manifest: File): Interpretation {
        logger.info { "interpreting manifest file" }
        val path = manifest.toOkioPath()
        val manifestString = fileSystem.read(path) {
            readUtf8()
        }
        return StringManifestInterpreter.interpret(manifestString)
    }
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

class KotlinJvmBuilder(
    private val manifestInterpreter: ManifestInterpreter<File>
) : Builder {
    override suspend fun build(path: Path): ExitCode {
        val interpretation = manifestInterpreter.interpret(File(path.path))
        val args = K2JVMCompilerArguments().apply {
            freeArgs = listOf(File("${path.path}/src/Main.kt").absolutePath)
            // TODO: read toml
            destination = File("${path.path}/build/kopa.jar").absolutePath
            classpath = interpretation.classpath
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
