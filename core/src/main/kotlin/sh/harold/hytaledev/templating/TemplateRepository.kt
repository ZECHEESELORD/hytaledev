package sh.harold.hytaledev.templating

interface TemplateRepository {
    val provider: TemplateProvider

    fun listTemplates(): List<TemplateDescriptor>

    fun openTemplate(templateId: String): Template?
}
