package com.example.droidpromptplugin

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import javax.swing.JFileChooser
import javax.swing.JPanel
import javax.swing.text.StyledDocument

object PromptContextProvider {


    fun getSelectedText(project: Project): String? {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        return editor?.selectionModel?.selectedText
    }

    private fun getFullFileContent(project: Project): String? {
        val virtualFile = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
        return virtualFile?.contentsToByteArray()?.toString(Charsets.UTF_8)
    }

    fun getUploadedFilesContent(project: Project, mainPanel: JPanel): String? {
        val fileChooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.FILES_ONLY
            isMultiSelectionEnabled = true
            // ✅ Set the current directory to the opened project's base path
            project.basePath?.let { base ->
                currentDirectory = java.io.File(base)
            }
        }
        val returnValue = fileChooser.showOpenDialog(mainPanel)
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            val selectedFiles = fileChooser.selectedFiles
            val fileContents = selectedFiles.joinToString("\n\n") { file ->
                try {
                    val content = file.readText()
                    "\uD83D\uDCC4 ${file.name}:\n$content"
                } catch (e: Exception) {
                    "\uD83D\uDEA9 Error reading ${file.name}"
                }
            }

            return fileContents

        }
        return null
    }
}