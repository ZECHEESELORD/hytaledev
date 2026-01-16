package sh.harold.hytaledev.templating

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.name

class ProjectGenerator {
    fun generate(destinationDir: Path, template: Template, values: Map<String, Any?>): GenerationResult {
        require(destinationDir.exists()) { "Destination directory does not exist: $destinationDir" }

        val renderer = VelocityRenderer()
        val created = mutableListOf<GeneratedFile>()

        template.descriptor.files.forEach { file ->
            val condition = file.condition
            if (condition != null) {
                val rendered = renderer.render(condition, values).trim()
                val shouldInclude = when {
                    rendered.isBlank() -> false
                    rendered.equals("true", ignoreCase = true) -> true
                    rendered.equals("false", ignoreCase = true) -> false
                    else -> error("Invalid template condition result for '${template.descriptor.id}': '$rendered' (from '$condition')")
                }
                if (!shouldInclude) return@forEach
            }

            val relativePathRaw = renderer.render(file.destinationPath, values).trim()
            val relativePath = relativePathRaw.trimStart('/', '\\')
            require(relativePath.isNotBlank()) { "Template produced a blank destinationPath for '${template.descriptor.id}'" }

            val targetPath = destinationDir.resolve(relativePath).normalize()
            ensureUnderRoot(destinationDir, targetPath)
            targetPath.parent?.createDirectories()

            if (file.render) {
                val input = template.readBytes(file.templatePath).toString(StandardCharsets.UTF_8)
                val output = renderer.render(input, values)
                Files.writeString(targetPath, output, StandardCharsets.UTF_8)
            } else {
                Files.write(targetPath, template.readBytes(file.templatePath))
            }

            if (targetPath.name == "gradlew") {
                tryMakeExecutable(targetPath)
            }

            created += GeneratedFile(
                relativePath = Path.of(relativePath),
                openInEditor = file.openInEditor,
                reformat = file.reformat,
            )
        }

        return GenerationResult(
            destinationDir = destinationDir,
            templateId = template.descriptor.id,
            files = created,
        )
    }
}

data class GenerationResult(
    val destinationDir: Path,
    val templateId: String,
    val files: List<GeneratedFile> = emptyList(),
)

data class GeneratedFile(
    val relativePath: Path,
    val openInEditor: Boolean = false,
    val reformat: Boolean = false,
)

private fun ensureUnderRoot(root: Path, file: Path) {
    val normalizedRoot = root.normalize()
    val normalizedFile = file.normalize()
    require(normalizedFile.startsWith(normalizedRoot)) {
        "Refusing to write outside destination directory. Root: $normalizedRoot, target: $normalizedFile"
    }
}

private fun tryMakeExecutable(path: Path) {
    runCatching {
        val current = Files.getPosixFilePermissions(path).toMutableSet()
        current += PosixFilePermission.OWNER_EXECUTE
        current += PosixFilePermission.GROUP_EXECUTE
        current += PosixFilePermission.OTHERS_EXECUTE
        Files.setPosixFilePermissions(path, current)
    }
}
