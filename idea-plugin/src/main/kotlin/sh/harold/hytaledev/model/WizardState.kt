package sh.harold.hytaledev.model

import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.Key

class WizardState(propertyGraph: PropertyGraph) {
    val templateIdProperty: GraphProperty<String> = propertyGraph.property(DEFAULT_TEMPLATE_ID)

    val buildSystemProperty: GraphProperty<BuildSystem> = propertyGraph.property(BuildSystem.Gradle)
    val languageProperty: GraphProperty<Language> = propertyGraph.property(Language.Kotlin)

    val groupIdProperty: GraphProperty<String> = propertyGraph.property("")
    val artifactIdProperty: GraphProperty<String> = propertyGraph.property("")
    val projectVersionProperty: GraphProperty<String> = propertyGraph.property(DEFAULT_VERSION)

    val jdkProperty: GraphProperty<Sdk?> = propertyGraph.property(null)

    val manifestNameProperty: GraphProperty<String> = propertyGraph.property("")
    val manifestMainProperty: GraphProperty<String> = propertyGraph.property("")
    val manifestDescriptionProperty: GraphProperty<String> = propertyGraph.property("")
    val manifestWebsiteProperty: GraphProperty<String> = propertyGraph.property("")
    val manifestServerVersionProperty: GraphProperty<String> = propertyGraph.property(DEFAULT_SERVER_VERSION)
    val manifestDisabledByDefaultProperty: GraphProperty<Boolean> = propertyGraph.property(false)
    val manifestIncludesAssetPackProperty: GraphProperty<Boolean> = propertyGraph.property(false)

    val authorNameProperty: GraphProperty<String> = propertyGraph.property("")
    val authorEmailProperty: GraphProperty<String> = propertyGraph.property("")
    val authorUrlProperty: GraphProperty<String> = propertyGraph.property("")

    val dependenciesTextProperty: GraphProperty<String> = propertyGraph.property("")
    val optionalDependenciesTextProperty: GraphProperty<String> = propertyGraph.property("")
    val loadBeforeTextProperty: GraphProperty<String> = propertyGraph.property("")

    val subPluginEnabledProperty: GraphProperty<Boolean> = propertyGraph.property(false)
    val subPluginNameProperty: GraphProperty<String> = propertyGraph.property("")
    val subPluginMainProperty: GraphProperty<String> = propertyGraph.property("")
    val subPluginVersionProperty: GraphProperty<String> = propertyGraph.property(DEFAULT_VERSION)
    val subPluginDescriptionProperty: GraphProperty<String> = propertyGraph.property("")
    val subPluginWebsiteProperty: GraphProperty<String> = propertyGraph.property("")
    val subPluginServerVersionProperty: GraphProperty<String> = propertyGraph.property(DEFAULT_SERVER_VERSION)
    val subPluginDisabledByDefaultProperty: GraphProperty<Boolean> = propertyGraph.property(false)
    val subPluginIncludesAssetPackProperty: GraphProperty<Boolean> = propertyGraph.property(false)
    val subPluginDependenciesTextProperty: GraphProperty<String> = propertyGraph.property("")
    val subPluginOptionalDependenciesTextProperty: GraphProperty<String> = propertyGraph.property("")
    val subPluginLoadBeforeTextProperty: GraphProperty<String> = propertyGraph.property("")

    val serverDirProperty: GraphProperty<String> = propertyGraph.property("")
    val assetsPathProperty: GraphProperty<String> = propertyGraph.property("")
    val serverValidationMessageProperty: GraphProperty<String> = propertyGraph.property("")

    companion object {
        const val DEFAULT_TEMPLATE_ID = "builtin:hytale-plugin-basic"
        const val DEFAULT_VERSION = "1.0-SNAPSHOT"
        const val DEFAULT_SERVER_VERSION = "*"

        val KEY: Key<WizardState> = Key.create("sh.harold.hytaledev.wizard.state")

        fun get(step: NewProjectWizardStep): WizardState {
            return step.data.getUserData(KEY) ?: error("WizardState is not initialized")
        }
    }
}

enum class BuildSystem {
    Gradle,
    Maven,
}

enum class Language {
    Kotlin,
    Java,
}
