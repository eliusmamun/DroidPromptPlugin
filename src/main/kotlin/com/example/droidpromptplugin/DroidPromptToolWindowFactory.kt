package com.example.droidpromptplugin

import com.example.droidpromptplugin.modelselector.ModelSelector
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import kotlinx.coroutines.*
import java.awt.*
import java.awt.event.*
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import javax.swing.*
import javax.swing.text.StyleConstants
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.EditorFactory

class DroidPromptToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val mainPanel = JPanel(BorderLayout())

        // 🔹 Conversation output area
        val conversationArea = JTextPane().apply {
            isEditable = false
            contentType = "text/plain"
            background = Color.DARK_GRAY
            foreground = Color.WHITE
        }

        val conversationScroll = JScrollPane(conversationArea).apply {
            preferredSize = Dimension(600, 200)
        }

        val askButton = JButton("Ask DroidPrompt")
        val placeHolderText = "Ask anything?"

        // 🔹 Input text area
        val inputArea = JTextArea(3, 60).apply {
            text = placeHolderText
            foreground = Color.GRAY
            lineWrap = true
            wrapStyleWord = true

            addFocusListener(object : FocusAdapter() {
                override fun focusGained(e: FocusEvent?) {
                    if (text == placeHolderText) {
                        text = ""
                        foreground = Color.WHITE
                    }
                }

                override fun focusLost(e: FocusEvent?) {
                    if (text.isBlank()) {
                        text = placeHolderText
                        foreground = Color.GRAY
                    }
                }
            })

            // ↵ Enter to submit
            getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "askAction")
            actionMap.put("askAction", object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent?) {
                    askButton.doClick()
                }
            })

            // ⇧+↵ for newline
            getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "insert-break")
        }

        val inputScroll = JScrollPane(inputArea).apply {
            preferredSize = Dimension(600, 80)
        }

        // 🔹 Upload panel
        val uploadedFilesPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }

        val uploadButton = JButton("Upload Files")
        val deleteFileButton = JButton("Delete Files")
        val uploadImageButton = JButton("📷 Upload Image")

        // 🔹 Checkboxes & dropdown panel
        val addSearchApiCheckbox = JCheckBox("Add search API").apply {
            isSelected = true
            foreground = Color.WHITE
            background = UIManager.getColor("Panel.background")
        }

        val checkboxAndDropdownPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        checkboxAndDropdownPanel.add(addSearchApiCheckbox)

        val modelList = listOf("Gemini-1.5", "GPT-4", "Claude 3", "Llama 3", "Mistral 7B")
        var selectedModel: String? = null

        val modelSelector = ModelSelector(modelList) { selected ->
            selectedModel = selected
            println("✅ Selected LLM model: $selected")
        }

        checkboxAndDropdownPanel.add(modelSelector.panel)

        // 🔹 File upload UI
        val uploadedFilesHeader = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(uploadButton)
            add(deleteFileButton)
            add(checkboxAndDropdownPanel)
            add(uploadImageButton)
        }

        val uploadedFilesContainer = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEtchedBorder()
            preferredSize = Dimension(600, 120)
            add(uploadedFilesHeader, BorderLayout.NORTH)
            add(JScrollPane(uploadedFilesPanel), BorderLayout.CENTER)
        }

        fun refreshUploadedFilesList() {
            uploadedFilesPanel.removeAll()
            PromptContextProvider.getUploadedFiles().forEach { (_, pair) ->
                uploadedFilesPanel.add(pair.second)
            }
            uploadedFilesPanel.revalidate()
            uploadedFilesPanel.repaint()
        }

        // 🔹 File upload logic
        uploadButton.addActionListener {
            OpenFilesDialog(project) { selectedFiles ->
                val filesToAdd = selectedFiles.mapNotNull { vf ->
                    val ioFile = File(vf.path)
                    if (ioFile.exists()) ioFile else null
                }
                PromptContextProvider.addFiles(filesToAdd)
                refreshUploadedFilesList()
            }.isVisible = true
        }

        // 🔹 Delete file logic
        deleteFileButton.addActionListener {
            val filesToRemove = PromptContextProvider.getUploadedFiles()
                .filter { it.value.second.isSelected }
                .map { it.key }

            if (filesToRemove.isNotEmpty()) {
                filesToRemove.forEach { PromptContextProvider.removeFile(it) }
                refreshUploadedFilesList()
            }
        }

        // 🔹 Image upload
        uploadImageButton.addActionListener {
            val fileChooser = JFileChooser().apply {
                fileSelectionMode = JFileChooser.FILES_ONLY
                isMultiSelectionEnabled = false
                fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
                    "Image Files", "jpg", "jpeg", "png"
                )
                project.basePath?.let { basePath -> currentDirectory = File(basePath) }
            }

            val result = fileChooser.showOpenDialog(mainPanel)
            if (result == JFileChooser.APPROVE_OPTION) {
                val selectedImage = fileChooser.selectedFile
                val base64String = ImageBase64Util.encodeImageToBase64(selectedImage)
                if (base64String != null) {
                    println("✅ Base64 string:\n$base64String")
                } else {
                    JOptionPane.showMessageDialog(mainPanel, "Failed to encode image.", "Error", JOptionPane.ERROR_MESSAGE)
                }
            }
        }

        // 🔹 Ask button logic
        askButton.addActionListener {
            val inputText = inputArea.text.trim()
            if (inputText == placeHolderText) return@addActionListener
            if (inputText.isNotEmpty()) {
                inputArea.text = ""

                val doc = conversationArea.styledDocument
                val boldStyle = conversationArea.addStyle("BoldStyle", null).apply {
                    StyleConstants.setBold(this, true)
                    StyleConstants.setForeground(this, Color.WHITE)
                }
                val normalStyle = conversationArea.addStyle("NormalStyle", null).apply {
                    StyleConstants.setForeground(this, Color.LIGHT_GRAY)
                }

                doc.insertString(doc.length, "\n\n🧑You: $inputText", boldStyle)

                val reply = "Pretend response from Droid prompt: This is a sample response."
                doc.insertString(doc.length, "\n\n🤖", normalStyle)

                CoroutineScope(Dispatchers.IO).launch {
                    reply.forEach { c ->
                        delay(30)
                        doc.insertString(doc.length, "$c", normalStyle)
                    }
                }

                conversationArea.caretPosition = doc.length

                val selectedText = PromptContextProvider.getSelectedText(project)
                val selectedFiles = PromptContextProvider.getSelectedFiles()
                val prompt = PromptBuilder.buildGeminiPrompt(selectedFiles, selectedText, userQuestion = inputText)

                if (addSearchApiCheckbox.isSelected) {
                    println("🔍 Search API checkbox is selected.")
                } else {
                    println("➡️ Normal flow without search API.")
                }

                println("🧠 Selected model to use: ${selectedModel ?: "None"}")
            }
        }

        // 🔹 Input + Upload bottom panel
        val inputPanel = JPanel(BorderLayout()).apply {
            add(inputScroll, BorderLayout.CENTER)
            add(askButton, BorderLayout.EAST)
        }

        val bottomPanel = JPanel(BorderLayout()).apply {
            add(inputPanel, BorderLayout.NORTH)
            add(uploadedFilesContainer, BorderLayout.CENTER)
        }

        mainPanel.add(conversationScroll, BorderLayout.CENTER)
        mainPanel.add(bottomPanel, BorderLayout.SOUTH)

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(mainPanel, "", false)
        toolWindow.contentManager.removeAllContents(true)
        toolWindow.contentManager.addContent(content)

        addSelectionListener(project)
        refreshUploadedFilesList()
    }

    private fun addSelectionListener(project: Project) {
        val debounceJob = AtomicReference<Job?>()
        val editorFactory = EditorFactory.getInstance()
        val listener = object : SelectionListener {
            override fun selectionChanged(e: SelectionEvent) {
                debounceJob.get()?.cancel()
                debounceJob.set(CoroutineScope(Dispatchers.Default).launch {
                    delay(300)
                    val selectedText = e.editor.selectionModel.selectedText
                    if (!selectedText.isNullOrBlank()) {
                        println("✅ Final selected text: $selectedText")
                    } else {
                        println("ℹ️ No text selected")
                    }
                })
            }
        }
        editorFactory.eventMulticaster.addSelectionListener(listener, project)
    }
}
