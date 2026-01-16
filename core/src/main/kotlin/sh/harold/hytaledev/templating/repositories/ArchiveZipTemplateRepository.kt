package sh.harold.hytaledev.templating.repositories

import sh.harold.hytaledev.templating.Template
import sh.harold.hytaledev.templating.TemplateDescriptor
import sh.harold.hytaledev.templating.TemplateProvider
import sh.harold.hytaledev.templating.TemplateRepository
import java.nio.file.Path

class ArchiveZipTemplateRepository(
    private val zipPath: Path,
) : TemplateRepository {
    override val provider: TemplateProvider = TemplateProvider.ArchiveZip(zipPath)

    private val delegate: ZipTemplateRepository by lazy {
        ZipTemplateRepository(zipPath = zipPath, providerOverride = provider)
    }

    override fun listTemplates(): List<TemplateDescriptor> = delegate.listTemplates()

    override fun openTemplate(templateId: String): Template? = delegate.openTemplate(templateId)
}
