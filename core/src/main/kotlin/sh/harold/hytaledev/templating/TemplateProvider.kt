package sh.harold.hytaledev.templating

import java.nio.file.Path

sealed interface TemplateProvider {
    data object Builtin : TemplateProvider

    data class RemoteZip(
        val url: String,
    ) : TemplateProvider

    data class LocalDir(
        val directory: Path,
    ) : TemplateProvider

    data class ArchiveZip(
        val zipFile: Path,
    ) : TemplateProvider
}
