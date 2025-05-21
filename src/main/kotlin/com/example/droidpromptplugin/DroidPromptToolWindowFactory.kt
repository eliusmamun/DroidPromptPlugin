package com.example.droidpromptplugin

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import kotlinx.coroutines.*
import okhttp3.*
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

class DroidPromptToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val mainPanel = JPanel(BorderLayout())

        // Output area for conversation history
        val conversationArea = JTextArea().apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
        }

        val conversationScroll = JScrollPane(conversationArea).apply {
            preferredSize = Dimension(600, 300)
        }

        // Input area
        val inputArea = JTextArea(5, 60).apply {
            lineWrap = true
            wrapStyleWord = true
        }

        val inputScroll = JScrollPane(inputArea)

        val askButton = JButton("Ask DroidPrompt").apply {
            addActionListener {
                val inputText = inputArea.text.trim()
                if (inputText.isNotEmpty()) {
                    inputArea.text = ""
                    conversationArea.append("\n\n🧑 You:\n$inputText")


                        val reply = "Pretend response from Elius prompt:: "
                            conversationArea.append("\n🤖 DroidPrompt:\n$reply")
                            conversationArea.caretPosition = conversationArea.document.length
                }
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
