package sh.harold.hytaledev.templates

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import sh.harold.hytaledev.settings.HytaleDevSettingsState
import sh.harold.hytaledev.templating.Template
import sh.harold.hytaledev.templating.TemplateCache
import sh.harold.hytaledev.templating.TemplateDescriptor
import sh.harold.hytaledev.templating.TemplateProvider
import sh.harold.hytaledev.templating.TemplateRepository
import sh.harold.hytaledev.templating.expandVersionPlaceholder
import sh.harold.hytaledev.templating.repositories.ArchiveZipTemplateRepository
import sh.harold.hytaledev.templating.repositories.BuiltinTemplateRepository
import sh.harold.hytaledev.templating.repositories.LocalDirTemplateRepository
import sh.harold.hytaledev.templating.repositories.RemoteZipTemplateRepository
import java.nio.file.Path

@Service(Service.Level.APP)
class HytaleDevTemplateService {
    private val logger = Logger.getInstance(HytaleDevTemplateService::class.java)

    fun loadTemplateIndex(): TemplateIndex {
        val repositories = repositories()

        val problems = mutableListOf<String>()
        val templates = mutableListOf<TemplateDescriptor>()
        val seenIds = mutableSetOf<String>()

        repositories.forEach { repository ->
            val loaded = runCatching { repository.listTemplates() }
                .getOrElse { error ->
                    val provider = providerLabel(repository.provider)
                    val message = error.message ?: error::class.java.simpleName
                    logger.warn("Failed to list templates from $provider", error)
                    problems += "$provider: $message"
                    emptyList()
                }

            loaded.forEach { descriptor ->
                if (seenIds.add(descriptor.id)) {
                    templates += descriptor
                } else {
                    logger.warn("Duplicate template id '${descriptor.id}' detected; keeping the first occurrence")
                }
            }
        }

        return TemplateIndex(
            templates = templates.sortedWith(compareBy(TemplateDescriptor::group, TemplateDescriptor::label, TemplateDescriptor::id)),
            problems = problems,
        )
    }

    fun openTemplate(templateId: String): Template? {
        return repositories().firstNotNullOfOrNull { it.openTemplate(templateId) }
    }

    fun clearCache() {
        TemplateCache(cacheDir()).clear()
    }

    fun cacheDir(): Path = cacheDir()

    private fun repositories(): List<TemplateRepository> {
        val settings = service<HytaleDevSettingsState>().state
        val cache = TemplateCache(cacheDir())

        val repositories = mutableListOf<TemplateRepository>()
        repositories += BuiltinTemplateRepository(javaClass.classLoader)

        settings.templateRepositories.forEach { entry ->
            buildRepository(entry, cache)?.let { repositories += it }
        }

        return repositories
    }

    private fun buildRepository(
        entry: HytaleDevSettingsState.TemplateRepositoryState,
        cache: TemplateCache,
    ): TemplateRepository? {
        return when (entry.kind) {
            HytaleDevSettingsState.TemplateRepositoryKind.RemoteZip -> {
                val urlTemplate = entry.urlTemplate?.trim().orEmpty()
                if (urlTemplate.isBlank()) return null

                val url = runCatching { expandVersionPlaceholder(urlTemplate, entry.urlVersion) }
                    .getOrElse { error ->
                        logger.warn("Invalid remoteZip template repository url: $urlTemplate", error)
                        return null
                    }

                RemoteZipTemplateRepository(url = url, cache = cache)
            }
            HytaleDevSettingsState.TemplateRepositoryKind.LocalDir -> {
                val path = entry.path?.trim().orEmpty()
                if (path.isBlank()) return null
                LocalDirTemplateRepository(Path.of(path))
            }
            HytaleDevSettingsState.TemplateRepositoryKind.ArchiveZip -> {
                val path = entry.path?.trim().orEmpty()
                if (path.isBlank()) return null
                ArchiveZipTemplateRepository(Path.of(path))
            }
        }
    }

    private fun cacheDir(): Path {
        return Path.of(PathManager.getSystemPath()).resolve("hytaledev").resolve("template-cache")
    }

    private fun providerLabel(provider: TemplateProvider): String {
        return when (provider) {
            TemplateProvider.Builtin -> "Builtin"
            is TemplateProvider.RemoteZip -> "Remote ZIP (${provider.url})"
            is TemplateProvider.LocalDir -> "Local Directory (${provider.directory})"
            is TemplateProvider.ArchiveZip -> "Archive ZIP (${provider.zipFile})"
        }
    }
}

data class TemplateIndex(
    val templates: List<TemplateDescriptor>,
    val problems: List<String> = emptyList(),
)

