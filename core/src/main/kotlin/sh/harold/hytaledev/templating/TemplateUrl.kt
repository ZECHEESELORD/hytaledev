package sh.harold.hytaledev.templating

fun expandVersionPlaceholder(urlTemplate: String, version: String?): String {
    if (!urlTemplate.contains("\$version")) return urlTemplate

    val value = version?.takeIf { it.isNotBlank() }
        ?: error("URL contains \$version but no version was provided")

    return urlTemplate.replace("\$version", value)
}
