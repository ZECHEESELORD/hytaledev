package sh.harold.hytaledev.server

import java.nio.file.Path

class ServerJarInspector {
    fun inspect(jarPath: Path): ServerJarReport {
        return ServerJarReport(jarPath = jarPath)
    }
}

data class ServerJarReport(
    val jarPath: Path,
)
