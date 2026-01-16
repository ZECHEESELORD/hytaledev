package sh.harold.hytaledev.templating

import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import java.io.StringWriter

class VelocityRenderer(
    private val engine: VelocityEngine = VelocityEngine().also { it.init() },
) {
    fun render(template: String, context: Map<String, Any?>): String {
        val velocityContext = VelocityContext(context)
        val out = StringWriter()
        engine.evaluate(velocityContext, out, "hytaledev", template)
        return out.toString()
    }
}
