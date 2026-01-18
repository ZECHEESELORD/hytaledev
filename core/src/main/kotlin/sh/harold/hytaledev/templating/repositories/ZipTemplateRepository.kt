package sh.harold.hytaledev.templating.repositories

import sh.harold.hytaledev.templating.TEMPLATE_DESCRIPTOR_SUFFIX
import sh.harold.hytaledev.templating.Template
import sh.harold.hytaledev.templating.TemplateDescriptor
import sh.harold.hytaledev.templating.TemplateDescriptorReader
import sh.harold.hytaledev.templating.TemplateProvider
import sh.harold.hytaledev.templating.TemplateRepository
import java.nio.file.Path
import java.util.Enumeration
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class ZipTemplateRepository(
    private val zipPath: Path,
    private val providerOverride: TemplateProvider,
    private val descriptorReader: TemplateDescriptorReader = TemplateDescriptorReader(),
) : TemplateRepository {
    override val provider: TemplateProvider = providerOverride

    private val templatesById: Map<String, ZipTemplateEntry> by lazy {
        scanTemplates()
    }

    override fun listTemplates(): List<TemplateDescriptor> = templatesById.values.map { it.descriptor }

    override fun openTemplate(templateId: String): Template? {
        val entry = templatesById[templateId] ?: return null
        return ZipTemplate(zipPath, entry)
    }

    private fun scanTemplates(): Map<String, ZipTemplateEntry> {
        val entries = ZipFile(zipPath.toFile()).use { zip ->
            zip.entries().asSequence()
                .filter { !it.isDirectory }
                .filter { it.name.endsWith(TEMPLATE_DESCRIPTOR_SUFFIX) }
                .map { descriptorEntry ->
                    val descriptor = zip.getInputStream(descriptorEntry).use { descriptorReader.read(it) }
                    val basePath = descriptorEntry.name.substringBeforeLast('/', missingDelimiterValue = "")
                    ZipTemplateEntry(descriptor = descriptor, basePath = basePath)
                }
                .toList()
        }

        val duplicates = entries.groupBy { it.descriptor.id }.filterValues { it.size > 1 }
        if (duplicates.isNotEmpty()) {
            val message = buildString {
                appendLine("Duplicate template ids detected in: $zipPath")
                duplicates.forEach { (id, items) ->
                    appendLine("  $id:")
                    items.forEach { appendLine("    - ${it.basePath.ifBlank { "<root>" }}") }
                }
            }
            error(message.trimEnd())
        }

        return entries.associateBy { it.descriptor.id }
    }
}

private data class ZipTemplateEntry(
    val descriptor: TemplateDescriptor,
    val basePath: String,
)

private class ZipTemplate(
    zipPath: Path,
    private val basePath: String,
    override val descriptor: TemplateDescriptor,
) : Template {
    private val zip = ZipFile(zipPath.toFile())

    constructor(zipPath: Path, entry: ZipTemplateEntry) : this(
        zipPath = zipPath,
        basePath = entry.basePath,
        descriptor = entry.descriptor,
    )

    override fun readBytes(relativePath: String): ByteArray {
        val normalized = relativePath.trimStart('/').replace('\\', '/')
        val entryName = if (basePath.isBlank()) normalized else "$basePath/$normalized"
        val zipEntry = zip.getEntry(entryName) ?: error("Missing template entry: $entryName")
        return zip.getInputStream(zipEntry).use { it.readBytes() }
    }

    override fun close() {
        zip.close()
    }
}

private fun <T> Enumeration<T>.asSequence(): Sequence<T> = sequence {
    while (hasMoreElements()) {
        yield(nextElement())
    }
}
