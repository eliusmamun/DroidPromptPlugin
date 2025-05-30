package com.example.droidpromptplugin

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import java.io.File
import javax.swing.JCheckBox
import javax.swing.JFileChooser
import javax.swing.JPanel

object PromptContextProvider {

   fun getSelectedText(project: Project): String? {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        return editor?.selectionModel?.selectedText
    }


    private val uploadedFileMap = linkedMapOf<File, Pair<String, JCheckBox>>()  // File -> (Content, Checkbox)

    fun getUploadedFiles(): Map<File, Pair<String, JCheckBox>> = uploadedFileMap.toMap()

    fun getSelectedFiles(): Map<File, Pair<String, JCheckBox>> = uploadedFileMap.filter { it.value.second.isSelected }.toMap()


    fun addFiles(files: List<File>) {
        files.forEach { file ->
            val content = try {
                file.readText()
            } catch (e: Exception) {
                "❗ Error reading ${file.name}: ${e.message}"
            }
            val checkBox = JCheckBox(file.name, true)
            uploadedFileMap[file] = Pair(content, checkBox)
        }
    }

    fun removeFile(file: File) {
        uploadedFileMap.remove(file)
    }

    fun getSelectedFilesContent(): String {
        return uploadedFileMap.filter { it.value.second.isSelected }
            .entries.joinToString("\n\n") { (file, pair) ->
                "\uD83D\uDCC4 ${file.name}:\n${pair.first}"
            }
    }

}