package com.google.android.marvin.talkback
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityGestureEvent
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.CountDownTimer
import android.graphics.Region
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import java.util.Locale
import com.android.talkback.*


class TalkbackService : AccessibilityService() {
    private lateinit var prefs: SharedPreferences
    private var keyboardNode: AccessibilityNodeInfo? = null
    private var isKeyboardShowed = false
    private var isFingerOnScreen = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        enableTouch()
        isFingerOnScreen = false
    }

    override fun onGesture(gestureId: Int): Boolean = false
    override fun onGesture(gesture: AccessibilityGestureEvent): Boolean = false
    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> handleKeyboardInput()
            AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START -> isFingerOnScreen = true
            AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END -> {
                isFingerOnScreen = false
                if (!prefs.getBoolean("single_tap_to_activate", true)) {
                    return
                }
                keyboardNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                keyboardNode = null
            }
            AccessibilityEvent.TYPE_VIEW_HOVER_ENTER -> {
                val node = event.source ?: return
                if (node.window?.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD && node.packageName ?: "" != "com.google.android.inputmethod.latin") {
                    keyboardNode = if (node.isAccessibilityFocusable) node else node.logicParent
                } else {
                    keyboardNode = null
                }
            }
            AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED -> {
                val delay = (prefs.getString("long_press_delay", null) ?: "3").toLongOrNull()
                if (delay == null || delay == 0L) {
                    return
                }
                object : CountDownTimer(delay * 1000, delay) {
                    override fun onTick(tick: Long) {
                        if (event.source != keyboardNode || !isFingerOnScreen) {
                            cancel()
                        }                        
                    }
                    override fun onFinish() {
                        if (event.source != keyboardNode) {
                            return
                        }
                        keyboardNode?.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
                        keyboardNode = null
                    }
                }.start()
            }
        }
    }

    fun handleKeyboardInput() {
        windows?.forEach {
            if (it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                disableTouch(it)
                return
            }
        }

        enableTouch()
    }

    fun enableTouch() {
        isKeyboardShowed = false
        setGestureDetectionPassthroughRegionInternal(Region())
    }

    fun disableTouch(keyboardWindow: AccessibilityWindowInfo) {
        if (!prefs.getBoolean("disable_gestures_when_keyboard_is_shown", true)) {
            return
        }
        if (Build.VERSION.SDK_INT > 29) {
        isKeyboardShowed = true
        val region = Region()
        keyboardWindow.getRegionInScreen(region)
        setGestureDetectionPassthroughRegionInternal(region)
        }
    }

    private fun setGestureDetectionPassthroughRegionInternal(region: Region) {
        if (Build.VERSION.SDK_INT > 29) {
            setGestureDetectionPassthroughRegion(0, region)
        }
    }
}