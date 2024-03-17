package com.surajrathod.localbuddy.ui

import android.R.attr.data
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import com.surajrathod.localbuddy.R
import com.surajrathod.localbuddy.databinding.ActivityMainBinding
import com.surajrathod.localbuddy.extensions.logE
import com.surajrathod.localbuddy.extensions.setStatusBarColor
import com.surajrathod.localbuddy.server.BuddyServer
import com.surajrathod.localbuddy.server.FileItem
import com.surajrathod.localbuddy.utils.URIPathHelper
import fi.iki.elonen.NanoHTTPD
import java.io.IOException
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Enumeration


class HomeActivity : AppCompatActivity() {

    companion object {
        const val TAG = "HomeActivity"
    }


    lateinit var binding: ActivityMainBinding
    private var folderUri: Uri? = null

    private var buddyServer: BuddyServer? = null

    private val directoryPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                uri?.let { _ ->
                    // Handle the selected directory URI
                    val selectedDirectory = DocumentFile.fromTreeUri(this, uri)
                    if (selectedDirectory != null && selectedDirectory.isDirectory) {
                        folderUri = selectedDirectory.uri

                        val takeFlags: Int = (result.data!!.flags
                                and (Intent.FLAG_GRANT_READ_URI_PERMISSION
                                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))

                        contentResolver.takePersistableUriPermission(uri, takeFlags)

                    } else {
                        // The selected item is not a directory or is null
                        // Handle the error
                    }
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
    }

    private fun init() {
        setStatusBarColor(R.color.primary_bg_color)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnStartServer.setOnClickListener {
            if (folderUri != null) {
                startServer()
            } else {
                Toast.makeText(this, "Please select a folder ", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnChooseDirectory.setOnClickListener {
            openDirectoryPicker()
        }
    }

    private fun openDirectoryPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        directoryPickerLauncher.launch(intent)
    }

    private fun getAllFilesFromDirectory(directoryUri: Uri): List<FileItem> {
        val selectedDirectory = DocumentFile.fromTreeUri(this, directoryUri)
        return if (selectedDirectory != null && selectedDirectory.isDirectory) {
            val files: MutableList<DocumentFile> = mutableListOf()
            val fileList = selectedDirectory.listFiles()
            fileList?.let { files.addAll(it) }
            val newList = files.map { file ->
                FileItem(name = file.name ?: "No name", fileUri = file.uri, filePath = "")
            }
            newList
        } else {
            emptyList()
        }
    }


    private fun startServer() {
        if (folderUri != null) {
            buddyServer?.stop()
            buddyServer = BuddyServer(8900, getLocalIpAddress(), this, folderUri!!)
            try {
                buddyServer?.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
                logE(
                    TAG,
                    "Server started at http://${buddyServer?.hostname}:${buddyServer?.listeningPort}"
                )
                logE(TAG, "Listening port : ${buddyServer?.listeningPort}")
                binding.txtLblServerUrl.text =
                    "http://${getLocalIpAddress()}:${buddyServer?.listeningPort}/home"
            } catch (e: IOException) {
                logE(TAG, "Error starting server: ${e.message}")
            }
        } else {
            Toast.makeText(this, "Please select a directory first", Toast.LENGTH_LONG).show()
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface: NetworkInterface = interfaces.nextElement()
                val addresses: Enumeration<InetAddress> = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address: InetAddress = addresses.nextElement()
                    if (!address.isLoopbackAddress && address.isSiteLocalAddress) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }
}


