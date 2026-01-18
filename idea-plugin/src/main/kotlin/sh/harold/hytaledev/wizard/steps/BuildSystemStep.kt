package sh.harold.hytaledev.wizard.steps

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import sh.harold.hytaledev.model.BuildSystem
import sh.harold.hytaledev.model.Language
import sh.harold.hytaledev.model.WizardState
import sh.harold.hytaledev.wizard.isValidJavaFqn

class BuildSystemStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    private val state = WizardState.get(this)

    override fun setupUI(builder: Panel) {
        builder.row("Build System:") {
            segmentedButton(BuildSystem.entries) { buildSystem ->
                text = when (buildSystem) {
                    BuildSystem.Gradle -> "Gradle"
                    BuildSystem.Maven -> "Maven"
                }
            }.bind(state.buildSystemProperty)
        }

        builder.row("Language:") {
            segmentedButton(Language.entries) { language ->
                text = when (language) {
                    Language.Kotlin -> "Kotlin"
                    Language.Java -> "Java"
                }
            }.bind(state.languageProperty)
        }

        builder.row("Hytale Version:") {
            textField()
                .bindText(state.manifestServerVersionProperty)
                .comment("Use \"*\" for any server version.")
        }

        builder.row("Mod Name:") {
            textField()
                .bindText(state.manifestNameProperty)
                .validationOnInput { field -> if (field.text.trim().isBlank()) error("Required") else null }
                .validationOnApply { field -> if (field.text.trim().isBlank()) error("Required") else null }
        }

        builder.row("Main Class:") {
            textField()
                .bindText(state.manifestMainProperty)
                .validationOnInput { field ->
                    when {
                        field.text.trim().isBlank() -> error("Required")
                        !isValidJavaFqn(field.text) -> error("Invalid class FQN")
                        else -> null
                    }
                }
                .validationOnApply { field ->
                    when {
                        field.text.trim().isBlank() -> error("Required")
                        !isValidJavaFqn(field.text) -> error("Invalid class FQN")
                        else -> null
                    }
                }
        }
    }
}
