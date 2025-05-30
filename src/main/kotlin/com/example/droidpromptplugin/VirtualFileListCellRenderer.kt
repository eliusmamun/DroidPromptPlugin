package com.example.droidpromptplugin

import com.intellij.openapi.vfs.VirtualFile
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

class VirtualFileListCellRenderer : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
    ): java.awt.Component {
        val file = value as? VirtualFile
        return super.getListCellRendererComponent(
            list, file?.name ?: "", index, isSelected, cellHasFocus
        )
    }
}
