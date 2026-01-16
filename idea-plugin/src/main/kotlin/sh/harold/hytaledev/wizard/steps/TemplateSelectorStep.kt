package sh.harold.hytaledev.wizard.steps

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bind
import sh.harold.hytaledev.model.WizardState

class TemplateSelectorStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    private val state = WizardState.get(this)

    override fun setupUI(builder: Panel) {
        builder.group("Template") {
            buttonsGroup {
                row {
                    radioButton("Basic", WizardState.DEFAULT_TEMPLATE_ID)
                }.comment("Builtin templates are always available; additional repositories will be configurable in Settings.")
            }.bind({ state.templateIdProperty.get() }, { state.templateIdProperty.set(it) })
        }
    }
}
