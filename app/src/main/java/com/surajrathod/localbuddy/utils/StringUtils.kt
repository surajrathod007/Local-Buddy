package com.surajrathod.localbuddy.utils

fun extractSubstring(inputString: String, prefix: String): String {
    val startIndex = inputString.indexOf(prefix)
    if (startIndex != -1) {
        return inputString.substring(startIndex + prefix.length)
    }
    return "" // Return empty string if the prefix is not found
}

fun main(){
    val s = "/storage/emulated/0/ApkEditor/CamScanner"
}