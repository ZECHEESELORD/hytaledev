package sh.harold.hytaledev.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.process.KillableProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import java.nio.file.Path
import javax.swing.JComponent
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

class HytaleServerRunConfiguration(
    project: com.intellij.openapi.project.Project,
    factory: com.intellij.execution.configurations.ConfigurationFactory,
    name: String,
) : RunConfigurationBase<HytaleServerRunConfigurationOptions>(project, factory, name) {
    val settings: HytaleServerRunConfigurationOptions
        get() = options

    override fun getOptions(): HytaleServerRunConfigurationOptions {
        return super.getOptions() as HytaleServerRunConfigurationOptions
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return HytaleServerRunConfigurationEditor()
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        val options = options
        val serverDirValue = options.serverDir?.trim().orEmpty()
        val assetsPathValue = options.assetsPath?.trim().orEmpty()
        require(serverDirValue.isNotBlank()) { "Server directory is required" }
        require(assetsPathValue.isNotBlank()) { "Assets.zip path is required" }

        val serverDir = Path.of(serverDirValue)
        val serverJar = serverDir.resolve("HytaleServer.jar")
        val assets = Path.of(assetsPathValue)

        val javaExe = resolveJavaExecutable()
        val commandLine = GeneralCommandLine(javaExe)
            .withWorkDirectory(serverDir.toString())
            .withParameters("-jar", serverJar.toString(), "--assets", assets.toString())

        return object : CommandLineState(environment) {
            override fun startProcess(): ProcessHandler = KillableProcessHandler(commandLine)
        }
    }

    override fun checkConfiguration() {
        val options = options

        val serverDir = options.serverDir?.trim().orEmpty()
        if (serverDir.isBlank()) throw RuntimeConfigurationError("Server directory is required")
        val serverDirPath = Path.of(serverDir)
        if (!serverDirPath.isDirectory()) throw RuntimeConfigurationError("Server directory does not exist: $serverDirPath")

        val serverJar = serverDirPath.resolve("HytaleServer.jar")
        if (!serverJar.isRegularFile()) throw RuntimeConfigurationError("Missing HytaleServer.jar under: $serverDirPath")

        val assetsPath = options.assetsPath?.trim().orEmpty()
        if (assetsPath.isBlank()) throw RuntimeConfigurationError("Assets.zip path is required")
        if (!Path.of(assetsPath).isRegularFile()) throw RuntimeConfigurationError("Assets.zip does not exist: $assetsPath")
    }

    private fun resolveJavaExecutable(): String {
        val sdk = ProjectRootManager.getInstance(project).projectSdk
        return runCatching { sdk?.let { JavaSdk.getInstance().getVMExecutablePath(it) } }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "java"
    }
}

class HytaleServerRunConfigurationOptions : RunConfigurationOptions() {
    var serverDir: String? by string("")
    var assetsPath: String? by string("")
    var pluginJarRelativePath: String? by string("")
    var gradleTasks: String? by string("build")
}

private class HytaleServerRunConfigurationEditor : SettingsEditor<HytaleServerRunConfiguration>() {
    private val serverDirField = JBTextField()
    private val assetsPathField = JBTextField()
    private val pluginJarRelativePathField = JBTextField()
    private val gradleTasksField = JBTextField()

    override fun resetEditorFrom(configuration: HytaleServerRunConfiguration) {
        val options = configuration.settings
        serverDirField.text = options.serverDir.orEmpty()
        assetsPathField.text = options.assetsPath.orEmpty()
        pluginJarRelativePathField.text = options.pluginJarRelativePath.orEmpty()
        gradleTasksField.text = options.gradleTasks.orEmpty()
    }

    override fun applyEditorTo(configuration: HytaleServerRunConfiguration) {
        val options = configuration.settings
        options.serverDir = serverDirField.text.trim().ifBlank { null }
        options.assetsPath = assetsPathField.text.trim().ifBlank { null }
        options.pluginJarRelativePath = pluginJarRelativePathField.text.trim().ifBlank { null }
        options.gradleTasks = gradleTasksField.text.trim().ifBlank { null }
    }

    override fun createEditor(): JComponent {
        return panel {
            row("Server directory:") {
                cell(serverDirField).align(Align.FILL)
            }
            row("Assets.zip:") {
                cell(assetsPathField).align(Align.FILL)
            }
            row("Plugin jar relative path:") {
                cell(pluginJarRelativePathField).align(Align.FILL)
            }
            row("Build tasks/goals:") {
                cell(gradleTasksField).align(Align.FILL)
            }
        }
    }
}
