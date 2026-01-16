package sh.harold.hytaledev.wizard.steps

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.rows
import com.intellij.ui.layout.ValidationInfoBuilder
import sh.harold.hytaledev.model.WizardState
import sh.harold.hytaledev.wizard.isValidJavaFqn

class ManifestStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    private val state = WizardState.get(this)

    override fun setupUI(builder: Panel) {
        builder.group("Hytale Plugin Manifest") {
            row("Group:") {
                textField()
                    .bindText(state.manifestGroupProperty)
                    .validationOnInput(::validateRequiredText)
                    .validationOnApply(::validateRequiredText)
            }
            row("Name:") {
                textField()
                    .bindText(state.manifestNameProperty)
                    .validationOnInput(::validateRequiredText)
                    .validationOnApply(::validateRequiredText)
            }
            row("Version:") {
                textField()
                    .bindText(state.manifestVersionProperty)
                    .validationOnInput(::validateRequiredText)
                    .validationOnApply(::validateRequiredText)
            }
            row("Main:") {
                textField()
                    .bindText(state.manifestMainProperty)
                    .validationOnInput(::validateMainFqn)
                    .validationOnApply(::validateMainFqn)
            }
            row("Description:") {
                textArea()
                    .rows(3)
                    .bindText(state.manifestDescriptionProperty)
                    .validationOnInput(::validateRequiredTextArea)
                    .validationOnApply(::validateRequiredTextArea)
            }

            row("Website:") {
                textField().bindText(state.manifestWebsiteProperty)
            }
            row("Authors:") {
                label("v1 supports a single author entry (optional).")
            }
            row("Author name:") {
                textField().bindText(state.authorNameProperty)
            }
            row("Author email:") {
                textField().bindText(state.authorEmailProperty)
            }
            row("Author url:") {
                textField().bindText(state.authorUrlProperty)
            }

            row("ServerVersion:") {
                textField()
                    .bindText(state.manifestServerVersionProperty)
                    .comment("Use \"*\" for any server version.")
            }
            row {
                checkBox("DisabledByDefault").bindSelected(state.manifestDisabledByDefaultProperty)
                checkBox("IncludesAssetPack").bindSelected(state.manifestIncludesAssetPackProperty)
            }

            groupRowsRange("Dependencies (map)", false) {
                row {
                    textArea()
                        .rows(4)
                        .bindText(state.dependenciesTextProperty)
                        .comment("One per line: key=value (e.g. Hytale:SomeDependency=*)")
                }
            }
            groupRowsRange("OptionalDependencies (map)", false) {
                row {
                    textArea()
                        .rows(3)
                        .bindText(state.optionalDependenciesTextProperty)
                        .comment("One per line: key=value")
                }
            }
            groupRowsRange("LoadBefore (map)", false) {
                row {
                    textArea()
                        .rows(3)
                        .bindText(state.loadBeforeTextProperty)
                        .comment("One per line: key=value")
                }
            }

            collapsibleGroup("Sub-plugin (advanced)", false) {
                row {
                    checkBox("Enable sub-plugin").bindSelected(state.subPluginEnabledProperty)
                }

                rowsRange {
                    row("Name:") {
                        textField()
                            .bindText(state.subPluginNameProperty)
                            .validationOnApply { b, c ->
                                if (state.subPluginEnabledProperty.get() && c.text.trim().isBlank()) b.error("Required") else null
                            }
                    }
                    row("Main:") {
                        textField()
                            .bindText(state.subPluginMainProperty)
                            .validationOnApply { b, c ->
                                if (!state.subPluginEnabledProperty.get()) return@validationOnApply null
                                validateMainFqn(b, c)
                            }
                    }
                    row("Version:") {
                        textField()
                            .bindText(state.subPluginVersionProperty)
                            .validationOnApply { b, c ->
                                if (state.subPluginEnabledProperty.get() && c.text.trim().isBlank()) b.error("Required") else null
                            }
                    }
                    row("Description:") {
                        textArea()
                            .rows(2)
                            .bindText(state.subPluginDescriptionProperty)
                            .validationOnApply { b, c ->
                                if (state.subPluginEnabledProperty.get() && c.text.trim().isBlank()) b.error("Required") else null
                            }
                    }
                    row("Website:") {
                        textField().bindText(state.subPluginWebsiteProperty)
                    }
                    row("ServerVersion:") {
                        textField().bindText(state.subPluginServerVersionProperty)
                    }
                    row {
                        checkBox("DisabledByDefault").bindSelected(state.subPluginDisabledByDefaultProperty)
                        checkBox("IncludesAssetPack").bindSelected(state.subPluginIncludesAssetPackProperty)
                    }
                    groupRowsRange("Dependencies (map)", false) {
                        row {
                            textArea().rows(3).bindText(state.subPluginDependenciesTextProperty)
                        }
                    }
                    groupRowsRange("OptionalDependencies (map)", false) {
                        row {
                            textArea().rows(3).bindText(state.subPluginOptionalDependenciesTextProperty)
                        }
                    }
                    groupRowsRange("LoadBefore (map)", false) {
                        row {
                            textArea().rows(3).bindText(state.subPluginLoadBeforeTextProperty)
                        }
                    }
                }.visibleIf(state.subPluginEnabledProperty)
            }
        }
    }

    private fun validateRequiredText(builder: ValidationInfoBuilder, component: javax.swing.JTextField) =
        if (component.text.trim().isBlank()) builder.error("Required") else null

    private fun validateRequiredTextArea(builder: ValidationInfoBuilder, component: javax.swing.JTextArea) =
        if (component.text.trim().isBlank()) builder.error("Required") else null

    private fun validateMainFqn(builder: ValidationInfoBuilder, component: javax.swing.JTextField) =
        when {
            component.text.trim().isBlank() -> builder.error("Required")
            !isValidJavaFqn(component.text) -> builder.error("Invalid class FQN")
            else -> null
        }
}
