package com.surajrathod.localbuddy.utils

fun extractSubstring(inputString: String, prefix: String): String {
    val startIndex = inputString.indexOf(prefix)
    if (startIndex != -1) {
        val substring = inputString.substring(startIndex + prefix.length)
        return substring
    }
    return "" // Return empty string if the prefix is not found
}