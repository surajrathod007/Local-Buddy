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
import com.surajrathod.localbuddy.utils.extractSubstring
import com.surajrathod.localbuddy.utils.getListOfFilesFromPath
import com.surajrathod.localbuddy.utils.getListOfFilesFromUri
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
        const val HOME_PREFIX = "/home/"
        var MAX_DOWNLOAD_SIZE_IN_BYTES = MBToBytes(500.0)
    }

    private var folderPath = "" //used for parent folder while accessing files


    /*
    Here we are storing the name of folder selected by user , in below example CamScanner/ will be stored
     */
    init {
        //eg :-  /storage/emulated/0/ApkEditor/CamScanner
        val path = URIPathHelper().getPath(context, folderUri)
        folderPath = extractSubstring(path ?: "", AppConstants.INTERNAL_STORAGE_PATH) + "/"
        logE(TAG, "My folder path : $folderPath")
    }

    private fun handleHelloRequest(session: IHTTPSession): Response {
        val response = "Hello, World!"
        return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, response)
    }

    private fun handleApiRequest(session: IHTTPSession): Response {
        logE(TAG, "Getting request at ${session.uri}")
        val filePath = extractSubstring(session.uri, HOME_PREFIX)
        if (filePath.isBlank()) {
            //hitting home -> just serve normal home page
            return try {
                val mimeType = "text/html"
                val inputStream: InputStream = context.resources.openRawResource(R.raw.index)
                val htmlString = inputStream.htmlToString()
                val dummyItems =
                    getListOfFilesFromUri(context.contentResolver, folderUri, context, folderPath)
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
        } else {
            //accessing files or directories
            return handleContents(filePath)
        }
    }


    private fun handleContents(filePath: String): Response {
        val file = File(AppConstants.INTERNAL_STORAGE_PATH + folderPath + filePath)
        if (file.exists()) {
            if (file.isDirectory) {
                //navigate with directory , what a pain
                return try {
                    val mimeType = "text/html"
                    val inputStream: InputStream = context.resources.openRawResource(R.raw.index)
                    val htmlString = inputStream.htmlToString()
                    val dummyItems = getListOfFilesFromPath(file, folderPath)
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
            } else {
                try {
                    val inputStream = FileInputStream(file)
                    val mimeType = "application/octet-stream" // Set appropriate MIME type
                    val response = newFixedLengthResponse(
                        Response.Status.OK,
                        mimeType,
                        inputStream,
                        file.length()
                    )
                    response.addHeader(
                        "Content-Disposition",
                        "attachment; filename=\"${file.name}\""
                    )
                    return response
                } catch (e: Exception) {
                    e.printStackTrace()
                    return newFixedLengthResponse(
                        Response.Status.OK,
                        MIME_PLAINTEXT,
                        "Internal server error ${e.message}"
                    )
                }
            }
        } else {
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                MIME_PLAINTEXT,
                "File not found"
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
            uri.startsWith("/home") -> handleApiRequest(session)
            uri.startsWith("/download") -> handleDownloadRequest(session)
            else -> newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                MIME_PLAINTEXT,
                "Not Found"
            )
        }
    }


}
