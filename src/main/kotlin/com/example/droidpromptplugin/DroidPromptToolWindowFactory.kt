package com.example.droidpromptplugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.event.KeyEvent
import kotlinx.coroutines.*
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import java.awt.*
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import javax.swing.text.StyleConstants
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.EditorFactory
import java.io.File
import java.util.concurrent.atomic.AtomicReference


class DroidPromptToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val mainPanel = JPanel(BorderLayout())

        // Output area for conversation history
        val conversationArea = JTextPane().apply {
            isEditable = false
            contentType = "text/plain"
            background = Color.DARK_GRAY
             foreground = Color.WHITE
        }


        val conversationScroll = JScrollPane(conversationArea).apply {
            preferredSize = Dimension(600, 200)  // 🔽 reduce height here (was 300)
        }


        val askButton = JButton("Ask DroidPrompt")
        val placeHolderText = "Ask anything?"

        // Input area with placeholder
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

            // Bind Enter key to "askAction" (when focused)
            getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                "askAction"
            )

            actionMap.put("askAction", object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent?) {
                    askButton.doClick()
                }
            })

            // Optional: Bind Shift+Enter to newline explicitly
            getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK),
                "insert-break"
            )
        }
        val inputScroll = JScrollPane(inputArea).apply {
            preferredSize = Dimension(600, 80)  // 🔽 reduce height
        }


// Panel to hold checkboxes for uploaded files
        val uploadedFilesPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }

        val uploadButton = JButton("Upload Files")
        val deleteFileButton = JButton("Delete Files")
        val uploadImageButton = JButton("📷 Upload Image")

        val addSearchApiCheckbox = JCheckBox("Add search API").apply {
            isSelected = false
            foreground = Color.WHITE
            background = UIManager.getColor("Panel.background") // optional for dark theme
        }

        val uploadedFilesHeader = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(uploadButton)
            add(deleteFileButton)
            add(addSearchApiCheckbox) // ✅ new checkbox added
            add(uploadImageButton) // ✅ Image Upload button added
        }

// Container panel for the whole file upload section
        val uploadedFilesContainer = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEtchedBorder()
            preferredSize = Dimension(600, 120)
            add(uploadedFilesHeader, BorderLayout.NORTH)
            add(JScrollPane(uploadedFilesPanel), BorderLayout.CENTER)
        }

        fun refreshUploadedFilesList() {
            uploadedFilesPanel.removeAll()
            PromptContextProvider.getUploadedFiles().forEach { (file, pair) ->
                uploadedFilesPanel.add(pair.second)
            }
            uploadedFilesPanel.revalidate()
            uploadedFilesPanel.repaint()
        }


        uploadButton.addActionListener {

            OpenFilesDialog(project) { selectedFiles ->
                val filesToAdd = selectedFiles.mapNotNull { vf ->
                    val ioFile = File(vf.path)
                    if (ioFile.exists()) ioFile else null
                }
                PromptContextProvider.addFiles(filesToAdd)
                refreshUploadedFilesList()
            }.isVisible = true


            /*   val fileChooser = JFileChooser().apply {
                   fileSelectionMode = JFileChooser.FILES_ONLY
                   isMultiSelectionEnabled = true
                   project.basePath?.let { basePath ->
                       currentDirectory = java.io.File(basePath)
                   }
               }

               val result = fileChooser.showOpenDialog(mainPanel)
               if (result == JFileChooser.APPROVE_OPTION) {
                   val selectedFiles = fileChooser.selectedFiles.toList()
                   PromptContextProvider.addFiles(selectedFiles)
                   refreshUploadedFilesList()
               }*/
        }

        deleteFileButton.addActionListener {
            val filesToRemove = PromptContextProvider.getUploadedFiles()
                .filter { it.value.second.isSelected }
                .map { it.key }

            if (filesToRemove.isNotEmpty()) {
                filesToRemove.forEach { PromptContextProvider.removeFile(it) }
                refreshUploadedFilesList()
            }
        }

        uploadImageButton.addActionListener {
            val fileChooser = JFileChooser().apply {
                fileSelectionMode = JFileChooser.FILES_ONLY
                isMultiSelectionEnabled = false
                fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
                    "Image Files", "jpg", "jpeg", "png"
                )
                project.basePath?.let { basePath ->
                    currentDirectory = File(basePath)
                }
            }

            val result = fileChooser.showOpenDialog(mainPanel)
            if (result == JFileChooser.APPROVE_OPTION) {
                val selectedImage = fileChooser.selectedFile
                val base64String = ImageBase64Util.encodeImageToBase64(selectedImage)
                if (base64String != null) {
                    println("✅ Base64 string of image:\n$base64String")
                    // 🔁 Store or use it in your prompt logic if needed
                } else {
                    JOptionPane.showMessageDialog(mainPanel, "Failed to encode image.", "Error", JOptionPane.ERROR_MESSAGE)
                }
            }
        }


        askButton.addActionListener {
            val inputText = inputArea.text.trim()
            if(inputText == placeHolderText) return@addActionListener
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

                val reply =
                    "Pretend response from Droid prompt: This is a sample response. Real response will appear once the API integration is done"
                doc.insertString(doc.length, "\n\n🤖", normalStyle)
                CoroutineScope(Dispatchers.IO).launch {
                    reply.forEach { eachChar ->
                        delay(30)
                        doc.insertString(doc.length, "$eachChar", normalStyle)

                    }
                }
                conversationArea.caretPosition = doc.length

                val selectedText = PromptContextProvider.getSelectedText(project)
                 println("Elius - Selected Text = ${selectedText}")

                val selectedFiles = PromptContextProvider.getSelectedFiles()

                val prompt = PromptBuilder.buildGeminiPrompt(selectedFiles, selectedText, userQuestion = inputText)

                if (addSearchApiCheckbox.isSelected) {
                    println("✅ Search API checkbox is selected. Perform search-related logic here.")
                    // 🔁 Later, replace this with real logic
                } else {
                    println("ℹ️ Search API checkbox not selected. Proceed with normal flow.")
                }
            }
        }

        addSelectionListener(project)

        refreshUploadedFilesList()

        val inputPanel = JPanel(BorderLayout()).apply {
            add(inputScroll, BorderLayout.CENTER)
            add(askButton, BorderLayout.EAST) // Only keep Ask button here
        }

        val bottomPanel = JPanel(BorderLayout()).apply {
            add(inputPanel, BorderLayout.NORTH)
            add(uploadedFilesContainer, BorderLayout.CENTER)
        }

        mainPanel.add(conversationScroll, BorderLayout.CENTER)
        mainPanel.add(bottomPanel, BorderLayout.SOUTH)

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(mainPanel, "", false)
        toolWindow.contentManager.removeAllContents(true) // 💡 ensures clean rebuild
        toolWindow.contentManager.addContent(content)

    }

    private fun addSelectionListener(project: Project) {
        val debounceJob = AtomicReference<Job?>()

        val editorFactory = EditorFactory.getInstance()
        val listener = object : SelectionListener {
            override fun selectionChanged(e: SelectionEvent) {
                debounceJob.get()?.cancel()
                debounceJob.set(
                    CoroutineScope(Dispatchers.Default).launch {
                        delay(300) // debounce delay
                        val selectedText = e.editor.selectionModel.selectedText
                        if (!selectedText.isNullOrBlank()) {
                            println("✅ Final selected text: $selectedText")
                            // Do something meaningful here
                        }else {
                            println("ℹ️ No text selected")
                        }
                    }
                )
            }
        }

        editorFactory.eventMulticaster.addSelectionListener(listener, project)
    }


}
