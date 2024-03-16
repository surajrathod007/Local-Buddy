package com.surajrathod.localbuddy

import com.surajrathod.localbuddy.server.BuddyServer
import java.net.URLEncoder

fun main() {
    val inputString = "content://com.android.externalstorage.documents/tree/primary:ApkEditor/RTO/document/primary:ApkEditor/RTO/RTOTamil.db"
    val substring = extractSubstring(inputString)
    val finalString = BuddyServer.PRIMARY_PATH+URLEncoder.encode(substring)
    println(finalString)
}

fun extractSubstring(inputString: String): String {
    val prefix = "content://com.android.externalstorage.documents/tree/primary"
    val startIndex = inputString.indexOf(prefix)
    if (startIndex != -1) {
        val substring = inputString.substring(startIndex + prefix.length)
        return substring
    }
    return "" // Return empty string if the prefix is not found
}