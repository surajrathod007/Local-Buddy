package com.surajrathod.localbuddy.server

import android.content.Context
import android.net.Uri
import com.surajrathod.localbuddy.ui.HomeActivity
import com.surajrathod.localbuddy.R
import com.surajrathod.localbuddy.extensions.logE
import com.surajrathod.localbuddy.utils.AppConstants
import com.surajrathod.localbuddy.utils.MBToBytes
import com.surajrathod.localbuddy.utils.URIPathHelper
import com.surajrathod.localbuddy.utils.addFilesItemsToHtmlString
import com.surajrathod.localbuddy.utils.getListOfFiles
import com.surajrathod.localbuddy.utils.htmlToString
import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import java.nio.charset.Charset

class BuddyServer(
    port: Int,
    hostName: String = "0.0.0.0",
    private val context: Context,
    private val folderUri: Uri
) : NanoHTTPD(hostName, port) {

    companion object {
        const val TAG = "BuddyServer"
        const val PRIMARY_PATH = "content://com.android.externalstorage.documents/tree/primary"
        var MAX_DOWNLOAD_SIZE_IN_BYTES = MBToBytes(500.0)
    }


    private fun handleHelloRequest(session: IHTTPSession): Response {
        val response = "Hello, World!"
        return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, response)
    }

    private fun handleApiRequest(session: IHTTPSession): Response {
        return try {
            val mimeType = "text/html"
            val inputStream: InputStream = context.resources.openRawResource(R.raw.index)
            val htmlString = inputStream.htmlToString()
            val dummyItems = getListOfFiles(context.contentResolver, folderUri, context)
            val newHtmlString = htmlString.addFilesItemsToHtmlString(dummyItems)
            val modifiedInputStream: InputStream = ByteArrayInputStream(
                newHtmlString.toByteArray(Charset.defaultCharset())
            )
            newChunkedResponse(Response.Status.OK, mimeType, modifiedInputStream)
        } catch (e: IOException) {
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                MIME_PLAINTEXT,
                "Internal Server Error"
            )
        }
    }

    private fun handleDownloadRequest(session: IHTTPSession): Response {
        // Example: Extracting specific query parameter
        val parameterValue = session.parms["uri"]
        logE(TAG, "Parametr : $parameterValue\n\nEncoded : ${URLEncoder.encode(parameterValue)}")
        var myFile: File? = null
        try {
            myFile = File(AppConstants.INTERNAL_STORAGE_PATH + parameterValue)
            logE(TAG, "Size of selected file ${myFile.length()}")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (myFile?.exists() == true && myFile?.isFile == true) {
            try {
                val inputStream = FileInputStream(myFile)
                val mimeType = "application/octet-stream" // Set appropriate MIME type
                val response = newFixedLengthResponse(
                    Response.Status.OK,
                    mimeType,
                    inputStream,
                    myFile.length()
                )
                response.addHeader("Content-Disposition", "attachment; filename=\"${myFile.name}\"")
                return response
            } catch (e: Exception) {
                e.printStackTrace()
                return newFixedLengthResponse(
                    Response.Status.OK,
                    MIME_PLAINTEXT,
                    "Internal server error ${e.message}"
                )
            }
        } else {
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                MIME_PLAINTEXT,
                "File not found"
            )
        }
    }

    override fun serve(session: IHTTPSession): Response {
        val method = session.method
        val uri = session.uri
        logE(HomeActivity.TAG, "$method request received for URI: $uri")
        return when {
            uri.startsWith("/hello") -> handleHelloRequest(session)
            uri.startsWith("/html") -> handleApiRequest(session)
            uri.startsWith("/download") -> handleDownloadRequest(session)
            else -> newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                MIME_PLAINTEXT,
                "Not Found"
            )
        }
    }


}
