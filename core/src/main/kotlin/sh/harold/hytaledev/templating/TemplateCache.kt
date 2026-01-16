package sh.harold.hytaledev.templating

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.moveTo
import kotlin.io.path.readText
import kotlin.io.path.writeText

class TemplateCache(
    val cacheDir: Path,
) {
    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun clear() {
        if (!cacheDir.exists()) return
        cacheDir.listDirectoryEntries().forEach { child ->
            deleteRecursively(child)
        }
    }

    fun getOrDownloadRemoteZip(url: String): Path {
        val remoteDir = cacheDir.resolve("remoteZip").createDirectories()

        val urlKey = sha256Hex(url.toByteArray())
        val entryPath = remoteDir.resolve("$urlKey.json")

        if (entryPath.isRegularFile()) {
            val entry = runCatching { json.decodeFromString(RemoteZipCacheEntry.serializer(), entryPath.readText()) }
                .getOrNull()
            if (entry != null) {
                val cached = remoteDir.resolve(entry.zipFileName)
                if (cached.isRegularFile()) return cached
            }
        }

        val tempDir = remoteDir.resolve("tmp").createDirectories()
        val tempFile = Files.createTempFile(tempDir, "download-", ".zip")

        val sha256 = downloadToFile(url, tempFile)
        val zipFileName = "content-$sha256.zip"
        val target = remoteDir.resolve(zipFileName)

        if (!target.exists()) {
            tempFile.moveTo(target, overwrite = false)
        } else {
            tempFile.deleteIfExists()
        }

        val entry = RemoteZipCacheEntry(url = url, sha256 = sha256, zipFileName = zipFileName)
        entryPath.writeText(json.encodeToString(entry))

        return target
    }

    private fun downloadToFile(url: String, destination: Path): String {
        destination.parent?.createDirectories()

        val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() !in 200..299) {
            response.body().close()
            error("Failed to download template archive ($url): HTTP ${response.statusCode()}")
        }

        response.body().use { input ->
            Files.newOutputStream(destination).use { output ->
                val digest = MessageDigest.getInstance("SHA-256")
                copyAndDigest(input, output, digest)
                return digest.digest().toHexString()
            }
        }
    }

    private fun deleteRecursively(path: Path) {
        if (!path.exists()) return
        if (path.isRegularFile()) {
            path.deleteIfExists()
            return
        }
        if (!path.isDirectory()) {
            path.deleteIfExists()
            return
        }

        Files.walk(path).sorted(Comparator.reverseOrder()).forEach { it.deleteIfExists() }
    }
}

@Serializable
private data class RemoteZipCacheEntry(
    val url: String,
    val sha256: String,
    val zipFileName: String,
)

private fun sha256Hex(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update(bytes)
    return digest.digest().toHexString()
}

private fun copyAndDigest(input: InputStream, output: java.io.OutputStream, digest: MessageDigest) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    while (true) {
        val read = input.read(buffer)
        if (read <= 0) break
        output.write(buffer, 0, read)
        digest.update(buffer, 0, read)
    }
}

private fun ByteArray.toHexString(): String = joinToString(separator = "") { "%02x".format(it) }
