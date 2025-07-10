package com.example.myapplication

import android.content.Context
import android.widget.Toast as AndroidToast
import androidx.annotation.StringRes

object Toast {
    const val LENGTH_SHORT = AndroidToast.LENGTH_SHORT
    const val LENGTH_LONG = AndroidToast.LENGTH_LONG

    fun show(context: Context, message: String, duration: Int = AndroidToast.LENGTH_SHORT) {
        AndroidToast.makeText(context, message, duration).show()
    }

    fun showLong(context: Context, message: String) {
        AndroidToast.makeText(context, message, AndroidToast.LENGTH_LONG).show()
    }

    fun show(context: Context, @StringRes messageResId: Int, duration: Int = AndroidToast.LENGTH_SHORT) {
        AndroidToast.makeText(context, context.getString(messageResId), duration).show()
    }

    fun showLong(context: Context, @StringRes messageResId: Int) {
        AndroidToast.makeText(context, context.getString(messageResId), AndroidToast.LENGTH_LONG).show()
    }

    // Add makeText function to mimic Android's Toast.makeText static method
    fun makeText(context: Context, message: String, duration: Int = AndroidToast.LENGTH_SHORT): AndroidToast {
        return AndroidToast.makeText(context, message, duration)
    }
    
    fun makeText(context: Context, @StringRes messageResId: Int, duration: Int = AndroidToast.LENGTH_SHORT): AndroidToast {
        return AndroidToast.makeText(context, context.getString(messageResId), duration)
    }
}