package ua.retrogaming.gcac.util

import android.content.Context
import android.widget.Toast
import android.widget.Toast.makeText

class ViewHelper(private val context: Context) {

    fun showToast(text: String) = makeText(context, text, Toast.LENGTH_SHORT).show()
}