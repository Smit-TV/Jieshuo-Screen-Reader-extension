package com.android.talkback
import android.accessibilityservice.AccessibilityService.SoftKeyboardController
import android.content.Context
import android.os.Build
import android.view.inputmethod.InputMethodManager
import android.util.Log

class KeyboardController(private val context: Context,
    private val softKbdController: SoftKeyboardController) {
        private lateinit var imm: InputMethodManager
    init {
        imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledIMList = imm.getInputMethodList()
        for (ime in enabledIMList) {
            Log.i(TAG,
            ime.id ?: "no id"  + " label " + "$ime")
        }
        if (Build.VERSION.SDK_INT > 32) {
        //softKbdController.switchToInputMethod("com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME")
        }
    } 

    companion object {
        const val TAG = "KeyboardController"
    }
}