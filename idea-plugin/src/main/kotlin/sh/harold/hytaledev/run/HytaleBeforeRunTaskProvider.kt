package sh.harold.hytaledev.run

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SystemInfo
import sh.harold.hytaledev.server.ServerValidator
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

class HytaleBeforeRunTaskProvider : BeforeRunTaskProvider<HytaleDeployBeforeRunTask>() {
    private val logger = Logger.getInstance(HytaleBeforeRunTaskProvider::class.java)

    override fun getId(): Key<HytaleDeployBeforeRunTask> = ID

    override fun getName(): String = "Deploy plugin to server"

    override fun createTask(runConfiguration: RunConfiguration): HytaleDeployBeforeRunTask? {
        if (runConfiguration !is HytaleServerRunConfiguration) return null
        return HytaleDeployBeforeRunTask().apply { isEnabled = true }
    }

    override fun executeTask(
        context: DataContext,
        configuration: RunConfiguration,
        environment: ExecutionEnvironment,
        task: HytaleDeployBeforeRunTask,
    ): Boolean {
        val runConfiguration = configuration as? HytaleServerRunConfiguration ?: return true
        if (!task.isEnabled) return true

        val project = environment.project
        val projectDir = project.basePath?.let { Path.of(it) } ?: return true

        val action = {
            try {
                validateServer(runConfiguration)
                buildPluginJar(projectDir, runConfiguration)
                copyPluginJar(projectDir, runConfiguration)
                true
            } catch (e: Throwable) {
                logger.warn("Before-run task failed", e)
                false
            }
        }

        if (!ApplicationManager.getApplication().isDispatchThread) return action()

        var ok = false
        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            { ok = action() },
            "Deploy plugin to Hytale server",
            true,
            project,
        )
        return ok
    }

    private fun validateServer(configuration: HytaleServerRunConfiguration) {
        val options = configuration.settings
        val serverDir = Path.of(options.serverDir)
        val assets = Path.of(options.assetsPath)

        val result = ServerValidator().validate(serverDir, assets)
        if (result is sh.harold.hytaledev.server.ValidationResult.Error) {
            error("Server configuration is invalid:\n${result.message}")
        }
    }

    private fun buildPluginJar(projectDir: Path, configuration: HytaleServerRunConfiguration) {
        val tasks = configuration.settings.gradleTasks.trim().ifBlank { "build" }
        val wrapper = if (SystemInfo.isWindows) "gradlew.bat" else "gradlew"
        val wrapperPath = projectDir.resolve(wrapper)
        require(wrapperPath.isRegularFile()) { "Missing Gradle wrapper: $wrapperPath" }

        val command = if (SystemInfo.isWindows) {
            listOf("cmd.exe", "/c", wrapperPath.toString()) + tasks.split(' ').filter { it.isNotBlank() }
        } else {
            listOf(wrapperPath.toString()) + tasks.split(' ').filter { it.isNotBlank() }
        }

        val process = ProcessBuilder(command)
            .directory(projectDir.toFile())
            .redirectErrorStream(true)
            .start()

        val output = StringBuilder()
        process.inputStream.bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
            lines.forEach { line ->
                if (output.length < 12_000) {
                    output.appendLine(line)
                }
            }
        }

        val exit = process.waitFor()
        if (exit != 0) {
            logger.warn("Gradle build failed (exit $exit):\n$output")
            error("Gradle build failed (exit $exit). See idea.log for details.")
        }
    }

    private fun copyPluginJar(projectDir: Path, configuration: HytaleServerRunConfiguration) {
        val options = configuration.settings
        val relative = options.pluginJarRelativePath.trim()
        val jarPath = if (relative.isNotBlank()) projectDir.resolve(relative).normalize() else null

        val resolvedJar = jarPath?.takeIf { it.isRegularFile() } ?: findLatestJar(projectDir.resolve("build/libs"))
        require(resolvedJar != null) { "Could not find built plugin jar under: ${projectDir.resolve("build/libs")}" }

        val modsDir = Path.of(options.serverDir).resolve("mods").createDirectories()
        val destination = modsDir.resolve(resolvedJar.fileName.toString())
        Files.copy(resolvedJar, destination, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun findLatestJar(libsDir: Path): Path? {
        if (!libsDir.exists()) return null

        return libsDir.listDirectoryEntries("*.jar")
            .filterNot { it.fileName.toString().endsWith("-sources.jar") }
            .filterNot { it.fileName.toString().endsWith("-javadoc.jar") }
            .maxByOrNull { Files.getLastModifiedTime(it).toMillis() }
    }

    companion object {
        val ID: Key<HytaleDeployBeforeRunTask> = Key.create("HytaleDev.DeployPluginBeforeRun")
    }
}

class HytaleDeployBeforeRunTask : BeforeRunTask<HytaleDeployBeforeRunTask>(HytaleBeforeRunTaskProvider.ID)
