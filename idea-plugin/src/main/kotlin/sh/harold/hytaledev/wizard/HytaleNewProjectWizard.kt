package sh.harold.hytaledev.wizard

import com.intellij.ide.wizard.GeneratorNewProjectWizard
import com.intellij.ide.wizard.GitNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardBaseStep
import com.intellij.ide.wizard.NewProjectWizardChainStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.RootNewProjectWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.util.IconLoader
import sh.harold.hytaledev.model.WizardState
import sh.harold.hytaledev.wizard.steps.BuildSystemStep
import sh.harold.hytaledev.wizard.steps.GroupSelectorStep
import sh.harold.hytaledev.wizard.steps.JdkStep
import sh.harold.hytaledev.wizard.steps.ManifestStep
import sh.harold.hytaledev.wizard.steps.GenerateProjectStep
import sh.harold.hytaledev.wizard.steps.RootStep
import sh.harold.hytaledev.wizard.steps.ServerConfigStep
import sh.harold.hytaledev.wizard.steps.SummaryCommentStep
import sh.harold.hytaledev.wizard.steps.TemplateSelectorStep

class HytaleNewProjectWizard : GeneratorNewProjectWizard {
    override val id: String = "sh.harold.hytaledev.generator"
    override val name: String = "Hytale"
    override val icon = IconLoader.getIcon("/icons/hytale.svg", javaClass)

    override fun createStep(context: WizardContext): NewProjectWizardStep {
        val root = RootNewProjectWizardStep(context)
        root.data.putUserData(WizardState.KEY, WizardState(root.propertyGraph))

        return NewProjectWizardChainStep(root)
            .nextStep(::NewProjectWizardBaseStep)
            .nextStep(::GitNewProjectWizardStep)
            .nextStep(::RootStep)
            .nextStep(::GroupSelectorStep)
            .nextStep(::TemplateSelectorStep)
            .nextStep(::BuildSystemStep)
            .nextStep(::JdkStep)
            .nextStep(::ManifestStep)
            .nextStep(::ServerConfigStep)
            .nextStep(::SummaryCommentStep)
            .nextStep(::GenerateProjectStep)
    }
}
