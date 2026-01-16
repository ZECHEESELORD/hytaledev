package sh.harold.hytaledev.templating

import kotlinx.serialization.Serializable

@Serializable
data class TemplateDescriptor(
    val version: Int,
    val id: String,
    val label: String,
    val group: String,
    val properties: List<TemplateProperty> = emptyList(),
    val files: List<TemplateFile> = emptyList(),
    val finalizers: List<String> = emptyList(),
)

@Serializable
data class TemplateFile(
    val templatePath: String,
    val destinationPath: String,
    val render: Boolean = true,
    val openInEditor: Boolean = false,
    val reformat: Boolean = false,
    val condition: String? = null,
)
