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

        // Input area with placeholder
        val inputArea = JTextArea(3, 60).apply {
            text = "Ask anything?"
            foreground = Color.GRAY
            lineWrap = true
            wrapStyleWord = true

            addFocusListener(object : FocusAdapter() {
                override fun focusGained(e: FocusEvent?) {
                    if (text == "Ask anything?") {
                        text = ""
                        foreground = Color.WHITE
                    }
                }

                override fun focusLost(e: FocusEvent?) {
                    if (text.isBlank()) {
                        text = "Ask anything?"
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

        askButton.addActionListener {
            val inputText = inputArea.text.trim()
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


            }
        }


        val inputPanel = JPanel(BorderLayout()).apply {
            add(inputScroll, BorderLayout.CENTER)
            add(askButton, BorderLayout.EAST)
        }

        mainPanel.add(conversationScroll, BorderLayout.CENTER)
        mainPanel.add(inputPanel, BorderLayout.SOUTH)

        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(mainPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }


}
