package com.surajrathod.localbuddy.utils

import com.surajrathod.localbuddy.server.FileItem
import java.io.InputStream


fun InputStream.htmlToString(): String {
    // Read the InputStream and convert it to a String
    return this.bufferedReader().use { it.readText() }
}

fun String.addFilesItemsToHtmlString(items: List<FileItem>): String {
    var myItemString = ""
    items.map { fileItem ->
        myItemString += "\n${fileItem.generateModernHtml()}"
    }
    return this.replace(AppConstants.ITEM_REPLACE_KEY, myItemString, true)
}

fun generateDummyItems(): List<FileItem> {
    val dummyItems = mutableListOf<FileItem>()

    for (i in 1..10) {
        val imageUrl = "https://via.placeholder.com/100/3f91ff/ffffff"
        val name = "Item $i"
        val item = FileItem(imageUrl, name, filePath = "")
        dummyItems.add(item)
    }

    return dummyItems
}