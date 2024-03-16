package com.surajrathod.localbuddy.extensions

import android.util.Log
import org.greenrobot.eventbus.android.BuildConfig

fun logE(tag: String, message: String) {
    if (!BuildConfig.DEBUG) {
        Log.e(tag, message)
    }
}

fun logD(tag: String, message: String) {
    if (!BuildConfig.DEBUG) {
        Log.d(tag, message)
    }
}

fun logI(tag: String, message: String) {
    if (BuildConfig.DEBUG) {
        Log.i(tag, message)
    }
}