package sh.harold.hytaledev.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.process.KillableProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.roots.ProjectRootManager
import java.nio.file.Path
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

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        val options = options
        val serverDir = Path.of(options.serverDir)
        val serverJar = serverDir.resolve("HytaleServer.jar")
        val assets = Path.of(options.assetsPath)

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

        val serverDir = options.serverDir.trim()
        require(serverDir.isNotBlank()) { "Server directory is required" }
        val serverDirPath = Path.of(serverDir)
        require(serverDirPath.isDirectory()) { "Server directory does not exist: $serverDirPath" }

        val serverJar = serverDirPath.resolve("HytaleServer.jar")
        require(serverJar.isRegularFile()) { "Missing HytaleServer.jar under: $serverDirPath" }

        val assetsPath = options.assetsPath.trim()
        require(assetsPath.isNotBlank()) { "Assets.zip path is required" }
        require(Path.of(assetsPath).isRegularFile()) { "Assets.zip does not exist: $assetsPath" }
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
    var serverDir: String by string("")
    var assetsPath: String by string("")
    var pluginJarRelativePath: String by string("")
    var gradleTasks: String by string("build")
}
