package sh.harold.hytaledev.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import sh.harold.hytaledev.settings.HytaleDevSettingsState.TemplateRepositoryKind
import sh.harold.hytaledev.settings.HytaleDevSettingsState.TemplateRepositoryState
import sh.harold.hytaledev.templates.HytaleDevTemplateService
import java.nio.file.Path
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ListSelectionModel

class TemplateRepositoriesPanel {
    private val listModel = DefaultListModel<TemplateRepositoryState>()
    private val list = JBList(listModel).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        cellRenderer = TemplateRepositoryRenderer()
        minimumSize = JBUI.size(460, 160)
    }

    private val panel = JPanel(VerticalLayout(JBUI.scale(8))).apply {
        add(createListPanel())
        add(createCachePanel())
        border = JBUI.Borders.empty(8)
    }

    val component: JComponent get() = panel

    fun reset(settings: HytaleDevSettingsState.SettingsState) {
        listModel.clear()
        settings.templateRepositories.forEach { listModel.addElement(it.copy()) }
    }

    fun isModified(settings: HytaleDevSettingsState.SettingsState): Boolean {
        return settings.templateRepositories != currentRepositories()
    }

    fun applyTo(settings: HytaleDevSettingsState.SettingsState) {
        settings.templateRepositories = currentRepositories().toMutableList()
        settings.version = maxOf(settings.version, 2)
    }

    private fun currentRepositories(): List<TemplateRepositoryState> {
        return (0 until listModel.size()).map { listModel.getElementAt(it) }
    }

    private fun createListPanel(): JComponent {
        val decorator = ToolbarDecorator.createDecorator(list)
            .setAddAction { _ -> addRepository() }
            .setEditAction { _ -> editSelectedRepository() }
            .setRemoveAction { _ -> removeSelectedRepository() }
            .setMoveUpAction { _ -> moveSelected(-1) }
            .setMoveDownAction { _ -> moveSelected(+1) }

        return decorator.createPanel().apply {
            border = JBUI.Borders.empty()
        }
    }

    private fun createCachePanel(): JComponent {
        return panel {
            row {
                button("Clear template cache") {
                    clearCache()
                }.comment("Removes cached remote ZIP downloads.")
            }
        }
    }

    private fun addRepository() {
        val options = arrayOf("Remote ZIP", "Local Directory", "Archive ZIP")
        val selectedIndex = Messages.showDialog(
            null,
            "Select a repository type to add:",
            "Add Template Repository",
            options,
            0,
            null,
        )
        if (selectedIndex < 0) return
        val kind = options[selectedIndex]

        val repository = when (kind) {
            "Remote ZIP" -> promptRemoteZip(null)
            "Local Directory" -> promptPathRepository(TemplateRepositoryKind.LocalDir, null, FileChooserDescriptorFactory.createSingleFolderDescriptor())
            "Archive ZIP" -> promptPathRepository(TemplateRepositoryKind.ArchiveZip, null, FileChooserDescriptorFactory.createSingleFileDescriptor("zip"))
            else -> null
        } ?: return

        listModel.addElement(repository)
        list.selectedIndex = listModel.size() - 1
    }

    private fun editSelectedRepository() {
        val index = list.selectedIndex
        if (index < 0) return

        val current = listModel.getElementAt(index)
        val updated = when (current.kind) {
            TemplateRepositoryKind.RemoteZip -> promptRemoteZip(current)
            TemplateRepositoryKind.LocalDir -> promptPathRepository(
                kind = TemplateRepositoryKind.LocalDir,
                initial = current,
                descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor(),
            )
            TemplateRepositoryKind.ArchiveZip -> promptPathRepository(
                kind = TemplateRepositoryKind.ArchiveZip,
                initial = current,
                descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("zip"),
            )
        } ?: return

        listModel.setElementAt(updated, index)
        list.selectedIndex = index
    }

    private fun removeSelectedRepository() {
        val index = list.selectedIndex
        if (index < 0) return
        listModel.remove(index)
        list.selectedIndex = (index - 1).coerceAtLeast(0).takeIf { it < listModel.size() } ?: -1
    }

    private fun moveSelected(delta: Int) {
        val index = list.selectedIndex
        if (index < 0) return
        val target = index + delta
        if (target !in 0 until listModel.size()) return

        val item = listModel.remove(index)
        listModel.add(target, item)
        list.selectedIndex = target
    }

    private fun promptRemoteZip(initial: TemplateRepositoryState?): TemplateRepositoryState? {
        val initialUrl = initial?.urlTemplate.orEmpty()
        val url = Messages.showInputDialog(
            "Remote ZIP URL (supports \$version placeholder):",
            "Remote ZIP Repository",
            null,
            initialUrl,
            null,
        )?.trim() ?: return null

        if (url.isBlank()) {
            Messages.showErrorDialog("URL is required.", "Remote ZIP Repository")
            return null
        }

        val version = if (url.contains("\$version")) {
            val initialVersion = initial?.urlVersion.orEmpty()
            val result = Messages.showInputDialog(
                "Version value for \$version:",
                "Remote ZIP Repository",
                null,
                initialVersion,
                null,
            )?.trim() ?: return null

            if (result.isBlank()) {
                Messages.showErrorDialog("Version is required when URL contains \$version.", "Remote ZIP Repository")
                return null
            }
            result
        } else {
            initial?.urlVersion?.takeIf { it.isNotBlank() }
        }

        return TemplateRepositoryState(
            kind = TemplateRepositoryKind.RemoteZip,
            urlTemplate = url,
            urlVersion = version,
        )
    }

    private fun promptPathRepository(
        kind: TemplateRepositoryKind,
        initial: TemplateRepositoryState?,
        descriptor: FileChooserDescriptor,
    ): TemplateRepositoryState? {
        val initialFile = initial?.path
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { Path.of(it).toFile() }.getOrNull() }
        val initialVirtual = initialFile?.let { com.intellij.openapi.vfs.LocalFileSystem.getInstance().findFileByIoFile(it) }

        val selected = FileChooser.chooseFile(descriptor, null, initialVirtual) ?: return null
        val path = selected.toNioPath().toString()

        return TemplateRepositoryState(
            kind = kind,
            path = path,
        )
    }

    private fun clearCache() {
        object : Task.Backgroundable(null, "Clear template cache", true) {
            override fun run(indicator: ProgressIndicator) {
                service<HytaleDevTemplateService>().clearCache()
            }

            override fun onSuccess() {
                Messages.showInfoMessage("Template cache cleared.", "HytaleDev")
            }

            override fun onThrowable(error: Throwable) {
                Messages.showErrorDialog(error.message ?: "Failed to clear cache.", "HytaleDev")
            }
        }.queue()
    }
}

private class TemplateRepositoryRenderer : com.intellij.ui.SimpleListCellRenderer<TemplateRepositoryState>() {
    override fun customize(list: javax.swing.JList<out TemplateRepositoryState>, value: TemplateRepositoryState?, index: Int, selected: Boolean, hasFocus: Boolean) {
        val text = when (value?.kind) {
            TemplateRepositoryKind.RemoteZip -> "Remote ZIP: ${value.urlTemplate.orEmpty()}"
            TemplateRepositoryKind.LocalDir -> "Local Directory: ${value.path.orEmpty()}"
            TemplateRepositoryKind.ArchiveZip -> "Archive ZIP: ${value.path.orEmpty()}"
            null -> ""
        }
        setText(text)
    }
}
