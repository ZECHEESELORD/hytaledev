package sh.harold.hytaledev.server

import java.nio.file.Path

data class ServerLayout(
    val serverDir: Path,
    val serverJar: Path,
    val assetsZip: Path?,
    val modsDir: Path,
)
