package sh.harold.hytaledev.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(name = "HytaleDevSettings", storages = [Storage("hytaledev.xml")])
class HytaleDevSettingsState : PersistentStateComponent<HytaleDevSettingsState.SettingsState> {
    data class SettingsState(
        var version: Int = 1,
        var lastServerDir: String? = null,
        var lastAssetsPath: String? = null,
    )

    private var settingsState = SettingsState()

    override fun getState(): SettingsState = settingsState

    override fun loadState(state: SettingsState) {
        settingsState = state
    }
}
