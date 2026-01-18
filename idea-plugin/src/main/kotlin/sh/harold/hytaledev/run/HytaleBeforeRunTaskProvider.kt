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
        val serverDirValue = options.serverDir?.trim().orEmpty()
        val assetsValue = options.assetsPath?.trim().orEmpty()
        require(serverDirValue.isNotBlank()) { "Server directory is required" }
        require(assetsValue.isNotBlank()) { "Assets.zip path is required" }

        val serverDir = Path.of(serverDirValue)
        val assets = Path.of(assetsValue)

        val result = ServerValidator().validate(serverDir, assets)
        if (result is sh.harold.hytaledev.server.ValidationResult.Error) {
            error("Server configuration is invalid:\n${result.message}")
        }
    }

    private fun buildPluginJar(projectDir: Path, configuration: HytaleServerRunConfiguration) {
        val gradleWrapper = projectDir.resolve(if (SystemInfo.isWindows) "gradlew.bat" else "gradlew")
        val pomXml = projectDir.resolve("pom.xml")
        val mavenWrapper = projectDir.resolve(if (SystemInfo.isWindows) "mvnw.cmd" else "mvnw")

        val tasksRaw = configuration.settings.gradleTasks?.trim().orEmpty()

        val (tool, defaultTasks, baseCommand) = when {
            gradleWrapper.isRegularFile() -> {
                val cmd = if (SystemInfo.isWindows) listOf("cmd.exe", "/c", gradleWrapper.toString()) else listOf(gradleWrapper.toString())
                Triple("Gradle", "build", cmd)
            }

            pomXml.isRegularFile() -> {
                val cmd = if (mavenWrapper.isRegularFile()) {
                    if (SystemInfo.isWindows) listOf("cmd.exe", "/c", mavenWrapper.toString()) else listOf(mavenWrapper.toString())
                } else {
                    listOf("mvn")
                }
                Triple("Maven", "package", cmd)
            }

            else -> error("Unknown build system (no Gradle wrapper or pom.xml found)")
        }

        val tasks = tasksRaw.ifBlank { defaultTasks }
        val tokens = tasks.split(' ').filter { it.isNotBlank() }
        val command = baseCommand + tokens

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
            logger.warn("$tool build failed (exit $exit):\n$output")
            error("$tool build failed (exit $exit). See idea.log for details.")
        }
    }

    private fun copyPluginJar(projectDir: Path, configuration: HytaleServerRunConfiguration) {
        val options = configuration.settings
        val relative = options.pluginJarRelativePath?.trim().orEmpty()
        val jarPath = if (relative.isNotBlank()) projectDir.resolve(relative).normalize() else null

        val candidateDirs = listOf(projectDir.resolve("build/libs"), projectDir.resolve("target"))
        val resolvedJar = jarPath?.takeIf { it.isRegularFile() }
            ?: candidateDirs.asSequence().mapNotNull(::findLatestJar).firstOrNull()
        require(resolvedJar != null) { "Could not find built plugin jar under: ${candidateDirs.joinToString()}" }

        val serverDir = options.serverDir?.trim().orEmpty()
        require(serverDir.isNotBlank()) { "Server directory is required" }
        val modsDir = Path.of(serverDir).resolve("mods").createDirectories()
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
