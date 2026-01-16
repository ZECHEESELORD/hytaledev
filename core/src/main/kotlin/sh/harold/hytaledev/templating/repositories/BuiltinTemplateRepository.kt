package sh.harold.hytaledev.templating.repositories

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import sh.harold.hytaledev.templating.Template
import sh.harold.hytaledev.templating.TemplateDescriptor
import sh.harold.hytaledev.templating.TemplateDescriptorReader
import sh.harold.hytaledev.templating.TemplateProvider
import sh.harold.hytaledev.templating.TemplateRepository

class BuiltinTemplateRepository(
    private val classLoader: ClassLoader,
    private val indexResourcePath: String = "templates/builtin/index.json",
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
    private val descriptorReader: TemplateDescriptorReader = TemplateDescriptorReader(json),
) : TemplateRepository {
    override val provider: TemplateProvider = TemplateProvider.Builtin

    private val entriesInOrder: List<BuiltinTemplateEntry> by lazy {
        val index = classLoader.getResourceAsStream(indexResourcePath)
            ?: error("Missing builtin template index resource: $indexResourcePath")

        val content = index.use { it.readBytes().decodeToString() }
        val parsed = json.decodeFromString<BuiltinTemplateIndex>(content)

        parsed.templates.map { template ->
            val descriptor = loadDescriptor(template.descriptorPath)
            require(descriptor.id == template.id) {
                "Builtin template index id '${template.id}' does not match descriptor id '${descriptor.id}' for '${template.descriptorPath}'"
            }

            val basePath = template.descriptorPath.substringBeforeLast('/', missingDelimiterValue = "")
            BuiltinTemplateEntry(
                id = template.id,
                descriptor = descriptor,
                baseResourcePath = basePath,
            )
        }
    }

    private val entriesById: Map<String, BuiltinTemplateEntry> by lazy {
        entriesInOrder.associateBy { it.id }
    }

    override fun listTemplates(): List<TemplateDescriptor> = entriesInOrder.map { it.descriptor }

    override fun openTemplate(templateId: String): Template? {
        val entry = entriesById[templateId] ?: return null
        return BuiltinTemplate(classLoader, entry)
    }

    private fun loadDescriptor(descriptorPath: String): TemplateDescriptor {
        val stream = classLoader.getResourceAsStream(descriptorPath)
            ?: error("Missing builtin template descriptor resource: $descriptorPath")
        return descriptorReader.read(stream)
    }
}

@Serializable
private data class BuiltinTemplateIndex(
    val version: Int = 1,
    val templates: List<BuiltinTemplateIndexEntry> = emptyList(),
)

@Serializable
private data class BuiltinTemplateIndexEntry(
    val id: String,
    val descriptorPath: String,
)

private data class BuiltinTemplateEntry(
    val id: String,
    val descriptor: TemplateDescriptor,
    val baseResourcePath: String,
)

private class BuiltinTemplate(
    private val classLoader: ClassLoader,
    override val descriptor: TemplateDescriptor,
    private val baseResourcePath: String,
) : Template {
    constructor(classLoader: ClassLoader, entry: BuiltinTemplateEntry) : this(
        classLoader = classLoader,
        descriptor = entry.descriptor,
        baseResourcePath = entry.baseResourcePath,
    )

    override fun readBytes(relativePath: String): ByteArray {
        val normalized = relativePath.trimStart('/').replace('\\', '/')
        val resourcePath = "$baseResourcePath/$normalized"
        val stream = classLoader.getResourceAsStream(resourcePath)
            ?: error("Missing builtin template resource: $resourcePath")
        return stream.use { it.readBytes() }
    }
}
