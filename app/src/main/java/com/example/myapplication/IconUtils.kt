package com.example.myapplication

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat

object IconUtils {
    fun getSearchIcon(context: Context): Drawable? {
        return ContextCompat.getDrawable(context, R.drawable.ic_search)
    }
    
    fun getFilterIcon(context: Context): Drawable? {
        return ContextCompat.getDrawable(context, R.drawable.ic_filter)
    }
    
    fun getSortIcon(context: Context): Drawable? {
        return ContextCompat.getDrawable(context, R.drawable.ic_sort)
    }
}