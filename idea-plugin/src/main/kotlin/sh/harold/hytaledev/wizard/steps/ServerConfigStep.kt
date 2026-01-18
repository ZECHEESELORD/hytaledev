package sh.harold.hytaledev.wizard.steps

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import sh.harold.hytaledev.model.WizardState
import sh.harold.hytaledev.settings.HytaleDevSettingsState
import sh.harold.hytaledev.server.ServerValidator
import java.nio.file.Path

class ServerConfigStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    private val state = WizardState.get(this)

    private val serverDirField = TextFieldWithBrowseButton()
    private val assetsField = TextFieldWithBrowseButton()

    init {
        serverDirField.addBrowseFolderListener(
            "Select Hytale server directory",
            null,
            null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor(),
        )
        assetsField.addBrowseFolderListener(
            "Select Assets.zip",
            null,
            null,
            FileChooserDescriptorFactory.createSingleFileDescriptor("zip"),
        )
    }

    override fun setupUI(builder: Panel) {
        builder.group("Server") {
            row("Mode:") {
                label("Use existing server directory")
            }

            row("Server directory:") {
                cell(serverDirField)
                    .bindText(state.serverDirProperty)
                    .validationOnApply { field -> if (field.text.trim().isBlank()) error("Required") else null }
            }

            row("Assets.zip:") {
                cell(assetsField)
                    .bindText(state.assetsPathProperty)
                    .validationOnApply { field -> if (field.text.trim().isBlank()) error("Required") else null }
            }

            row {
                button("Validate") {
                    validateServer()
                }
                label("").bindText(state.serverValidationMessageProperty)
            }
        }
    }

    private fun validateServer() {
        state.serverValidationMessageProperty.set("Validating…")

        val serverDir = state.serverDirProperty.get().trim()
        val assetsPath = state.assetsPathProperty.get().trim()

        object : Task.Backgroundable(null, "Validate Hytale server", true) {
            private var message: String = ""

            override fun run(indicator: ProgressIndicator) {
                val serverDirPath = runCatching { Path.of(serverDir) }.getOrNull()
                    ?: return run { message = "Invalid server directory path" }
                val assets = assetsPath.takeIf { it.isNotBlank() }?.let { Path.of(it) }

                val result = ServerValidator().validate(serverDirPath, assets)
                message = result.message
            }

            override fun onSuccess() {
                state.serverValidationMessageProperty.set(message)
            }

            override fun onThrowable(error: Throwable) {
                state.serverValidationMessageProperty.set(error.message ?: "Validation failed")
            }
        }.queue()
    }

    override fun setupProject(project: Project) {
        val settings = service<HytaleDevSettingsState>().state
        settings.lastServerDir = state.serverDirProperty.get().trim().ifBlank { null }
        settings.lastAssetsPath = state.assetsPathProperty.get().trim().ifBlank { null }
    }
}
