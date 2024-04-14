package com.surajrathod.localbuddy.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val mApp: Application) : AndroidViewModel(mApp) {

    private var _isProgressVisible = MutableLiveData<Boolean>(false)
    val isProgressVisible : LiveData<Boolean> get() = _isProgressVisible

    private var _fileProgress = MutableLiveData<String>()
    val fileProgress : LiveData<String> get() = _fileProgress

    fun setIsProgressVisible(isVisible : Boolean){
        _isProgressVisible.postValue(isVisible)
    }

    fun setFileProgress(progress : Int){
        _fileProgress.postValue("$progress %")
    }
}