package com.surajrathod.localbuddy.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.surajrathod.localbuddy.extensions.logE
import com.surajrathod.localbuddy.server.BuddyServer
import com.surajrathod.localbuddy.server.FileItem
import java.io.File


// Convert bytes to kilobytes (KB)
fun bytesToKB(bytes: Long): Double {
    return bytes / 1024.0
}

// Convert kilobytes (KB) to bytes
fun KBToBytes(kb: Double): Long {
    return (kb * 1024).toLong()
}

// Convert bytes to megabytes (MB)
fun bytesToMB(bytes: Long): Double {
    return bytes / 1024.0 / 1024.0
}

// Convert megabytes (MB) to bytes
fun MBToBytes(mb: Double): Long {
    return (mb * 1024 * 1024).toLong()
}


@SuppressLint("Range")
fun getListOfFilesFromUri(
    contentResolver: ContentResolver,
    parentUri: Uri,
    context: Context,
    folderPath: String
): List<FileItem> {
    val fileList = mutableListOf<FileItem>()
    val parentDocumentId = DocumentsContract.getTreeDocumentId(parentUri)
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parentUri, parentDocumentId)
    contentResolver.query(childrenUri, null, null, null, null)?.use { cursor ->
        while (cursor.moveToNext()) {
            val displayName =
                cursor.getString(cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME))
            val childDocumentId =
                cursor.getString(cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID))
            val childUri =
                DocumentsContract.buildDocumentUriUsingTree(childrenUri, childDocumentId)
            val wholePath = URIPathHelper().getPath(context, childUri) ?: ""
            val filePath = extractSubstring(
                wholePath,
                AppConstants.INTERNAL_STORAGE_PATH + folderPath
            ) //folderpath is main folder name
            logE(BuddyServer.TAG, "File path : $filePath")
            fileList.add(FileItem(name = displayName, fileUri = childUri, filePath = filePath))
        }
    }
    return fileList
}

fun getListOfFilesFromPath(file: File, folderPath: String): List<FileItem> {
    val fileList = mutableListOf<FileItem>()
    val filesInFolder = file.listFiles()
    filesInFolder?.forEach { f ->
        val wholePath = f.path
        val filePath = extractSubstring(wholePath, AppConstants.INTERNAL_STORAGE_PATH + folderPath)
        logE(
            BuddyServer.TAG, "Filepath for nested folder : $filePath"
        )
        fileList.add(FileItem(name = f.name, filePath = filePath))
    }
    return fileList
}

fun encodeUriString(uriString: String): String {
    val uri = Uri.parse(uriString)
    val encodedUri = Uri.Builder()
        .scheme(uri.scheme)
        .authority(uri.authority)
        .encodedPath(uri.encodedPath)
        .build()
    return encodedUri.toString()
}
