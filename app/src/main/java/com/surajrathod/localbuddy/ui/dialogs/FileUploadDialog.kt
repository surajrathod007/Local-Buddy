package com.surajrathod.localbuddy.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.surajrathod.localbuddy.databinding.DialogFileUploadingBinding

class FileUploadDialog : DialogFragment() {

    private var binding : DialogFileUploadingBinding? =null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogFileUploadingBinding.inflate(inflater,container,false)
        return binding?.root
    }

    fun setProgress(progressText : String){
        binding?.txtLblProgress?.text = progressText
    }
}