package com.example.droidpromptplugin

fun String.sanitize(): String {
    return this
        .replace("\u0000", "") // Remove null characters
        .replace("\\", "\\\\") // Escape backslashes
        .replace("\"", "\\\"") // Escape quotes
}