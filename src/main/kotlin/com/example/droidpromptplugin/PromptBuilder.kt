package com.example.droidpromptplugin

import java.io.File
import javax.swing.JCheckBox

object PromptBuilder {

    fun buildGeminiPrompt(
        referenceFiles: Map<File, Pair<String, JCheckBox>>,
        selectedText: String?,
        userQuestion: String
    ): String {
        val prompt = StringBuilder()

        prompt.appendLine("You are a code assistant. Use the following context to answer the user's question.\n")

        if(referenceFiles.isNotEmpty()){
            // Reference Files Section
            prompt.appendLine("## Reference Files")
            prompt.appendLine("The following files have been provided as context. Use them to better understand the codebase.\n")

            referenceFiles.entries.forEach {
                prompt.appendLine("- File: ${it.key.name}")
                prompt.appendLine("```")
                prompt.appendLine(it.value.first)
                prompt.appendLine("```\n")
            }
        }

        // Selected Code Section
        if (!selectedText.isNullOrBlank()) {
            prompt.appendLine("## Selected Code (if any)")
            prompt.appendLine("The user has selected this portion of code from the editor:\n")
            prompt.appendLine("```")
            prompt.appendLine(selectedText)
            prompt.appendLine("```\n")
        }

        // User Question
        prompt.appendLine("## User Question")
        prompt.appendLine(userQuestion)
        prompt.appendLine("\nPlease answer based on the code context provided. " +
                "If relevant, suggest code improvements, fixes, or additions. " +
                "Respond with concise, well-structured code and explanations where needed.")

        return prompt.toString()
    }
}