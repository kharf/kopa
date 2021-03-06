package io.kharf.kopa.packages

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString

private val logger = KotlinLogging.logger { }

sealed interface PackageComponent

sealed class PackageDocument : PackageComponent

sealed class PackageDirectory(val file: File, val children: List<PackageComponent>) : PackageDocument()

sealed class PackageFile(val file: File) : PackageDocument()

class SourceDirectory(file: File, children: List<PackageComponent>) : PackageDirectory(file, children)

class RootDirectory(file: File, children: List<PackageComponent>) : PackageDirectory(file, children)

class SourceFile(file: File) : PackageFile(file)

interface Package {
    suspend fun init(path: Path): Template
}

@ExperimentalSerializationApi
class SinglePackage : Package {
    override suspend fun init(path: Path): Template {
        logger.info { "initializing package on path ${path.absolutePathString()}" }
        val template = SinglePackageTemplate(path)
        template.forEach { component ->
            create(component)
        }
        return template
    }

    private fun create(component: PackageComponent) {
        when (component) {
            is PackageDirectory -> component.file.mkdir()
                .also { component.children.forEach { create(it) } }
            is Manifest ->
                // TODO: contribute to Ktoml for: component.file.writeText(Toml.encodeToString(component.content))
                component.file.writeText(
                    "[package]\n" +
                        "name = \"${component.content.packageTable.name.name}\"\n" +
                        "version = \"${component.content.packageTable.version.version}\"\n" +
                        "\n" +
                        "[dependencies]\n" +
                        "\"${component.content.dependencyTable.dependencies[0].group}.${component.content.dependencyTable.dependencies[0].name}\" = \"${component.content.dependencyTable.dependencies[0].version}\"\n"
                )
            is PackageFile -> if (component is SourceFile) {
                component.file.writeText(
                    "fun main() {\n" +
                        "println(\"Hello World\")\n" +
                        "}"
                )
            } else {
                component.file.createNewFile()
            }
        }
    }
}

@Serializable
data class ManifestContent(
    val packageTable: PackageTable,
    val dependencyTable: DependencyTable
)

@Serializable
@JvmInline
value class PackageTableName(val name: String)

@Serializable
@JvmInline
value class PackageTableVersion(val version: String)

@Serializable
data class PackageTable(
    val name: PackageTableName,
    val version: PackageTableVersion
)

@Serializable
data class DependencyTable(val dependencies: Dependencies)

class Manifest(
    file: File,
    val content: ManifestContent
) : PackageFile(file)

class Template(components: List<PackageComponent>) : List<PackageComponent> by components

interface PackageTemplate {
    suspend operator fun invoke(root: Path): Template
}

object SinglePackageTemplate : PackageTemplate {
    override suspend fun invoke(root: Path): Template {
        val file = File(root.absolutePathString())
        return Template(
            listOf(
                RootDirectory(
                    file = file,
                    children = listOf(
                        Manifest(
                            File("${root.absolutePathString()}/kopa.toml"),
                            content = ManifestContent(
                                PackageTable(
                                    name = PackageTableName(file.name),
                                    version = PackageTableVersion("0.1.0")
                                ),
                                DependencyTable(
                                    Dependencies(
                                        listOf(
                                            Dependency(
                                                name = "kotlin-stdlib",
                                                group = "org.jetbrains.kotlin",
                                                version = "1.6.10",
                                                type = Dependency.Type.CLASSES
                                            ),
                                        )
                                    )
                                )
                            )
                        ),
                        SourceDirectory(
                            File("${root.absolutePathString()}/src"),
                            listOf(SourceFile(File("${root.absolutePathString()}/src/Main.kt")))
                        )
                    )
                )
            )
        )
    }
}
