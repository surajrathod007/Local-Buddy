package com.surajrathod.localbuddy.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.surajrathod.localbuddy.databinding.DialogFileUploadingBinding

class FileUploadDialog(private val onViewFileClick: () -> Unit = {}) : DialogFragment() {

    private var binding: DialogFileUploadingBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogFileUploadingBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //setup click listeners
        binding?.imgClose?.setOnClickListener {
            dismiss()
        }
        binding?.btnViewFile?.setOnClickListener {
            onViewFileClick.invoke()
        }
    }


    fun setProgress(progressText: String) {
        binding?.txtLblProgress?.text = progressText
    }

    fun notifyFileReceived() {
        binding?.txtLblTitle?.text = "File received successfully!"
        binding?.btnViewFile?.visibility = View.VISIBLE
        binding?.imgClose?.visibility = View.VISIBLE


    }
}