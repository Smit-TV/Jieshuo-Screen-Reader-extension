package com.google.android.marvin.talkback
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityGestureEvent
import android.graphics.Region
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import java.util.Locale
class TalkbackService : AccessibilityService() {
    override fun onServiceConnected() {
    }

    override fun onGesture(gestureId: Int): Boolean = false
    override fun onGesture(gesture: AccessibilityGestureEvent): Boolean = false
    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val eventType = event.eventType
        if (eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            handleKeyboardInput()
    }
    }

    fun handleKeyboardInput() {
        windows.forEach {
            if (it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                disableTouch(it)
                return
            }
        }

        enableTouch()
    }

    fun enableTouch() {
        setGestureDetectionPassthroughRegion(0, Region())
    }

    fun disableTouch(keyboardWindow: AccessibilityWindowInfo) {
        val region = Region()
        keyboardWindow.getRegionInScreen(region)
        setGestureDetectionPassthroughRegion(0, region)
    }
}