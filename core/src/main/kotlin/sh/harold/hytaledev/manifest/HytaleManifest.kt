package sh.harold.hytaledev.manifest

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HytaleManifest(
    @SerialName("Group")
    val group: String,
    @SerialName("Name")
    val name: String,
    @SerialName("Version")
    val version: String,
    @SerialName("Website")
    val website: String? = null,
    @SerialName("Description")
    val description: String,
    @SerialName("Authors")
    val authors: List<Author> = emptyList(),
    @SerialName("Main")
    val main: String,
    @SerialName("ServerVersion")
    val serverVersion: String = "*",
    @SerialName("Dependencies")
    val dependencies: Map<String, String> = emptyMap(),
    @SerialName("OptionalDependencies")
    val optionalDependencies: Map<String, String> = emptyMap(),
    @SerialName("LoadBefore")
    val loadBefore: Map<String, String> = emptyMap(),
    @SerialName("DisabledByDefault")
    val disabledByDefault: Boolean = false,
    @SerialName("IncludesAssetPack")
    val includesAssetPack: Boolean = false,
    @SerialName("SubPlugins")
    val subPlugins: List<SubPlugin> = emptyList(),
) {
    @Serializable
    data class Author(
        @SerialName("Name")
        val name: String,
        @SerialName("Email")
        val email: String? = null,
        @SerialName("Url")
        val url: String? = null,
    )

    @Serializable
    data class SubPlugin(
        @SerialName("Name")
        val name: String,
        @SerialName("Main")
        val main: String,
        @SerialName("Version")
        val version: String,
        @SerialName("Description")
        val description: String,
        @SerialName("Website")
        val website: String? = null,
        @SerialName("Authors")
        val authors: List<Author> = emptyList(),
        @SerialName("ServerVersion")
        val serverVersion: String = "*",
        @SerialName("Dependencies")
        val dependencies: Map<String, String> = emptyMap(),
        @SerialName("OptionalDependencies")
        val optionalDependencies: Map<String, String> = emptyMap(),
        @SerialName("LoadBefore")
        val loadBefore: Map<String, String> = emptyMap(),
        @SerialName("DisabledByDefault")
        val disabledByDefault: Boolean = false,
        @SerialName("IncludesAssetPack")
        val includesAssetPack: Boolean = false,
    )
}
