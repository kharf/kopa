import com.akuleshov7.ktoml.KtomlConf
import com.akuleshov7.ktoml.parsers.TomlParser
import com.akuleshov7.ktoml.parsers.node.TomlKeyValuePrimitive
import com.akuleshov7.ktoml.parsers.node.TomlTable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.io.File

class ImportKopaProjectAction : AnAction() {
    private val fileInterpreter = FileManifestInterpreter()

    override fun update(e: AnActionEvent) {
        super.update(e)
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project: Project? = event.project
        val instance = ProjectRootManager.getInstance(project!!)
        val contentRoots = instance.contentRoots
        var kopaVirtualFile: VirtualFile? = null
        var kopaToml: File? = null
        for (file in contentRoots) {
            kopaVirtualFile = file.findChild("kopa.toml")
            kopaToml = kopaVirtualFile!!.toNioPath().toFile()
        }
        val module = instance.fileIndex.getModuleForFile(kopaVirtualFile!!)
        val interpretation = fileInterpreter.interpret(kopaToml!!)
        val homeDir = System.getProperty("user.home")
        val app = ApplicationManager.getApplication()
        interpretation.dependencies.forEach {
            app.runWriteAction {
                ModuleRootModificationUtil.addModuleLibrary(
                    module!!,
                    "jar://$homeDir/.kopa/packages/${it.name}-${it.version}.jar!/"
                )
            }
        }
        val modifiableModel = ModuleRootManager.getInstance(module!!).modifiableModel
        modifiableModel.sdk
        modifiableModel.contentEntries.forEach {
            val src = it.file!!.findChild("src")
            it.addSourceFolder(src!!, false)
        }
        app.runWriteAction {
            modifiableModel.commit()
        }
        Messages.showInfoMessage("Kopa project is ready to go", "Kopa Project")
    }
}

data class Dependency(
    val name: String,
    val group: String,
    val version: String,
)

class Dependencies(list: List<Dependency>) : List<Dependency> by list

data class ManifestInterpretation(
    val dependencies: Dependencies,
)

interface ManifestInterpreter<in T> {
    fun interpret(manifest: T): ManifestInterpretation
}

class FileManifestInterpreter(
    private val fileSystem: FileSystem = FileSystem.SYSTEM
) : ManifestInterpreter<File> {
    override fun interpret(manifest: File): ManifestInterpretation {
        val path = manifest.toOkioPath()
        val manifestString = fileSystem.read(path) {
            readUtf8()
        }
        return StringManifestInterpreter.interpret(manifestString)
    }
}

object StringManifestInterpreter : ManifestInterpreter<String> {
    override fun interpret(manifest: String): ManifestInterpretation {
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
