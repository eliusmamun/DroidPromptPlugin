package com.example.droidpromptplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import com.google.gson.JsonObject
import com.google.gson.Gson



class MyPluginAction : AnAction() {

    private val client = OkHttpClient()
    private val apiKey = "YOUR_API_KEY"

    override fun actionPerformed(e: AnActionEvent) {
        val input = Messages.showInputDialog(
            "Enter your prompt for the AI:",
            "DroidPrompt",
            Messages.getQuestionIcon()
        )

        if (!input.isNullOrBlank()) {
            // Simulate calling ChatGPT API
            val response = "Pretend response for : \"$input\""
            Messages.showMessageDialog(response, "EliusPrompt Result", Messages.getInformationIcon())
        }

        if (!input.isNullOrBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                val responseText = callApi(input)
                withContext(Dispatchers.Main) {
                    showResponse(e, responseText)
                }
            }
        }

    }

    private suspend fun callApi(userInput: String): String {
        return try {
            val apiUrl = "https://api.example.com/chat" // Replace with your real API

            val requestBody = """
                {
                    "message": "$userInput"
                }
            """.trimIndent()

            val request = Request.Builder()
                .url(apiUrl)
                .post(RequestBody.create("application/json".toMediaTypeOrNull(), requestBody))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return "Error: ${response.code}"
                }

                val json = Gson().fromJson(response.body?.string(), JsonObject::class.java)
                json.get("reply")?.asString ?: "No response"
            }
        } catch (ex: Exception) {
            "Exception: ${ex.message}"
        }
    }

    private fun showResponse(e: AnActionEvent, message: String) {
        Messages.showMessageDialog(
            message,
            "DroidPrompt Response",
            Messages.getInformationIcon()
        )
    }
}
