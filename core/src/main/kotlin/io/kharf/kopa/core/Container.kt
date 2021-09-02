package io.kharf.kopa.core

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import java.io.File

interface Container {
    suspend fun init(path: Path): Template
    suspend fun build(path: Path)
}

@JvmInline
value class Path(val path: String)

@ExperimentalSerializationApi
object AppContainer : Container {
    override suspend fun init(path: Path): Template {
        val template = AppContainerTemplate(path)
        template.forEach { component ->
            create(component)
        }
        return template
    }

    override suspend fun build(path: Path) {
        KotlinJvmBuilder.build(path)
    }

    private fun create(component: ContainerComponent) {
        when (component) {
            is ContainerDirectory -> component.file.mkdir()
                .also { component.children.forEach { create(it) } }
            is Manifest ->
                // TODO: contribute to Ktoml for: component.file.writeText(Toml.encodeToString(component.content))
                component.file.writeText(
                    "[container]\n" +
                        "name = \"${component.content.container.name.name}\"\n" +
                        "version = \"${component.content.container.version.version}\"\n" +
                        "\n" +
                        "[dependencies]\n"
                )
            is ContainerFile -> if (component is SourceFile) {
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

sealed interface ContainerComponent
sealed class ContainerDocument : ContainerComponent
sealed class ContainerDirectory(val file: File, val children: List<ContainerComponent>) : ContainerDocument()
sealed class ContainerFile(val file: File) : ContainerDocument()
class SourceDirectory(file: File, children: List<ContainerComponent>) : ContainerDirectory(file, children)
class RootDirectory(file: File, children: List<ContainerComponent>) : ContainerDirectory(file, children)
class SubDirectory(file: File, children: List<ContainerComponent>) : ContainerDirectory(file, children)
class SourceFile(file: File) : ContainerFile(file)

@Serializable
data class ManifestContent(
    val container: ContainerTable
)

@Serializable
@JvmInline
value class ContainerTableName(val name: String)

@Serializable
@JvmInline
value class ContainerTableVersion(val version: String)

@Serializable
data class ContainerTable(
    val name: ContainerTableName,
    val version: ContainerTableVersion
)

@JvmInline
value class ContainerName(val name: String) {
    override fun toString(): String = name
}

class Manifest(
    containerName: ContainerName,
    file: File,
    val content: ManifestContent = ManifestContent(
        ContainerTable(
            name = ContainerTableName(containerName.name),
            version = ContainerTableVersion("0.1.0")
        )
    )
) : ContainerFile(file)

class Template(components: List<ContainerComponent>) : List<ContainerComponent> by components

interface ContainerTemplate {
    suspend operator fun invoke(root: Path): Template
}

object AppContainerTemplate : ContainerTemplate {
    override suspend fun invoke(root: Path): Template {
        val file = File(root.path)
        return Template(
            listOf(
                RootDirectory(
                    file = file,
                    children = listOf(
                        Manifest(ContainerName(file.name), File("${root.path}/kopa.toml")),
                        SourceDirectory(File("${root.path}/src"), listOf(SourceFile(File("${root.path}/src/Main.kt"))))
                    )
                ),
            )
        )
    }
}
