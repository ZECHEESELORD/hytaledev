package sh.harold.hytaledev.wizard.steps

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bind
import sh.harold.hytaledev.model.HytaleProjectKind
import sh.harold.hytaledev.model.WizardState

class GroupSelectorStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    private val state = WizardState.get(this)

    override fun setupUI(builder: Panel) {
        builder.group("Group") {
            buttonsGroup {
                row {
                    radioButton("Plugin", HytaleProjectKind.Plugin)
                    radioButton("Mod", HytaleProjectKind.Mod).enabled(false)
                    radioButton("Proxy", HytaleProjectKind.Proxy).enabled(false)
                }.comment("v1 implements Plugin only; Mod and Proxy are coming soon.")
            }.bind({ state.projectKindProperty.get() }, { state.projectKindProperty.set(it) })
        }
    }
}
