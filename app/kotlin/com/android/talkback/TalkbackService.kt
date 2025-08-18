package com.google.android.marvin.talkback
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.AccessibilityGestureEvent
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.InputMethod
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.PowerManager
import android.provider.Settings
import android.net.Uri
import android.graphics.Rect
import android.graphics.Region
import android.graphics.Path
import android.graphics.PointF
import android.view.KeyEvent
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.accessibility.AccessibilityManager
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import com.android.talkback.*


class TalkbackService : AccessibilityService() {
    private lateinit var executorService: ExecutorService
    private lateinit var prefs: SharedPreferences
    private lateinit var accessibilityManager: AccessibilityManager
    private lateinit var kbdController: KeyboardController
    private lateinit var powerManager: PowerManager
    private var keyboardNode: AccessibilityNodeInfo? = null
    private var isKeyboardShowed = false
    private var isFingerOnScreen = false
    private var isRunning = false

    companion object {
        const val TAG = "JieshuoExtension"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        isRunning = true
        if (prefs.getBoolean("debug_mode", false)) {
            LogUtils.createDebugFile(this, "onServiceConnected")
        }
    }

    override fun onCreate() {
        super.onCreate()
        executorService = Executors.newFixedThreadPool(3)
        prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        kbdController = KeyboardController(this, getSoftKeyboardController())
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        if (prefs.getBoolean("debug_mode", false)) {
            LogUtils.createDebugFile(this, "onCreate")
        }
    }

    override fun onDestroy() {
                if (prefs.getBoolean("debug_mode", false)) {
            LogUtils.createDebugFile(this, "onDestroy")
        }
        super.onDestroy()
        isRunning = false
        enableTouch()
        isFingerOnScreen = false
    }

    override fun onGesture(gestureId: Int): Boolean = false
    override fun onGesture(gesture: AccessibilityGestureEvent): Boolean = false
    override fun onInterrupt() {}
    override fun onKeyEvent(event: KeyEvent): Boolean = false

    @SuppressLint("NewApi")
    override fun onCreateInputMethod(): InputMethod {
        return object : InputMethod(this) {
            override fun onStartInput(editorInfo: android.view.inputmethod.EditorInfo, restarted: Boolean) {}
        }
    }

fun emulateTap(node: AccessibilityNodeInfo, time: Long) {
val rect = Rect()
node.getBoundsInScreen(rect)
emulateTapXY(rect.exactCenterX().toFloat(), rect.exactCenterY().toFloat(), time)
} fun emulateTapXY(x: Float, y: Float, time: Long) {
val point = PointF(x, y)
    val tap = GestureDescription.StrokeDescription(path(point), 0, time)
    val builder = GestureDescription.Builder()
    builder.addStroke(tap)
    dispatchGesture(builder.build(), object : GestureResultCallback() {
override fun onCancelled(gesture: GestureDescription) {
Log.e(TAG, "Gesture cancelled.")
}
override fun onCompleted(gesture: GestureDescription) {
Log.i(TAG, "Gesture completed.")
}
}, null)
}
private fun path(point: PointF): Path {
    val path = Path()
    path.moveTo(point.x, point.y)
    return path
}

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!isRunning) {
            return
        }

        try {
            if (!powerManager.isInteractive) {
                return
            }
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> handleKeyboardInput()
            AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START -> isFingerOnScreen = true
            AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END -> {
                isFingerOnScreen = false
                if (!prefs.getBoolean("single_tap_to_activate", true)) {
                    keyboardNode = null
                    return
                }
                keyboardNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                keyboardNode = null
            }
            AccessibilityEvent.TYPE_VIEW_HOVER_ENTER -> {
                val node = event.source ?: return
                if (node.window?.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                    keyboardNode = if (node.isAccessibilityFocusable) node else node.logicParent
                } else {
                    keyboardNode = null
                }
            }
            AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED -> {
                val node = event.source ?: return
                val window = node.window
                if (window?.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                    disableTouch(window)
                } else if (window?.type != AccessibilityWindowInfo.TYPE_INPUT_METHOD && prefs.getBoolean("resume_browse_by_touch_and_gesture_detection_when_none_kbd_element_touched", true)) {
                    enableTouch()
                }
                val delay = (prefs.getString("long_press_delay", null) ?: "3").toLongOrNull()
                if (delay == null || delay == 0L) {
                    return
                }
                object : CountDownTimer(delay * 1000, delay) {
                    override fun onTick(tick: Long) {
                        if (node != keyboardNode || !isFingerOnScreen) {
                            cancel()
                        }                        
                    }
                    override fun onFinish() {
                        if (node != keyboardNode) {
                            return
                        }
                        keyboardNode?.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
                        keyboardNode = null
                    }
                }.start()
            }
        }
        } catch (e: Throwable) {
                    if (prefs.getBoolean("debug_mode", false)) {
            LogUtils.createDebugFile(this, "e: $e")
        }
        } finally {
                    if (prefs.getBoolean("debug_mode", false)) {
            LogUtils.createDebugFile(this, "onAccessibilityEvent $event")
        }
        } 
    }

    fun handleKeyboardInput() {
        windows?.forEach {
            if (!isKeyboardShowed && it?.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                disableTouch(it)
                return
            }
        }

        enableTouch()
    }

    fun enableTouch() {
        isKeyboardShowed = false
        setGestureDetectionPassthroughRegionInternal(Region())
        setTouchExplorationPassthroughRegionInternal(Region())
    }

    fun disableTouch(keyboardWindow: AccessibilityWindowInfo) {
        val disableGestureDetection = prefs.getBoolean("disable_gestures_when_keyboard_is_shown", true)
        val disableExploreByTouch = prefs.getBoolean("disable_explore_by_touch", false)
        if (!disableGestureDetection && !disableExploreByTouch) {
            return
        }
        if (Build.VERSION.SDK_INT > 29) {
        isKeyboardShowed = true
        val rect = Rect()
        val region = Region()
        keyboardWindow?.getBoundsInScreen(rect)
        region.set(rect)
        if (disableExploreByTouch) {
            setTouchExplorationPassthroughRegionInternal(region)
        } else if (disableGestureDetection) {
            setGestureDetectionPassthroughRegionInternal(region)
        }
        }
    }

    private fun setGestureDetectionPassthroughRegionInternal(region: Region) {
        if (Build.VERSION.SDK_INT > 29) {
            setGestureDetectionPassthroughRegion(0, region)
        }
    }

        private fun setTouchExplorationPassthroughRegionInternal(region: Region) {
        if (Build.VERSION.SDK_INT > 29) {
            setTouchExplorationPassthroughRegion(0, region)
        }
    }
}