package sh.harold.hytaledev.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(name = "HytaleDevSettings", storages = [Storage("hytaledev.xml")])
class HytaleDevSettingsState : PersistentStateComponent<HytaleDevSettingsState.SettingsState> {
    data class SettingsState(
        var version: Int = 2,
        var templateRepositories: MutableList<TemplateRepositoryState> = mutableListOf(),
    )

    data class TemplateRepositoryState(
        var kind: TemplateRepositoryKind = TemplateRepositoryKind.RemoteZip,
        var urlTemplate: String? = null,
        var urlVersion: String? = null,
        var path: String? = null,
    )

    enum class TemplateRepositoryKind {
        RemoteZip,
        LocalDir,
        ArchiveZip,
    }

    private var settingsState = SettingsState()

    override fun getState(): SettingsState = settingsState

    override fun loadState(state: SettingsState) {
        if (state.version < 2) {
            state.version = 2
        }
        settingsState = state
    }
}
