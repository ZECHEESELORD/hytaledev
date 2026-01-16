package sh.harold.hytaledev.templating

interface Template : AutoCloseable {
    val descriptor: TemplateDescriptor

    fun readBytes(relativePath: String): ByteArray

    override fun close() {}
}
