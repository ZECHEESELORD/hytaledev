package sh.harold.hytaledev.wizard.steps

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.rows
import sh.harold.hytaledev.model.WizardState
import sh.harold.hytaledev.wizard.isValidJavaFqn

class ManifestStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    private val state = WizardState.get(this)

    override fun setupUI(builder: Panel) {
        builder.collapsibleGroup("Optional Settings", false) {
            row("Description:") {
                textArea()
                    .rows(3)
                    .bindText(state.manifestDescriptionProperty)
                    .comment("Shown in server logs and admin tools.")
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
                            .validationOnApply { field ->
                                if (state.subPluginEnabledProperty.get() && field.text.trim().isBlank()) error("Required") else null
                            }
                    }
                    row("Main:") {
                        textField()
                            .bindText(state.subPluginMainProperty)
                            .validationOnApply { field ->
                                if (!state.subPluginEnabledProperty.get()) return@validationOnApply null
                                when {
                                    field.text.trim().isBlank() -> error("Required")
                                    !isValidJavaFqn(field.text) -> error("Invalid class FQN")
                                    else -> null
                                }
                            }
                    }
                    row("Version:") {
                        textField()
                            .bindText(state.subPluginVersionProperty)
                            .validationOnApply { field ->
                                if (state.subPluginEnabledProperty.get() && field.text.trim().isBlank()) error("Required") else null
                            }
                    }
                    row("Description:") {
                        textArea()
                            .rows(2)
                            .bindText(state.subPluginDescriptionProperty)
                            .validationOnApply { area ->
                                if (state.subPluginEnabledProperty.get() && area.text.trim().isBlank()) error("Required") else null
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
}
