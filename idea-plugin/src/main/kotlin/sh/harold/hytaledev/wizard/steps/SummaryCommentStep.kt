package sh.harold.hytaledev.wizard.steps

import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.comment.CommentNewProjectWizardStep
import sh.harold.hytaledev.model.BuildSystem
import sh.harold.hytaledev.model.Language
import sh.harold.hytaledev.model.WizardState

class SummaryCommentStep(parent: NewProjectWizardStep) : CommentNewProjectWizardStep(parent) {
    private val state = WizardState.get(this)

    override val comment: String
        get() = """
            The generated plugin will use ${languageLabel()} and will be built by ${buildSystemLabel()}.
            
            The Hytale Server run configuration (v1) will run `${buildCommandHint()}` and copy the plugin JAR into the server's mods folder before launch.
        """.trimIndent()

    private fun buildSystemLabel(): String = when (state.buildSystemProperty.get()) {
        BuildSystem.Gradle -> "Gradle"
        BuildSystem.Maven -> "Maven"
    }

    private fun languageLabel(): String = when (state.languageProperty.get()) {
        Language.Kotlin -> "Kotlin"
        Language.Java -> "Java"
    }

    private fun buildCommandHint(): String = when (state.buildSystemProperty.get()) {
        BuildSystem.Gradle -> "./gradlew build"
        BuildSystem.Maven -> "mvn package"
    }
}
