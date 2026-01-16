package sh.harold.hytaledev.wizard.steps

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.externalSystem.service.ui.ExternalSystemJdkComboBox
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.layout.ValidationInfoBuilder
import sh.harold.hytaledev.model.WizardState

class JdkStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    private val state = WizardState.get(this)
    private val jdkComboBox = ExternalSystemJdkComboBox()

    init {
        jdkComboBox.addActionListener {
            state.jdkProperty.set(jdkComboBox.selectedJdk)
        }
    }

    override fun setupUI(builder: Panel) {
        builder.group("JDK") {
            row("JDK:") {
                cell(jdkComboBox)
                    .comment("HytaleServer.jar requires JDK 21+")
                    .validationOnApply(::validateJdk)
            }
        }

        state.jdkProperty.set(jdkComboBox.selectedJdk)
    }

    private fun validateJdk(builder: ValidationInfoBuilder, component: ExternalSystemJdkComboBox) =
        when (val sdk = component.selectedJdk) {
            null -> builder.error("Select a JDK (21+)")
            else -> {
                if (!JavaSdk.getInstance().isOfVersionOrHigher(sdk, JavaSdkVersion.JDK_21)) {
                    builder.error("HytaleServer.jar requires JDK 21+")
                } else {
                    null
                }
            }
        }
}
