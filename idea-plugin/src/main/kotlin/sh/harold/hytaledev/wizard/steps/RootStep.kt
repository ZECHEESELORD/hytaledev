package sh.harold.hytaledev.wizard.steps

import com.intellij.ide.wizard.NewProjectWizardBaseData
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import sh.harold.hytaledev.model.WizardState
import sh.harold.hytaledev.settings.HytaleDevSettingsState
import sh.harold.hytaledev.wizard.normalizeArtifactId
import sh.harold.hytaledev.wizard.suggestMainClassFqn
import java.nio.file.Path
import kotlin.io.path.isRegularFile

class RootStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    private val state = WizardState.get(this)

    init {
        val baseData = data.getUserData(NewProjectWizardBaseData.KEY)
        val initialProjectName = baseData?.name?.trim() ?: context.projectName?.trim().orEmpty()
        var lastProjectName = initialProjectName

        val settings = service<HytaleDevSettingsState>().state
        if (state.serverDirProperty.get().isBlank()) {
            state.serverDirProperty.set(settings.lastServerDir.orEmpty())
        }
        if (state.assetsPathProperty.get().isBlank()) {
            state.assetsPathProperty.set(settings.lastAssetsPath.orEmpty())
        }

        if (state.groupIdProperty.get().isBlank()) {
            state.groupIdProperty.set("com.example")
        }
        if (state.artifactIdProperty.get().isBlank()) {
            state.artifactIdProperty.set(normalizeArtifactId(initialProjectName))
        }

        if (state.manifestNameProperty.get().isBlank()) {
            state.manifestNameProperty.set(initialProjectName)
        }
        if (state.manifestMainProperty.get().isBlank()) {
            val nameForSuggestion = state.manifestNameProperty.get().ifBlank { initialProjectName }
            state.manifestMainProperty.set(suggestMainClassFqn(state.groupIdProperty.get(), nameForSuggestion))
        }
        if (state.manifestDescriptionProperty.get().isBlank()) {
            state.manifestDescriptionProperty.set("A Hytale plugin.")
        }

        baseData?.nameProperty?.afterChange { newName ->
            val oldName = lastProjectName
            val trimmedNewName = newName.trim()

            val currentModName = state.manifestNameProperty.get().trim()
            if (currentModName.isBlank() || currentModName == oldName) {
                state.manifestNameProperty.set(trimmedNewName)
            }

            val currentArtifactId = state.artifactIdProperty.get().trim()
            val oldSuggestedArtifactId = normalizeArtifactId(oldName)
            val newSuggestedArtifactId = normalizeArtifactId(trimmedNewName)
            if (currentArtifactId.isBlank() || currentArtifactId == oldSuggestedArtifactId) {
                state.artifactIdProperty.set(newSuggestedArtifactId)
            }

            val groupId = state.groupIdProperty.get()
            val currentMainClass = state.manifestMainProperty.get().trim()
            val oldSuggestedMain = suggestMainClassFqn(groupId, oldName)
            val newSuggestedMain = suggestMainClassFqn(groupId, trimmedNewName)
            if (currentMainClass.isBlank() || currentMainClass == oldSuggestedMain) {
                state.manifestMainProperty.set(newSuggestedMain)
            }

            lastProjectName = trimmedNewName
        }

        var lastModName = state.manifestNameProperty.get().trim()
        state.manifestNameProperty.afterChange { newModName ->
            val oldModName = lastModName
            val trimmedNewModName = newModName.trim()

            val currentArtifactId = state.artifactIdProperty.get().trim()
            val oldSuggestedArtifactId = normalizeArtifactId(oldModName)
            val newSuggestedArtifactId = normalizeArtifactId(trimmedNewModName)
            if (currentArtifactId.isBlank() || currentArtifactId == oldSuggestedArtifactId) {
                state.artifactIdProperty.set(newSuggestedArtifactId)
            }

            val groupId = state.groupIdProperty.get()
            val currentMainClass = state.manifestMainProperty.get().trim()
            val oldSuggestedMain = suggestMainClassFqn(groupId, oldModName)
            val newSuggestedMain = suggestMainClassFqn(groupId, trimmedNewModName)
            if (currentMainClass.isBlank() || currentMainClass == oldSuggestedMain) {
                state.manifestMainProperty.set(newSuggestedMain)
            }

            lastModName = trimmedNewModName
        }

        var lastGroupId = state.groupIdProperty.get()
        state.groupIdProperty.afterChange { newGroupId ->
            val oldGroupId = lastGroupId

            val effectiveProjectName = state.manifestNameProperty.get().ifBlank { lastProjectName }
            val oldSuggestedMain = suggestMainClassFqn(oldGroupId, effectiveProjectName)
            val newSuggestedMain = suggestMainClassFqn(newGroupId, effectiveProjectName)
            if (state.manifestMainProperty.get().isBlank() || state.manifestMainProperty.get() == oldSuggestedMain) {
                state.manifestMainProperty.set(newSuggestedMain)
            }

            lastGroupId = newGroupId
        }

        state.serverDirProperty.afterChange { serverDir ->
            if (serverDir.isBlank() || state.assetsPathProperty.get().isNotBlank()) return@afterChange

            val dirPath = runCatching { Path.of(serverDir) }.getOrNull() ?: return@afterChange
            val candidate = dirPath.resolve("Assets.zip")

            ApplicationManager.getApplication().executeOnPooledThread {
                if (candidate.isRegularFile()) {
                    ApplicationManager.getApplication().invokeLater {
                        if (state.assetsPathProperty.get().isBlank()) {
                            state.assetsPathProperty.set(candidate.toString())
                        }
                    }
                }
            }
        }
    }
}
