package sh.harold.hytaledev.templating.repositories

import sh.harold.hytaledev.templating.Template
import sh.harold.hytaledev.templating.TemplateCache
import sh.harold.hytaledev.templating.TemplateDescriptor
import sh.harold.hytaledev.templating.TemplateProvider
import sh.harold.hytaledev.templating.TemplateRepository

class RemoteZipTemplateRepository(
    private val url: String,
    private val cache: TemplateCache,
) : TemplateRepository {
    override val provider: TemplateProvider = TemplateProvider.RemoteZip(url)

    private val delegate: ZipTemplateRepository by lazy {
        val zip = cache.getOrDownloadRemoteZip(url)
        ZipTemplateRepository(zipPath = zip, providerOverride = provider)
    }

    override fun listTemplates(): List<TemplateDescriptor> = delegate.listTemplates()

    override fun openTemplate(templateId: String): Template? = delegate.openTemplate(templateId)
}
