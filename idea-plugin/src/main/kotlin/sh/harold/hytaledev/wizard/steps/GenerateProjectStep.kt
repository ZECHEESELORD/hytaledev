package sh.harold.hytaledev.wizard.steps

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import sh.harold.hytaledev.manifest.HytaleManifest
import sh.harold.hytaledev.manifest.ManifestWriter
import sh.harold.hytaledev.model.WizardState
import sh.harold.hytaledev.run.HytaleDeployBeforeRunTask
import sh.harold.hytaledev.run.HytaleServerRunConfiguration
import sh.harold.hytaledev.run.HytaleServerRunConfigurationType
import sh.harold.hytaledev.server.ServerValidator
import sh.harold.hytaledev.templating.ProjectGenerator
import sh.harold.hytaledev.templates.HytaleDevTemplateService
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.isRegularFile
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants

class GenerateProjectStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    private val logger = Logger.getInstance(GenerateProjectStep::class.java)
    private val state = WizardState.get(this)

    override fun setupProject(project: Project) {
        val projectDir = project.basePath?.let { Path.of(it) } ?: return
        projectDir.createDirectories()

        val generation: () -> GenerationOutput = {
            val templateId = state.templateIdProperty.get()
            val templateService = service<HytaleDevTemplateService>()
            val template = templateService.openTemplate(templateId)
                ?: error("Unknown template: $templateId")

            template.use {
                val values = buildTemplateValues(projectDir)
                val result = ProjectGenerator().generate(projectDir, template, values)

                val manifest = buildManifest()
                val manifestPath = projectDir.resolve("src/main/resources/manifest.json")
                manifestPath.parent?.createDirectories()
                ManifestWriter().write(manifestPath, manifest)

                GenerationOutput(
                    generated = result,
                    mainClassFqn = values.getValue("mainClassFqn").toString(),
                )
            }
        }

        val output = runWithProgress(project, "Generate Hytale project") {
            try {
                ensureProjectSdk(project)
                validateServerInputs()
                generation()
            } catch (e: Throwable) {
                logger.warn("Project generation failed", e)
                throw e
            }
        }

        refreshVfs(projectDir)

        StartupManager.getInstance(project).runAfterOpened {
            createRunConfiguration(project)
            importGradleProject(project, projectDir)
            openKeyFiles(project, projectDir, output.mainClassFqn)
        }
    }

    private fun createRunConfiguration(project: Project) {
        val type = ConfigurationType.CONFIGURATION_TYPE_EP.extensionList
            .filterIsInstance<HytaleServerRunConfigurationType>()
            .singleOrNull()
            ?: return
        val factory = type.configurationFactories.first()

        val runManager = RunManager.getInstance(project)
        val settings = runManager.createConfiguration("Hytale Server (dev)", factory)
        val configuration = settings.configuration as? HytaleServerRunConfiguration ?: return

        val artifactId = state.artifactIdProperty.get().trim()
        val version = state.projectVersionProperty.get().trim()

        configuration.settings.serverDir = state.serverDirProperty.get().trim()
        configuration.settings.assetsPath = state.assetsPathProperty.get().trim()
        configuration.settings.gradleTasks = "build"
        configuration.settings.pluginJarRelativePath = "build/libs/$artifactId-$version.jar"

        val tasks = listOf(HytaleDeployBeforeRunTask())

        ApplicationManager.getApplication().runWriteAction {
            runManager.addConfiguration(settings)
            runManager.setBeforeRunTasks(settings, tasks)
            runManager.selectedConfiguration = settings
        }
    }

    private fun importGradleProject(project: Project, projectDir: Path) {
        val externalProjectPath = projectDir.toString()

        runCatching {
            val gradleSettings = GradleSettings.getInstance(project)
            val alreadyLinked = gradleSettings.linkedProjectsSettings.any { it.externalProjectPath == externalProjectPath }
            if (!alreadyLinked) {
                val projectSettings = GradleProjectSettings().apply {
                    this.externalProjectPath = externalProjectPath
                }
                gradleSettings.linkProject(projectSettings)
            }

            ExternalSystemUtil.refreshProject(
                externalProjectPath,
                ImportSpecBuilder(project, GradleConstants.SYSTEM_ID),
            )
        }.onFailure { e ->
            logger.warn("Failed to import Gradle project", e)
        }
    }

    private fun ensureProjectSdk(project: Project) {
        val sdk = state.jdkProperty.get() ?: return
        if (!JavaSdk.getInstance().isOfVersionOrHigher(sdk, JavaSdkVersion.JDK_21)) return

        val tableSdk = ProjectJdkTable.getInstance().findJdk(sdk.name)
        if (tableSdk != null) {
            ApplicationManager.getApplication().runWriteAction {
                ProjectRootManager.getInstance(project).projectSdk = tableSdk
            }
        }
    }

    private fun validateServerInputs() {
        val serverDir = state.serverDirProperty.get().trim().takeIf { it.isNotBlank() } ?: return
        val assets = state.assetsPathProperty.get().trim().takeIf { it.isNotBlank() } ?: return

        val serverPath = runCatching { Path.of(serverDir) }.getOrNull() ?: return
        val assetsPath = runCatching { Path.of(assets) }.getOrNull() ?: return

        val result = ServerValidator().validate(serverPath, assetsPath)
        if (result is sh.harold.hytaledev.server.ValidationResult.Error) {
            error("Server configuration is invalid:\n${result.message}")
        }
    }

    private fun buildTemplateValues(projectDir: Path): Map<String, Any?> {
        val groupId = state.groupIdProperty.get().trim()
        val artifactId = state.artifactIdProperty.get().trim()
        val version = state.projectVersionProperty.get().trim()

        val mainClassFqn = state.manifestMainProperty.get().trim()
        val mainClassPackage = mainClassFqn.substringBeforeLast('.')
        val mainClassName = mainClassFqn.substringAfterLast('.')
        val mainClassPackagePath = mainClassPackage.replace('.', '/')

        val serverDir = state.serverDirProperty.get().trim()
        val serverJar = runCatching { Path.of(serverDir).resolve("HytaleServer.jar") }.getOrNull()
            ?.takeIf { it.isRegularFile() }
            ?.toString()
            ?.replace('\\', '/')
            .orEmpty()

        return buildMap<String, Any?> {
            put("groupId", groupId)
            put("artifactId", artifactId)
            put("version", version)
            put("mainClassFqn", mainClassFqn)
            put("mainClassPackage", mainClassPackage)
            put("mainClassName", mainClassName)
            put("mainClassPackagePath", mainClassPackagePath)

            put("serverJar", serverJar)
            put("assetsPath", state.assetsPathProperty.get().trim().replace('\\', '/'))
            put("projectDir", projectDir.toString().replace('\\', '/'))
        }
    }

    private fun buildManifest(): HytaleManifest {
        val authors = buildList {
            val name = state.authorNameProperty.get().trim()
            if (name.isNotBlank()) {
                add(
                    HytaleManifest.Author(
                        name = name,
                        email = state.authorEmailProperty.get().trim().takeIf { it.isNotBlank() },
                        url = state.authorUrlProperty.get().trim().takeIf { it.isNotBlank() },
                    ),
                )
            }
        }

        val subPlugins = buildList {
            if (!state.subPluginEnabledProperty.get()) return@buildList

            add(
                HytaleManifest.SubPlugin(
                    name = state.subPluginNameProperty.get().trim(),
                    main = state.subPluginMainProperty.get().trim(),
                    version = state.subPluginVersionProperty.get().trim(),
                    description = state.subPluginDescriptionProperty.get().trim(),
                    website = state.subPluginWebsiteProperty.get().trim().takeIf { it.isNotBlank() },
                    authors = authors,
                    serverVersion = state.subPluginServerVersionProperty.get().trim().ifBlank { WizardState.DEFAULT_SERVER_VERSION },
                    dependencies = parseMap(state.subPluginDependenciesTextProperty.get()),
                    optionalDependencies = parseMap(state.subPluginOptionalDependenciesTextProperty.get()),
                    loadBefore = parseMap(state.subPluginLoadBeforeTextProperty.get()),
                    disabledByDefault = state.subPluginDisabledByDefaultProperty.get(),
                    includesAssetPack = state.subPluginIncludesAssetPackProperty.get(),
                ),
            )
        }

        return HytaleManifest(
            group = state.manifestGroupProperty.get().trim(),
            name = state.manifestNameProperty.get().trim(),
            version = state.manifestVersionProperty.get().trim(),
            website = state.manifestWebsiteProperty.get().trim().takeIf { it.isNotBlank() },
            description = state.manifestDescriptionProperty.get().trim(),
            authors = authors,
            main = state.manifestMainProperty.get().trim(),
            serverVersion = state.manifestServerVersionProperty.get().trim().ifBlank { WizardState.DEFAULT_SERVER_VERSION },
            dependencies = parseMap(state.dependenciesTextProperty.get()),
            optionalDependencies = parseMap(state.optionalDependenciesTextProperty.get()),
            loadBefore = parseMap(state.loadBeforeTextProperty.get()),
            disabledByDefault = state.manifestDisabledByDefaultProperty.get(),
            includesAssetPack = state.manifestIncludesAssetPackProperty.get(),
            subPlugins = subPlugins,
        )
    }

    private fun parseMap(text: String): Map<String, String> {
        val result = linkedMapOf<String, String>()
        text.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isBlank()) return@forEach

            val idx = trimmed.indexOf('=')
            require(idx > 0 && idx < trimmed.length - 1) { "Invalid entry '$trimmed' (expected key=value)" }

            val key = trimmed.substring(0, idx).trim()
            val value = trimmed.substring(idx + 1).trim()
            require(key.isNotBlank() && value.isNotBlank()) { "Invalid entry '$trimmed' (expected key=value)" }

            result[key] = value
        }
        return result
    }

    private fun runWithProgress(project: Project, title: String, action: () -> GenerationOutput): GenerationOutput {
        if (!ApplicationManager.getApplication().isDispatchThread) return action()

        var output: GenerationOutput? = null
        var error: Throwable? = null

        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            {
                try {
                    output = action()
                } catch (e: Throwable) {
                    error = e
                }
            },
            title,
            true,
            project,
        )

        error?.let { throw it }
        return output ?: error("Project generation did not complete")
    }

    private fun refreshVfs(projectDir: Path) {
        val root = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(projectDir) ?: return
        VfsUtil.markDirtyAndRefresh(false, true, true, root)
    }

    private fun openKeyFiles(project: Project, projectDir: Path, mainClassFqn: String) {
        val mainClassPackagePath = mainClassFqn.substringBeforeLast('.').replace('.', '/')
        val mainClassName = mainClassFqn.substringAfterLast('.')

        val paths = listOf(
            projectDir.resolve("build.gradle.kts"),
            projectDir.resolve("src/main/kotlin/$mainClassPackagePath/$mainClassName.kt"),
            projectDir.resolve("src/main/resources/manifest.json"),
        )

        val lfs = LocalFileSystem.getInstance()
        val files: List<VirtualFile> = paths.mapNotNull { lfs.findFileByNioFile(it) }
        if (files.isEmpty()) return

        ApplicationManager.getApplication().invokeLater {
            val manager = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project)
            files.forEach { manager.openFile(it, true) }
        }
    }
}

private data class GenerationOutput(
    val generated: sh.harold.hytaledev.templating.GenerationResult,
    val mainClassFqn: String,
)
