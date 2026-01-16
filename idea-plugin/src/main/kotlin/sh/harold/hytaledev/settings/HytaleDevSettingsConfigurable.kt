package sh.harold.hytaledev.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.options.SearchableConfigurable
import javax.swing.JComponent

class HytaleDevSettingsConfigurable : SearchableConfigurable {
    private var repositoriesPanel: TemplateRepositoriesPanel? = null

    override fun getId(): String = "sh.harold.hytaledev.settings"

    override fun getDisplayName(): String = "HytaleDev"

    override fun createComponent(): JComponent {
        val panel = TemplateRepositoriesPanel()
        panel.reset(service<HytaleDevSettingsState>().state)
        repositoriesPanel = panel
        return panel.component
    }

    override fun isModified(): Boolean {
        val panel = repositoriesPanel ?: return false
        return panel.isModified(service<HytaleDevSettingsState>().state)
    }

    override fun apply() {
        val panel = repositoriesPanel ?: return
        panel.applyTo(service<HytaleDevSettingsState>().state)
    }

    override fun reset() {
        repositoriesPanel?.reset(service<HytaleDevSettingsState>().state)
    }

    override fun disposeUIResources() {
        repositoriesPanel = null
    }
}
