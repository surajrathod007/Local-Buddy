package com.surajrathod.localbuddy.server

import android.content.Context
import android.net.Uri
import com.surajrathod.localbuddy.R
import com.surajrathod.localbuddy.extensions.logE
import com.surajrathod.localbuddy.ui.HomeActivity
import com.surajrathod.localbuddy.utils.AppConstants
import com.surajrathod.localbuddy.utils.MBToBytes
import com.surajrathod.localbuddy.utils.URIPathHelper
import com.surajrathod.localbuddy.utils.addFilesItemsToHtmlString
import com.surajrathod.localbuddy.utils.addUploadUrlToHtmlString
import com.surajrathod.localbuddy.utils.extractSubstring
import com.surajrathod.localbuddy.utils.getListOfFilesFromPath
import com.surajrathod.localbuddy.utils.getListOfFilesFromUri
import com.surajrathod.localbuddy.utils.htmlToString
import fi.iki.elonen.NanoFileUpload
import fi.iki.elonen.NanoHTTPD
import org.apache.commons.fileupload.FileItem
import org.apache.commons.fileupload.FileUploadException
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Enumeration


class BuddyServer(
    port: Int,
    private val hostName: String = "0.0.0.0",
    private val context: Context,
    private val folderUri: Uri
) : NanoHTTPD(hostName, port) {


    private var mListener: BuddyServerListener? = null

    interface BuddyServerListener {
        fun onFileUploading()

        fun onFileUploading(pBytesRead : Long, pContentLength : Long, pItems : Int)
    }

    fun registerListener(buddyServerListener: BuddyServerListener) {
        mListener = buddyServerListener
    }


    companion object {
        const val TAG = "BuddyServer"
        const val PRIMARY_PATH = "content://com.android.externalstorage.documents/tree/primary"
        const val HOME_PREFIX = "/home/"
        const val UPLOAD_PREFIX = "/upload/"
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


    override fun serve(session: IHTTPSession): Response {
        val method = session.method
        val uri = session.uri
        logE(HomeActivity.TAG, "$method request received for URI: $uri")
        return when {
            uri.startsWith("/hello") -> handleHelloRequest(session)
            uri.startsWith("/home") -> handleApiRequest(session)
            uri.startsWith("/download") -> handleDownloadRequest(session)
            uri.startsWith("/uptest") -> handleUploadPageRequest(session)
            uri.startsWith("/upload") -> handleUploadRequest(session)
            else -> newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                MIME_PLAINTEXT,
                "Not Found"
            )
        }
    }

    private fun handleUploadPageRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        return try {
            val mimeType = "text/html"
            val inputStream: InputStream = context.resources.openRawResource(R.raw.uploadfile)
            val htmlString = inputStream.htmlToString()
            val uploadUrl = "http://${hostName}:${listeningPort}/uploadtest"
            val newHtmlString = htmlString.addUploadUrlToHtmlString(uploadUrl)
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

    private fun handleHelloRequest(session: IHTTPSession): Response {
        val response = "Hello, World!"
        return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, response)
    }

    private fun handleApiRequest(session: IHTTPSession): Response {
        logE(TAG, "Getting request at ${session.uri}")
        val filePath = extractSubstring(session.uri, HOME_PREFIX)
        logE("SURAJFILEPATH", "My home file path :- $filePath")
        if (filePath.isBlank()) {
            //hitting home -> just serve normal home page
            return try {
                val mimeType = "text/html"
                val inputStream: InputStream = context.resources.openRawResource(R.raw.latesthome)
                var htmlString = inputStream.htmlToString()
                val uploadUrl = "http://${hostName}:${listeningPort}/upload"
                htmlString = htmlString.addUploadUrlToHtmlString(uploadUrl) //adding upload functionality
                val dummyItems =
                    getListOfFilesFromUri(context.contentResolver, folderUri, context, folderPath)
                htmlString = htmlString.addFilesItemsToHtmlString(dummyItems)   //adding dynamic files
                val modifiedInputStream: InputStream = ByteArrayInputStream(
                    htmlString.toByteArray(Charset.defaultCharset())
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
                    val inputStream: InputStream =
                        context.resources.openRawResource(R.raw.latesthome)
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


    private fun handleUploadRequest(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        mListener?.onFileUploading()
        val filePath = extractSubstring(session.uri, UPLOAD_PREFIX)
        val fileUpload = NanoFileUpload(DiskFileItemFactory())
        fileUpload.setProgressListener { pBytesRead, pContentLength, pItems ->
            mListener?.onFileUploading(pBytesRead, pContentLength, pItems)
        }
        return try {
            val files: MutableList<FileItem> =
                fileUpload.parseRequest(session)
            if(files.isNotEmpty()){
                val firstFile = files[0]
                if(filePath.isEmpty()){
                    //store file in parent folder
                    val file = File(AppConstants.INTERNAL_STORAGE_PATH + folderPath + firstFile.name)
                    firstFile.write(file)
                }else{
                    //store file in sub folder
                    val file = File(AppConstants.INTERNAL_STORAGE_PATH + folderPath + filePath + firstFile.name)
                    firstFile.write(file)
                }
            }
            newFixedLengthResponse(
                Response.Status.OK, MIME_PLAINTEXT,
                "Uploaded files " + " out of " + files.size
            )
        } catch (e: IOException) {
            e.printStackTrace()
            throw IllegalArgumentException("Could not handle files from API request", e)
        } catch (e: FileUploadException) {
            e.printStackTrace()
            throw IllegalArgumentException("Could not handle files from API request", e)
        }
    }


}
