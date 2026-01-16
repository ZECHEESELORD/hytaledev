package sh.harold.hytaledev.manifest

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.writeText

class ManifestWriter(
    private val json: Json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
        explicitNulls = false
    },
) {
    fun write(manifestPath: Path, manifest: HytaleManifest) {
        val content = json.encodeToString(manifest)
        manifestPath.writeText(content + "\n")
    }
}
