package sh.harold.hytaledev.templating

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TemplateProperty(
    val name: String,
    val type: PropertyType,
    val label: String,
    val default: String? = null,
    val remember: Boolean = false,
    val editable: Boolean = true,
    val visible: Boolean = true,
    val validator: String? = null,
    val options: List<String> = emptyList(),
)

@Serializable
enum class PropertyType {
    @SerialName("string")
    String,

    @SerialName("boolean")
    Boolean,

    @SerialName("inline_string_list")
    InlineStringList,

    @SerialName("class_fqn")
    ClassFqn,

    @SerialName("build_system_properties")
    BuildSystemProperties,

    @SerialName("jdk")
    Jdk,
}
