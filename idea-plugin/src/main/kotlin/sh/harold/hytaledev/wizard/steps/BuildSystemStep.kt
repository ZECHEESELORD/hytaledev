package sh.harold.hytaledev.wizard.steps

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.layout.ValidationInfoBuilder
import sh.harold.hytaledev.model.BuildSystem
import sh.harold.hytaledev.model.Language
import sh.harold.hytaledev.model.WizardState
import sh.harold.hytaledev.wizard.isValidGroupId

class BuildSystemStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    private val state = WizardState.get(this)

    override fun setupUI(builder: Panel) {
        builder.group("Build System") {
            buttonsGroup {
                row("Build system:") {
                    radioButton("Gradle", BuildSystem.Gradle)
                    radioButton("Maven", BuildSystem.Maven).enabled(false)
                }.comment("v1 implements Gradle; Maven is coming soon.")
            }.bind({ state.buildSystemProperty.get() }, { state.buildSystemProperty.set(it) })

            buttonsGroup {
                row("Language:") {
                    radioButton("Kotlin", Language.Kotlin)
                    radioButton("Java", Language.Java).enabled(false)
                }.comment("v1 implements Kotlin; Java is coming soon.")
            }.bind({ state.languageProperty.get() }, { state.languageProperty.set(it) })

            row("Group ID:") {
                textField()
                    .bindText(state.groupIdProperty)
                    .validationOnInput(::validateGroupId)
                    .validationOnApply(::validateGroupId)
            }
            row("Artifact ID:") {
                textField()
                    .bindText(state.artifactIdProperty)
                    .validationOnInput { b, c -> if (c.text.trim().isBlank()) b.error("Required") else null }
                    .validationOnApply { b, c -> if (c.text.trim().isBlank()) b.error("Required") else null }
            }
            row("Version:") {
                textField()
                    .bindText(state.projectVersionProperty)
                    .validationOnInput { b, c -> if (c.text.trim().isBlank()) b.error("Required") else null }
                    .validationOnApply { b, c -> if (c.text.trim().isBlank()) b.error("Required") else null }
            }
        }
    }

    private fun validateGroupId(builder: ValidationInfoBuilder, component: javax.swing.JTextField) =
        when {
            component.text.trim().isBlank() -> builder.error("Required")
            !isValidGroupId(component.text) -> builder.error("Invalid group ID")
            else -> null
        }
}
