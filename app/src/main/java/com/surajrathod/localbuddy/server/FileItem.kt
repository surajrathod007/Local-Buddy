package com.surajrathod.localbuddy.server

import android.net.Uri

data class FileItem(
    val imageUrl: String = "https://via.placeholder.com/100/3f91ff/ffffff",
    val name: String,
    val fileUri: Uri? = null,
    val filePath: String,
) {
    fun generateHtmlString(): String {
        return """
        <div class="item">
            <img src="${this.imageUrl}" alt="${this.name}">
            <div class="name">${this.name}</div>
            <a href="${BuddyServer.HOME_PREFIX}${getPath()}" class="download-btn">Download</a>
        </div>
    """.trimIndent()
    }

    // file path -> ApkEditor/CamScanner/classes2.dex where ApkEditor is parent folder selected by user
    private fun getPath(): String {
        return filePath
    }
}

