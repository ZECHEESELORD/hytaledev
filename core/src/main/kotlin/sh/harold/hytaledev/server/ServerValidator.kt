package sh.harold.hytaledev.server

import java.nio.file.Path

class ServerValidator {
    fun validate(serverDir: Path): ValidationResult {
        return ValidationResult.Ok(serverDir)
    }
}

sealed interface ValidationResult {
    data class Ok(val serverDir: Path) : ValidationResult
    data class Error(val serverDir: Path, val message: String) : ValidationResult
}
