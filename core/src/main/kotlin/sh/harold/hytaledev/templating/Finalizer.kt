package sh.harold.hytaledev.templating

sealed interface Finalizer {
    data object ImportGradleProject : Finalizer
    data object GitAddAll : Finalizer
    data object CreateRunConfiguration : Finalizer
    data object CopyPluginToServer : Finalizer
}
