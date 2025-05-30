package com.example.droidpromptplugin


import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class OpenFilesDialog(
    private val project: Project,
    private val onFilesSelected: (List<VirtualFile>) -> Unit
) : JDialog() {

    private val searchField = JTextField(30)
    private val listModel = DefaultListModel<VirtualFile>()
    private val fileList = JBList(listModel)
    private val openFiles = mutableListOf<VirtualFile>()
    private val allProjectFiles = mutableListOf<VirtualFile>()

    init {
        title = "Upload Files"
        layout = BorderLayout()
        preferredSize = Dimension(600, 400)
        isModal = true

        scanProjectFiles()
        listModel.addAll(openFiles) // Show only open files by default

        fileList.cellRenderer = VirtualFileListCellRenderer()
        fileList.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION

        val scrollPane = JScrollPane(fileList)
        scrollPane.border = JBUI.Borders.empty(5)

        val topPanel = JPanel(BorderLayout())
        topPanel.add(JLabel("Search: "), BorderLayout.WEST)
        topPanel.add(searchField, BorderLayout.CENTER)

        val buttonPanel = JPanel().apply {
            val okButton = JButton("OK").apply {
                addActionListener {
                    val selected = fileList.selectedValuesList
                    onFilesSelected(selected)
                    dispose()
                }
            }
            val cancelButton = JButton("Cancel").apply {
                addActionListener { dispose() }
            }
            add(okButton)
            add(cancelButton)
        }

        add(topPanel, BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)
        add(buttonPanel, BorderLayout.SOUTH)

        attachSearchListener()

        pack()
        setLocationRelativeTo(null)
    }

    private fun scanProjectFiles() {
        val baseDir = project.baseDir ?: return
        // Add the extension here if any file is missing here
        val allowedExtensions = setOf("kt", "kts", "java", "xml")

        // Get open files
        val editorManager = FileEditorManager.getInstance(project)
        openFiles.clear()
        openFiles.addAll(
            editorManager.openFiles.filter { it.extension?.lowercase() in allowedExtensions }
        )

        // Get all project files
        allProjectFiles.clear()
        VfsUtilCore.visitChildrenRecursively(baseDir, object : VirtualFileVisitor<Any>() {
            override fun visitFile(file: VirtualFile): Boolean {
                if (!file.isDirectory && file.extension?.lowercase() in allowedExtensions) {
                    allProjectFiles.add(file)
                }
                return true
            }
        })
    }

    private fun attachSearchListener() {
        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = filterFiles()
            override fun removeUpdate(e: DocumentEvent) = filterFiles()
            override fun changedUpdate(e: DocumentEvent) = filterFiles()

            private fun filterFiles() {
                val query = searchField.text.lowercase()
                listModel.clear()
                if (query.isBlank()) {
                    listModel.addAll(openFiles) // Show open files again if search is cleared
                } else {
                    val matching = allProjectFiles.filter { it.name.lowercase().contains(query) }
                    listModel.addAll(matching)
                }
            }
        })
    }
}
