package io.kharf.kopa.packages

import com.akuleshov7.ktoml.KtomlConf
import com.akuleshov7.ktoml.parsers.TomlParser
import com.akuleshov7.ktoml.parsers.node.TomlKeyValuePrimitive
import com.akuleshov7.ktoml.parsers.node.TomlTable
import mu.KotlinLogging
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.io.File

private val logger = KotlinLogging.logger { }

data class ManifestInterpretation(
    val dependencies: Dependencies,
)

interface ManifestInterpreter<in T> {
    fun interpret(manifest: T): ManifestInterpretation
}

object StringManifestInterpreter : ManifestInterpreter<String> {
    override fun interpret(manifest: String): ManifestInterpretation {
        logger.trace { "interpreting manifest string" }
        val toml = TomlParser(KtomlConf()).parseString(manifest)
        val dependencies: TomlTable =
            toml.children.find { node -> node.name == "dependencies" && node is TomlTable } as TomlTable?
                ?: throw RuntimeException("dependencies wrongly configured")
        val filteredDependencies = dependencies.children.filterIsInstance<TomlKeyValuePrimitive>()
        val deps = filteredDependencies.map { dependency ->
            Dependency(
                name = dependency.key.content.substringAfterLast("."),
                version = dependency.value.content as String,
                group = dependency.key.content.substringBeforeLast(".", ""),
            )
        }
        return ManifestInterpretation(
            dependencies = Dependencies(deps)
        )
    }
}

class FileManifestInterpreter(
    private val fileSystem: FileSystem = FileSystem.SYSTEM
) : ManifestInterpreter<File> {
    override fun interpret(manifest: File): ManifestInterpretation {
        logger.trace { "interpreting manifest file" }
        val path = manifest.toOkioPath()
        val manifestString = fileSystem.read(path) {
            readUtf8()
        }
        return StringManifestInterpreter.interpret(manifestString)
    }
}
