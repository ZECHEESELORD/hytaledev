package sh.harold.hytaledev.templating

import java.nio.file.Path

class ProjectGenerator {
    fun generate(destinationDir: Path, template: TemplateDescriptor, values: Map<String, String>): GenerationResult {
        return GenerationResult(destinationDir = destinationDir, templateId = template.id)
    }
}

data class GenerationResult(
    val destinationDir: Path,
    val templateId: String,
)
