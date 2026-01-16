package sh.harold.hytaledev.wizard.steps

import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.comment.CommentNewProjectWizardStep

class SummaryCommentStep(parent: NewProjectWizardStep) : CommentNewProjectWizardStep(parent) {
    override val comment: String
        get() = """
            The generated plugin JAR will be built by Gradle.
            
            The Hytale Server run configuration (v1) will build the plugin and copy it into the server's mods folder before launch.
        """.trimIndent()
}
