package com.example.droidpromptplugin

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import javax.imageio.ImageIO

object ImageBase64Util {
    fun encodeImageToBase64(imageFile: File): String? {
        return try {
            val bufferedImage: BufferedImage = ImageIO.read(imageFile)
            val outputStream = ByteArrayOutputStream()
            val formatName = getFormatName(imageFile.name)
            ImageIO.write(bufferedImage, formatName, outputStream)
            val imageBytes = outputStream.toByteArray()
            Base64.getEncoder().encodeToString(imageBytes)
        } catch (e: Exception) {
            println("❌ Failed to encode image: ${e.message}")
            null
        }
    }

    private fun getFormatName(fileName: String): String {
        return when {
            fileName.endsWith(".png", true) -> "png"
            fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) -> "jpg"
            else -> "png" // Default fallback
        }
    }
}
