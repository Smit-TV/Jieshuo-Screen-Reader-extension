package com.google.android.marvin.talkback
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityGestureEvent
import android.graphics.Region
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import java.util.Locale

class TalkbackService : AccessibilityService() {
    private var keyboardNode: AccessibilityNodeInfo? = null
    private var isKeyboardShowed = false

    override fun onServiceConnected() {
    }

    override fun onDestroy() {
        super.onDestroy()
        enableTouch()
    }
    override fun onGesture(gestureId: Int): Boolean = false
    override fun onGesture(gesture: AccessibilityGestureEvent): Boolean = false
    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> handleKeyboardInput()
            AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END -> {
                keyboardNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                keyboardNode = null
            }
            AccessibilityEvent.TYPE_VIEW_HOVER_ENTER -> {
                val node = event.source ?: return
                if (node.window?.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD && node.packageName ?: "" != "com.google.android.inputmethod.latin") {
                    keyboardNode = node
                } else {
                    keyboardNode = null
                }
            }
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
        isKeyboardShowed = false
        setGestureDetectionPassthroughRegion(0, Region())
    }

    fun disableTouch(keyboardWindow: AccessibilityWindowInfo) {
        isKeyboardShowed = true
        val region = Region()
        keyboardWindow.getRegionInScreen(region)
        setGestureDetectionPassthroughRegion(0, region)
    }
}