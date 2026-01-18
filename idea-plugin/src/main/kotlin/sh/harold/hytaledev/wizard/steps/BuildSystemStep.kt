package sh.harold.hytaledev.wizard.steps

import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.util.projectWizard.ProjectWizardUtil
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.AbstractWizard
import com.intellij.openapi.Disposable
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindText
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.roots.ui.configuration.validateSdk
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Disposer
import javax.swing.JComponent
import sh.harold.hytaledev.model.BuildSystem
import sh.harold.hytaledev.model.Language
import sh.harold.hytaledev.model.WizardState
import sh.harold.hytaledev.wizard.isValidGroupId

class BuildSystemStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    private val state = WizardState.get(this)

    private val sdkModel = ProjectSdksModel()
    private lateinit var jdkComboBox: HytaleJdkComboBox
    private var jdkComboInitialized = false

    override fun setupUI(builder: Panel) {
        builder.group("Build System") {
            row("Build system:") {
                segmentedButton(BuildSystem.entries) { buildSystem ->
                    text = when (buildSystem) {
                        BuildSystem.Gradle -> "Gradle"
                        BuildSystem.Maven -> "Maven"
                    }
                }.bind(state.buildSystemProperty)
            }

            row("Language:") {
                segmentedButton(Language.entries) { language ->
                    text = when (language) {
                        Language.Kotlin -> "Kotlin"
                        Language.Java -> "Java"
                    }
                }.bind(state.languageProperty)
            }

            row("Group ID:") {
                textField()
                    .bindText(state.groupIdProperty)
                    .validationOnInput { field ->
                        when {
                            field.text.trim().isBlank() -> error("Required")
                            !isValidGroupId(field.text) -> error("Invalid group ID")
                            else -> null
                        }
                    }
                    .validationOnApply { field ->
                        when {
                            field.text.trim().isBlank() -> error("Required")
                            !isValidGroupId(field.text) -> error("Invalid group ID")
                            else -> null
                        }
                    }
            }
            row("Artifact ID:") {
                textField()
                    .bindText(state.artifactIdProperty)
                    .validationOnInput { field -> if (field.text.trim().isBlank()) error("Required") else null }
                    .validationOnApply { field -> if (field.text.trim().isBlank()) error("Required") else null }
            }
            row("Version:") {
                textField()
                    .bindText(state.projectVersionProperty)
                    .validationOnInput { field -> if (field.text.trim().isBlank()) error("Required") else null }
                    .validationOnApply { field -> if (field.text.trim().isBlank()) error("Required") else null }
            }

            row("JDK:") {
                initializeJdkComboBox()

                cell(jdkComboBox)
                    .align(Align.FILL)
                    .comment("Recommended: JDK 25 (JDK 21+ required)")
                    .validationOnApply {
                        validateSdk(state.jdkProperty, sdkModel) ?: run {
                            val sdk = state.jdkProperty.get() ?: return@run error("Select a JDK (25 recommended, 21+ required)")
                            if (!JavaSdk.getInstance().isOfVersionOrHigher(sdk, JavaSdkVersion.JDK_21)) {
                                error("HytaleServer.jar requires JDK 21+")
                            } else {
                                null
                            }
                        }
                    }
                    .onApply { context.projectJdk = state.jdkProperty.get() }
            }
        }
    }

    private fun initializeJdkComboBox() {
        if (jdkComboInitialized) return
        jdkComboInitialized = true

        Disposer.register(context.disposable) {
            sdkModel.disposeUIResources()
        }

        val project = context.project
        sdkModel.reset(project)

        val sdkFilter = Condition<Sdk> { sdk ->
            JavaSdk.getInstance().isOfVersionOrHigher(sdk, JavaSdkVersion.JDK_21)
        }

        jdkComboBox = HytaleJdkComboBox(project, sdkModel, sdkFilter)

        val selectedJdkKey = "jdk.selected.hytaledev"
        val properties = project?.let(PropertiesComponent::getInstance) ?: PropertiesComponent.getInstance()

        jdkComboBox.addActionListener {
            val sdk = jdkComboBox.selectedJdk
            state.jdkProperty.set(sdk)
            if (sdk != null) {
                properties.setValue(selectedJdkKey, sdk.name)
            }
        }

        val alreadySelected = state.jdkProperty.get()
        if (alreadySelected != null) {
            jdkComboBox.setSelectedJdk(alreadySelected)
        } else {
            val lastUsedSdkName = properties.getValue(selectedJdkKey)
            ProjectWizardUtil.preselectJdkForNewModule(project, lastUsedSdkName, jdkComboBox) { true }

            if (lastUsedSdkName.isNullOrBlank()) {
                selectPreferredJdk25()
            }

            state.jdkProperty.set(jdkComboBox.selectedJdk)
        }

        val windowChild = context.getUserData(AbstractWizard.KEY)?.contentPanel
        if (windowChild != null) {
            jdkComboBox.loadSuggestions(windowChild, context.disposable)
        }
    }

    private fun selectPreferredJdk25() {
        val preferred = choosePreferredJdk(sdkModel.sdks, preferredFeatureVersion = 25) ?: return
        jdkComboBox.setSelectedJdk(preferred)
    }

    private fun choosePreferredJdk(sdks: Array<Sdk>, preferredFeatureVersion: Int): Sdk? {
        val javaSdk = JavaSdk.getInstance()

        val candidates = sdks
            .asSequence()
            .filter { javaSdk.isOfVersionOrHigher(it, JavaSdkVersion.JDK_21) }
            .toList()

        return candidates.firstOrNull { it.featureVersion() == preferredFeatureVersion }
            ?: candidates.maxByOrNull { it.featureVersion() ?: 0 }
    }

    private fun Sdk.featureVersion(): Int? {
        val versionString = versionString ?: return null
        val match = VERSION_REGEX.find(versionString) ?: return null

        val first = match.groupValues[1].toIntOrNull() ?: return null
        val second = match.groupValues.getOrNull(2)?.toIntOrNull()

        return if (first == 1 && second != null) second else first
    }

    private class HytaleJdkComboBox(
        project: com.intellij.openapi.project.Project?,
        model: ProjectSdksModel,
        sdkFilter: Condition<in Sdk>?,
    ) : JdkComboBox(project, model, null, sdkFilter, null, null, null) {
        fun loadSuggestions(windowChild: JComponent, disposable: Disposable) {
            myModel.detectItems(windowChild, disposable)
        }
    }

    private companion object {
        private val VERSION_REGEX = Regex("""\b(\d+)(?:\.(\d+))?""")
    }
}
