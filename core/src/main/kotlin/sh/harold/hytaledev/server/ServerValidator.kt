package sh.harold.hytaledev.server

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

class ServerValidator {
    fun validate(serverDir: Path, assetsPath: Path? = null): ValidationResult {
        val errors = mutableListOf<String>()

        if (!serverDir.exists() || !serverDir.isDirectory()) {
            errors += "Server directory does not exist or is not a directory: $serverDir"
        }

        val serverJar = serverDir.resolve("HytaleServer.jar")
        if (!serverJar.isRegularFile()) {
            errors += "Missing HytaleServer.jar under: $serverDir"
        }

        val resolvedAssets = when {
            assetsPath == null -> serverDir.resolve("Assets.zip").takeIf { it.isRegularFile() }
            assetsPath.exists() -> assetsPath
            else -> null
        }
        if (resolvedAssets == null) {
            errors += "Missing Assets.zip (select it explicitly or place it under the server directory)"
        }

        if (errors.isNotEmpty()) return ValidationResult.Error(serverDir = serverDir, errors = errors)

        return ValidationResult.Ok(
            layout = ServerLayout(
                serverDir = serverDir,
                serverJar = serverJar,
                assetsZip = resolvedAssets,
                modsDir = serverDir.resolve("mods"),
            ),
            warnings = buildList {
                if (resolvedAssets != null && resolvedAssets.isDirectory()) {
                    add("Assets path is a directory; expected Assets.zip")
                }
            },
        )
    }
}

sealed interface ValidationResult {
    val message: String

    data class Ok(
        val layout: ServerLayout,
        val warnings: List<String> = emptyList(),
    ) : ValidationResult {
        override val message: String =
            if (warnings.isEmpty()) "OK" else "OK (${warnings.joinToString(separator = "; ")})"
    }

    data class Error(
        val serverDir: Path,
        val errors: List<String>,
    ) : ValidationResult {
        override val message: String = errors.joinToString(separator = "\n")
    }
}
