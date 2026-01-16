package sh.harold.hytaledev.wizard

internal const val MIN_JDK_FEATURE_VERSION = 21

internal fun normalizeArtifactId(projectName: String): String {
    val normalized = projectName
        .trim()
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')

    return normalized.ifBlank { "hytale-plugin" }
}

internal fun normalizePackageSegment(projectName: String): String {
    val normalized = projectName
        .trim()
        .lowercase()
        .replace(Regex("[^a-z0-9_]"), "_")
        .trim('_')

    val result = normalized.ifBlank { "plugin" }
    return if (result.firstOrNull()?.isDigit() == true) "plugin_$result" else result
}

internal fun suggestMainClassFqn(groupId: String, projectName: String): String {
    val packageSegment = normalizePackageSegment(projectName)
    val base = groupId.trim().ifBlank { "com.example" }
    return "$base.$packageSegment.Main"
}

internal fun isValidJavaFqn(value: String): Boolean {
    val trimmed = value.trim()
    return trimmed.matches(Regex("^[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+$"))
}

internal fun isValidGroupId(value: String): Boolean {
    val trimmed = value.trim()
    return trimmed.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)*$"))
}
