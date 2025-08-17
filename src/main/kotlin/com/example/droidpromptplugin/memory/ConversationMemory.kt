package com.example.droidpromptplugin.memory

import java.util.LinkedList
import java.util.Queue

class ConversationMemory(private val capacity: Int = 7) {

    private val memoryQueue: Queue<Interaction> = LinkedList()

    fun addInteraction(question: String, answer: String) {
        if (memoryQueue.size >= capacity) {
            memoryQueue.poll() // Remove oldest
        }
        memoryQueue.offer(Interaction(question, answer))
    }

    fun getMemory(): List<Interaction> {
        return memoryQueue.toList() // Return copy to prevent external modification
    }

    fun clearMemory() {
        memoryQueue.clear()
    }

    fun isEmpty(): Boolean {
        return memoryQueue.isEmpty()
    }

    fun size(): Int {
        return memoryQueue.size
    }
}

data class Interaction(
    val question: String,
    val answer: String
)