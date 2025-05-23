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
import javax.swing.text.StyledDocument
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.EditorFactory
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


        val uploadButton = JButton("Upload Files").apply {
            addActionListener {
                val fileContents = PromptContextProvider.getUploadedFilesContent(project, mainPanel)
                val doc = conversationArea.document as StyledDocument
                doc.insertString(doc.length, "\n\n📎 Uploaded Files:\n$fileContents", null)
                conversationArea.caretPosition = conversationArea.document.length
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

                val context = PromptContextProvider.getSelectedText(project)
                 println("Elius - context = ${context}")


            }
        }


        addSelectionListener(project)


        val inputPanel = JPanel(BorderLayout()).apply {
            val buttonPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                add(askButton)
                add(Box.createRigidArea(Dimension(10, 0)))
                add(uploadButton)
            }

            add(inputScroll, BorderLayout.CENTER)
            add(buttonPanel, BorderLayout.EAST)
        }

        mainPanel.add(conversationScroll, BorderLayout.CENTER)
        mainPanel.add(inputPanel, BorderLayout.SOUTH)

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(mainPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    fun addSelectionListener(project: Project) {
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
