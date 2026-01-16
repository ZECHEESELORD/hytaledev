package sh.harold.hytaledev.templating

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.readText

class TemplateDescriptorReader(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) {
    fun read(path: Path): TemplateDescriptor {
        val content = path.readText()
        return json.decodeFromString(content)
    }

    fun read(input: InputStream): TemplateDescriptor {
        val content = input.readBytes().toString(StandardCharsets.UTF_8)
        return json.decodeFromString(content)
    }
}
