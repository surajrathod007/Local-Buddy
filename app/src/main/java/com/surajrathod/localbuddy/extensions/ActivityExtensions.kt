package com.surajrathod.localbuddy.extensions

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

fun Activity.setStatusBarColor(@ColorRes colorRes: Int) {
    val color = ContextCompat.getColor(this, colorRes)
    val darkness =
        1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255

    if (Build.VERSION.SDK_INT < 30) {
        var systemUiVisibility = window.decorView.systemUiVisibility
        systemUiVisibility = if (darkness < 0.5) {
            systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
        window.decorView.systemUiVisibility = systemUiVisibility
    } else {

        if (darkness > 0.5) {
            val controller = window.insetsController
            controller?.setSystemBarsAppearance(
                0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {

            val controller = window.insetsController
            controller?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        }

    }

    window.statusBarColor = color
}