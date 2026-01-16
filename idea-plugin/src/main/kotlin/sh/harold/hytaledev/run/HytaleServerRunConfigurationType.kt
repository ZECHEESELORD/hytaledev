package sh.harold.hytaledev.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.openapi.util.IconLoader

class HytaleServerRunConfigurationType : ConfigurationTypeBase(
    ID,
    "Hytale Server",
    "Runs a Hytale server and deploys the current plugin into its mods folder.",
    IconLoader.getIcon("/icons/hytale.svg", HytaleServerRunConfigurationType::class.java),
) {
    init {
        addFactory(HytaleServerRunConfigurationFactory(this))
    }

    private class HytaleServerRunConfigurationFactory(type: HytaleServerRunConfigurationType) : ConfigurationFactory(type) {
        override fun getId(): String = ID

        override fun createTemplateConfiguration(project: com.intellij.openapi.project.Project): HytaleServerRunConfiguration {
            return HytaleServerRunConfiguration(project, this, "Hytale Server (dev)")
        }
    }

    companion object {
        const val ID = "sh.harold.hytaledev.hytaleServer"
    }
}
