package io.kharf.koship.core

import java.io.File

interface Kontainer {
    suspend fun init(path: Path)
}

@JvmInline
value class Path(val path: String)

class LibKontainer : Kontainer {
    override suspend fun init(path: Path) {
        val template = SingleLibKontainerTemplate(path)
        template.forEach { component ->
            when(component) {
                is KontainerDirectory, is KontainerFile ->
            }
        }

    }
}

sealed class KontainerComponent
sealed class Kontainer
sealed class KontainerDirectory(val file: File) : KontainerComponent()
sealed class KontainerFile(val file: File) : KontainerComponent()
class Manifest(file: File) : KontainerFile(file)
class SourceDirectory(file: File) : KontainerDirectory(file)
class RootDirectory(file: File) : KontainerDirectory(file)
class SubDirectory(file: File) : KontainerDirectory(file)

class Template(components: List<KontainerComponent>) : List<KontainerComponent> by components

interface KontainerTemplate {
    suspend operator fun invoke(root: Path): Template
}

object SingleLibKontainerTemplate : KontainerTemplate {
    override suspend fun invoke(root: Path): Template {
        return Template(
            listOf(
                RootDirectory(File(root.path)),
                Manifest(File("${root.path}/Kontainer.toml")),
                SourceDirectory(File("${root.path}/src"))
            )
        )
    }
}