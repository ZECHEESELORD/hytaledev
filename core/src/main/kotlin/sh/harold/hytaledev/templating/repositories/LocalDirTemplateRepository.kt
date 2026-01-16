package sh.harold.hytaledev.templating.repositories

import sh.harold.hytaledev.templating.TEMPLATE_DESCRIPTOR_SUFFIX
import sh.harold.hytaledev.templating.Template
import sh.harold.hytaledev.templating.TemplateDescriptor
import sh.harold.hytaledev.templating.TemplateDescriptorReader
import sh.harold.hytaledev.templating.TemplateProvider
import sh.harold.hytaledev.templating.TemplateRepository
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.streams.asSequence

class LocalDirTemplateRepository(
    private val rootDir: Path,
    private val descriptorReader: TemplateDescriptorReader = TemplateDescriptorReader(),
) : TemplateRepository {
    override val provider: TemplateProvider = TemplateProvider.LocalDir(rootDir)

    private val templatesById: Map<String, FileSystemTemplateEntry> by lazy {
        scanTemplates()
    }

    override fun listTemplates(): List<TemplateDescriptor> = templatesById.values.map { it.descriptor }

    override fun openTemplate(templateId: String): Template? {
        val entry = templatesById[templateId] ?: return null
        return FileSystemTemplate(entry)
    }

    private fun scanTemplates(): Map<String, FileSystemTemplateEntry> {
        require(Files.isDirectory(rootDir)) { "Template directory does not exist or is not a directory: $rootDir" }

        val entries = Files.walk(rootDir).use { stream ->
            stream.asSequence()
                .filter { it.isRegularFile() }
                .filter { it.fileName.toString().endsWith(TEMPLATE_DESCRIPTOR_SUFFIX) }
                .map { descriptorPath ->
                    val descriptor = descriptorReader.read(descriptorPath)
                    FileSystemTemplateEntry(descriptor = descriptor, root = descriptorPath.parent)
                }
                .toList()
        }

        val duplicates = entries.groupBy { it.descriptor.id }.filterValues { it.size > 1 }
        if (duplicates.isNotEmpty()) {
            val message = buildString {
                appendLine("Duplicate template ids detected in: $rootDir")
                duplicates.forEach { (id, items) ->
                    appendLine("  $id:")
                    items.forEach { appendLine("    - ${it.root}") }
                }
            }
            error(message.trimEnd())
        }

        return entries.associateBy { it.descriptor.id }
    }
}

private data class FileSystemTemplateEntry(
    val descriptor: TemplateDescriptor,
    val root: Path,
)

private class FileSystemTemplate(
    private val root: Path,
    override val descriptor: TemplateDescriptor,
) : Template {
    constructor(entry: FileSystemTemplateEntry) : this(
        root = entry.root,
        descriptor = entry.descriptor,
    )

    override fun readBytes(relativePath: String): ByteArray {
        val normalized = relativePath.trimStart('/').replace('\\', '/')
        val path = root.resolve(normalized)
        return Files.readAllBytes(path)
    }
}
