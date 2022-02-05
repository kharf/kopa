
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import io.kharf.kopa.packages.FileManifestInterpreter
import java.io.File

class ImportKopaProjectAction : AnAction() {
    private val fileInterpreter = FileManifestInterpreter()

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
