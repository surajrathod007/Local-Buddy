package com.surajrathod.localbuddy.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.surajrathod.localbuddy.R
import com.surajrathod.localbuddy.databinding.ActivityMainBinding
import com.surajrathod.localbuddy.extensions.logE
import com.surajrathod.localbuddy.extensions.setStatusBarColor
import com.surajrathod.localbuddy.server.BuddyServer
import com.surajrathod.localbuddy.server.FileItem
import com.surajrathod.localbuddy.ui.dialogs.FileUploadDialog
import dagger.hilt.android.AndroidEntryPoint
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.lang.reflect.Method
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Enumeration


@AndroidEntryPoint
class HomeActivity : AppCompatActivity(), BuddyServer.BuddyServerListener {


    private val homeViewModel by viewModels<HomeViewModel>()

    companion object {
        const val TAG = "HomeActivity"
    }

    private var fileUploadDialog: FileUploadDialog? = null


    private lateinit var binding: ActivityMainBinding
    private var folderUri: Uri? = null

    private var buddyServer: BuddyServer? = null

    private var receivedFile: File? = null
    private var isServerRunning = false

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
        setupObservers()
    }

    private fun setupObservers() {
        homeViewModel.fileProgress.observe(this) { progressText ->
            logE("SURAJPROGRESS", "$progressText")
            if (progressText.isNotEmpty() && fileUploadDialog != null) {
                fileUploadDialog?.setProgress(progressText)
            }
        }
        homeViewModel.isProgressVisible.observe(this) {
            if (it) {
                fileUploadDialog?.dismiss()
                fileUploadDialog = FileUploadDialog() {
                    receivedFile?.let { it1 -> openDirectoryWithFileManager(it1) }
                }
                fileUploadDialog?.isCancelable = false
                fileUploadDialog?.show(supportFragmentManager, null)

            } else {
                /*if(fileUploadDialog!=null){
                    fileUploadDialog?.dismiss()
                    fileUploadDialog = null
                    Toast.makeText(this,"File recieved successfully !",Toast.LENGTH_SHORT).show()
                }*/
            }
        }
    }


    private fun updateButtonState() {
        if (isServerRunning) {
            binding.btnStartServer.text = "Stop server"
        } else {
            binding.btnStartServer.text = "Start server"
        }
    }

    private fun setupClickListeners() {
        binding.btnStartServer.setOnClickListener {
            if (!isServerRunning) {
                if (folderUri != null) {
                    isServerRunning = true;
                    startServer()
                } else {
                    Toast.makeText(this, "Please select a folder ", Toast.LENGTH_SHORT).show()
                }
            } else {
                isServerRunning = false
                buddyServer?.stop()
            }
            updateButtonState()
        }
        binding.btnChooseDirectory.setOnClickListener {
            openDirectoryPicker()
        }
        binding.btnGetAllFiles.setOnClickListener {
            getAllFiles(this)
        }
    }

    fun listFiles(directory: File): List<String> {
        val allFiles = mutableListOf<String>()
        directory.listFiles()?.forEach { file ->
            allFiles.add(file.absolutePath)
        }
        return allFiles
    }

    fun getAllFiles(context: Context) {
        if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                TODO("VERSION.SDK_INT < R")
            }
        ) {
            val root = File(Environment.getExternalStorageDirectory().absolutePath)
            val allFiles = listFiles(root)
            allFiles.forEach { Log.d("FileList", it) }
        } else {
            Log.e("Permission", "MANAGE_EXTERNAL_STORAGE permission is not granted")
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
            buddyServer?.registerListener(this)
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

    override fun onFileUploading() {
        receivedFile = null
        homeViewModel.setIsProgressVisible(true)
    }

    override fun onFileUploading(pBytesRead: Long, pContentLength: Long, pItems: Int) {
        val percentComplete = ((pBytesRead.toDouble() / pContentLength.toDouble()) * 100).toInt()
        homeViewModel.setFileProgress(percentComplete)
    }

    override fun onFileUploaded(file: File) {
        lifecycleScope.launch {
            delay(1000)
            fileUploadDialog?.notifyFileReceived()
            receivedFile = file
        }
    }

    override fun onError(exception: Exception) {
        fileUploadDialog?.dismiss()
    }

    private fun openDirectoryWithFileManager(file: File) {

        if (Build.VERSION.SDK_INT >= 24) {
            try {
                val m: Method = StrictMode::class.java.getMethod("disableDeathOnFileUriExposure")
                m.invoke(null)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
        // Define your internal storage path and folder path


        // Convert the directory to a Uri
        val uri = Uri.parse("file://$file")

        // Create an intent to view the directory
        val intent = Intent(Intent.ACTION_VIEW).apply {
            //setDataAndType(uri, "resource/folder")
            setDataAndType(uri, "*/*")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        // Check if there's an app that can handle this intent
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            // Handle the case where no file manager app is available
            println("No file manager app found to open the directory.")
        }

    }

}


