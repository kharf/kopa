import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.io.URLUtil
import io.kharf.kopa.packages.Dependency
import io.kharf.kopa.packages.FileManifestInterpreter
import io.kharf.kopa.packages.getGroupPath
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
        val projectLibraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
        val projectLibraryModel = projectLibraryTable.modifiableModel
        interpretation.dependencies.filter { it.type == Dependency.Type.CLASSES }.forEach {
            val kopaDepName = "Kopa: ${it.fullName}"
            val libExists = projectLibraryModel.libraries.find { it.name == kopaDepName } != null
            if (!libExists) {
                val lib = projectLibraryModel.createLibrary(kopaDepName)
                val pathUrl: String =
                    VirtualFileManager.constructUrl(
                        URLUtil.JAR_PROTOCOL,
                        "$homeDir/.kopa/artifacts/${it.getGroupPath()}/${it.fullName}"
                    ) + JarFileSystem.JAR_SEPARATOR
                val file: VirtualFile = VirtualFileManager.getInstance().findFileByUrl(pathUrl)!!
                val sourcesPathUrl: String =
                    VirtualFileManager.constructUrl(
                        URLUtil.JAR_PROTOCOL,
                        "$homeDir/.kopa/artifacts/${it.getGroupPath()}/${it.name}-${it.version}-sources.jar"
                    ) + JarFileSystem.JAR_SEPARATOR
                val sourcesFile: VirtualFile = VirtualFileManager.getInstance().findFileByUrl(sourcesPathUrl)!!
                val libModifiableModel = lib.modifiableModel
                libModifiableModel.addRoot(file, OrderRootType.CLASSES)
                libModifiableModel.addRoot(sourcesFile, OrderRootType.SOURCES)
                app.runWriteAction {
                    libModifiableModel.commit()
                    projectLibraryModel.commit()
                    ModuleRootModificationUtil.addDependency(
                        module!!,
                        lib
                    )
                }
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
