package sh.harold.hytaledev.wizard.steps

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardBaseData
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
        val baseData = NewProjectWizardBaseData.getBaseData(this)

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
            state.artifactIdProperty.set(normalizeArtifactId(baseData.name))
        }

        if (state.manifestNameProperty.get().isBlank()) {
            state.manifestNameProperty.set(baseData.name)
        }
        if (state.manifestGroupProperty.get().isBlank()) {
            state.manifestGroupProperty.set(state.groupIdProperty.get())
        }
        if (state.manifestMainProperty.get().isBlank()) {
            state.manifestMainProperty.set(suggestMainClassFqn(state.groupIdProperty.get(), baseData.name))
        }

        var lastProjectName = baseData.name
        baseData.nameProperty.afterChange { newProjectName ->
            val oldProjectName = lastProjectName
            val oldSuggestedMain = suggestMainClassFqn(state.groupIdProperty.get(), oldProjectName)
            val newSuggestedMain = suggestMainClassFqn(state.groupIdProperty.get(), newProjectName)

            if (state.artifactIdProperty.get().isBlank() || state.artifactIdProperty.get() == normalizeArtifactId(oldProjectName)) {
                state.artifactIdProperty.set(normalizeArtifactId(newProjectName))
            }

            if (state.manifestNameProperty.get().isBlank() || state.manifestNameProperty.get() == oldProjectName) {
                state.manifestNameProperty.set(newProjectName)
            }

            if (state.manifestMainProperty.get().isBlank() || state.manifestMainProperty.get() == oldSuggestedMain) {
                state.manifestMainProperty.set(newSuggestedMain)
            }

            lastProjectName = newProjectName
        }

        var lastGroupId = state.groupIdProperty.get()
        state.groupIdProperty.afterChange { newGroupId ->
            val oldGroupId = lastGroupId

            if (state.manifestGroupProperty.get().isBlank() || state.manifestGroupProperty.get() == oldGroupId) {
                state.manifestGroupProperty.set(newGroupId)
            }

            val projectName = baseData.name
            val oldSuggestedMain = suggestMainClassFqn(oldGroupId, projectName)
            val newSuggestedMain = suggestMainClassFqn(newGroupId, projectName)
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
