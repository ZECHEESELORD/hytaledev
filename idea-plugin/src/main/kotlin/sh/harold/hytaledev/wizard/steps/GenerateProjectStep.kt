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
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import sh.harold.hytaledev.manifest.HytaleManifest
import sh.harold.hytaledev.manifest.ManifestWriter
import sh.harold.hytaledev.model.BuildSystem
import sh.harold.hytaledev.model.Language
import sh.harold.hytaledev.model.WizardState
import sh.harold.hytaledev.templating.ProjectGenerator
import sh.harold.hytaledev.templates.HytaleDevTemplateService
import java.nio.file.Path
import kotlin.io.path.createDirectories
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.idea.maven.project.MavenProjectsManager

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
                )
            }
        }

        val output = runWithProgress(project, "Generate Hytale project") {
            try {
                ensureProjectSdk(project)
                generation()
            } catch (e: Throwable) {
                logger.warn("Project generation failed", e)
                throw e
            }
        }

        refreshVfs(projectDir)

        StartupManager.getInstance(project).runAfterOpened {
            when (state.buildSystemProperty.get()) {
                BuildSystem.Gradle -> importGradleProject(project, projectDir)
                BuildSystem.Maven -> importMavenProject(project, projectDir)
            }
            openKeyFiles(project, projectDir, output.generated)
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

    private fun importMavenProject(project: Project, projectDir: Path) {
        val pomPath = projectDir.resolve("pom.xml")
        val pom = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(pomPath) ?: return

        runCatching {
            val manager = MavenProjectsManager.getInstance(project)
            manager.addManagedFilesOrUnignore(listOf(pom))
            manager.forceUpdateProjects()
        }.onFailure { e ->
            logger.warn("Failed to import Maven project", e)
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

    private fun buildTemplateValues(projectDir: Path): Map<String, Any?> {
        val groupId = state.groupIdProperty.get().trim()
        val artifactId = state.artifactIdProperty.get().trim()
        val version = state.projectVersionProperty.get().trim()

        val buildSystem = state.buildSystemProperty.get()
        val language = state.languageProperty.get()

        val isGradle = buildSystem == BuildSystem.Gradle
        val isMaven = buildSystem == BuildSystem.Maven
        val isKotlin = language == Language.Kotlin
        val isJava = language == Language.Java

        val mainClassFqn = state.manifestMainProperty.get().trim()
        val mainClassPackage = mainClassFqn.substringBeforeLast('.')
        val mainClassName = mainClassFqn.substringAfterLast('.')
        val mainClassPackagePath = mainClassPackage.replace('.', '/')

        return buildMap<String, Any?> {
            put("groupId", groupId)
            put("artifactId", artifactId)
            put("version", version)

            put("buildSystem", buildSystem.name)
            put("language", language.name)
            put("isGradle", isGradle)
            put("isMaven", isMaven)
            put("isKotlin", isKotlin)
            put("isJava", isJava)
            put("isGradleKotlin", isGradle && isKotlin)
            put("isGradleJava", isGradle && isJava)
            put("isMavenKotlin", isMaven && isKotlin)
            put("isMavenJava", isMaven && isJava)

            put("mainClassFqn", mainClassFqn)
            put("mainClassPackage", mainClassPackage)
            put("mainClassName", mainClassName)
            put("mainClassPackagePath", mainClassPackagePath)

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
            group = state.groupIdProperty.get().trim(),
            name = state.manifestNameProperty.get().trim(),
            version = state.projectVersionProperty.get().trim(),
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

    private fun openKeyFiles(project: Project, projectDir: Path, generated: sh.harold.hytaledev.templating.GenerationResult) {
        val paths = generated.files
            .asSequence()
            .filter { it.openInEditor }
            .map { projectDir.resolve(it.relativePath) }
            .toList()

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
)
