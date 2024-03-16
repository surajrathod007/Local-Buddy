package com.surajrathod.localbuddy.server

import android.net.Uri

data class FileItem(
    val imageUrl: String = "https://via.placeholder.com/100/3f91ff/ffffff",
    val name: String,
    val fileUri: Uri? = null,
    val filePath: String
) {
    fun generateHtmlString(): String {
        return """
        <div class="item">
            <img src="${this.imageUrl}" alt="${this.name}">
            <div class="name">${this.name}</div>
            <a href="./download?uri=$filePath" class="download-btn">Download</a>
        </div>
    """.trimIndent()
    }
}

