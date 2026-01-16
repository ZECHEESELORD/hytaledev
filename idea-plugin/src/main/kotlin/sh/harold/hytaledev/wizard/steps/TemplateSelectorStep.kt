package sh.harold.hytaledev.wizard.steps

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.Panel
import sh.harold.hytaledev.templates.HytaleDevTemplateService
import sh.harold.hytaledev.templates.TemplateIndex
import sh.harold.hytaledev.model.WizardState
import javax.swing.DefaultComboBoxModel

class TemplateSelectorStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    private val state = WizardState.get(this)
    private val model = DefaultComboBoxModel<TemplateOption>()
    private val comboBox = ComboBox(model)
    private val statusLabel = JBLabel()

    private var loadQueued = false

    init {
        comboBox.addActionListener {
            val selected = comboBox.selectedItem as? TemplateOption ?: return@addActionListener
            state.templateIdProperty.set(selected.id)
        }
    }

    override fun setupUI(builder: Panel) {
        builder.group("Template") {
            row("Template:") {
                cell(comboBox)
            }.comment("Builtin templates are always available; additional repositories can be configured in Settings.")

            row {
                cell(statusLabel)
            }
        }

        if (!loadQueued) {
            loadQueued = true
            loadTemplates()
        }
    }

    private fun loadTemplates() {
        comboBox.isEnabled = false
        statusLabel.text = "Loading templates…"

        object : Task.Backgroundable(null, "Load Hytale templates", true) {
            private lateinit var index: TemplateIndex

            override fun run(indicator: ProgressIndicator) {
                index = service<HytaleDevTemplateService>().loadTemplateIndex()
            }

            override fun onSuccess() {
                applyIndex(index)
            }

            override fun onThrowable(error: Throwable) {
                statusLabel.text = error.message ?: "Failed to load templates"
                comboBox.isEnabled = true
            }
        }.queue()
    }

    private fun applyIndex(index: TemplateIndex) {
        model.removeAllElements()

        index.templates.forEach { template ->
            model.addElement(TemplateOption(id = template.id, label = template.label))
        }

        val options = (0 until model.size).map { model.getElementAt(it) }
        val desiredId = state.templateIdProperty.get()
        val selected = options.firstOrNull { it.id == desiredId } ?: options.firstOrNull()

        if (selected != null) {
            comboBox.selectedItem = selected
            state.templateIdProperty.set(selected.id)
        }

        comboBox.isEnabled = model.size > 0

        if (model.size == 0) {
            statusLabel.text = "No templates found. Configure repositories in Settings."
            statusLabel.toolTipText = null
            return
        }

        if (index.problems.isEmpty()) {
            statusLabel.text = ""
            statusLabel.toolTipText = null
            return
        }

        statusLabel.text = "Some repositories failed to load. Check Settings or idea.log."
        statusLabel.toolTipText = index.problems.joinToString(separator = "\n")
    }
}

private data class TemplateOption(
    val id: String,
    val label: String,
) {
    override fun toString(): String = label
}
