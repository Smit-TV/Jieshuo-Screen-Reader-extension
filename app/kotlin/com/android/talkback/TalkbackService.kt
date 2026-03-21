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
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.accessibility.AccessibilityManager
import android.util.Log
import android.view.KeyEvent
import com.android.talkback.*

class TalkbackService : AccessibilityService(),
TextToSpeech.OnInitListener {
    private lateinit var prefs: SharedPreferences
    private lateinit var powerManager: PowerManager
    private var keyboardNode: AccessibilityNodeInfo? = null
    private var isKeyboardShowed = false
    private var isFingerOnScreen = false
    private var isTEAndGDResumed = false
    private var isRunning = false
    private lateinit var accessibilityManager: AccessibilityManager
    private lateinit var tts: TextToSpeech

    companion object {
        const val TAG = "JieshuoExtension"
    }

    override fun onInit(status: Int) {
       tts.setAudioAttributes(AudioAttributes.Builder()
            .setFlags(AudioAttributes.FLAG_LOW_LATENCY)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
            .build())
        tts.setLanguage(tts.defaultLanguage ?: return)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        val info = serviceInfo
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY
        serviceInfo = info
        isRunning = true
        if (prefs.getBoolean("debug_mode", false)) {
            LogUtils.createDebugFile(this, "onServiceConnected")
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
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
        accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        tts = TextToSpeech(this, this)
    }

    override fun onDestroy() {
                if (prefs.getBoolean("debug_mode", false)) {
            LogUtils.createDebugFile(this, "onDestroy")
        }
        super.onDestroy()
        tts.shutdown()
        isRunning = false
        enableTouch()
        isFingerOnScreen = false
        keyboardNode = null
        if (Build.VERSION.SDK_INT > 32) {
            clearCache()
        }
    }

    override fun onLowMemory() {
        if (Build.VERSION.SDK_INT > 32) {
            clearCache()
        }
        super.onLowMemory()
    }

    override fun onGesture(gestureId: Int): Boolean = false
    override fun onGesture(gesture: AccessibilityGestureEvent): Boolean = false
    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!isRunning || !powerManager.isInteractive) {
            return
        }
 try {
    when (event.eventType) {
        AccessibilityEvent.TYPE_WINDOWS_CHANGED -> handleKeyboardInput(event)
        AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED -> handleFocusEvent(event)
        AccessibilityEvent.TYPE_VIEW_HOVER_ENTER -> handleHoverEnter(event)
        AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START -> isFingerOnScreen = true
        AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END -> handleTouchExplorationGestureEnd(event)
        AccessibilityEvent.TYPE_TOUCH_INTERACTION_END -> handleTouchInteractionEnd()
        AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> handleKeyClick(event)
    }
 } catch (e: Throwable) {
    if (prefs.getBoolean("debug_mode", false)) {
        LogUtils.createDebugFile(this, "e: $e")
    }
 } finally {
        if (prefs.getBoolean("debug_mode", false)) {
        LogUtils.createDebugFile(this, "$event")
    }
 }
    }

    fun handleTouchInteractionEnd() {
        val focusedWindow = findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)?.window ?: return
        if (focusedWindow.type != AccessibilityWindowInfo.TYPE_INPUT_METHOD && prefs.getBoolean("resume_browse_by_touch_and_gesture_detection_when_none_kbd_element_touched", true)) {
            enableTouch()
        }
    }

fun handleHoverEnter(event: AccessibilityEvent) {
    val source = event.getSource() ?: return
    val window = source.window
    if (window?.type != AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
        keyboardNode = null
        if (!isKeyboardShowed && prefs.getBoolean("resume_browse_by_touch_and_gesture_detection_when_none_kbd_element_touched", true)) {
            isTEAndGDResumed = true
            enableTouch()
        }
        return
    }

    keyboardNode = if (source.isAccessibilityFocusable) {
        source
    } else {
        source.logicParent
    }

    if (!isKeyboardShowed) {
    disableTouch(window)
    }
}

fun handleTouchExplorationGestureEnd(event: AccessibilityEvent) {
    if (event.packageName == packageName) {
        return
    }
    isFingerOnScreen = false
    if (keyboardNode == null || !prefs.getBoolean("single_tap_to_activate", true)) {
        keyboardNode = null
        return
    }
    keyboardNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    keyboardNode = null
}

fun handleFocusEvent(event: AccessibilityEvent) {
    val node = event.source ?: return
    val longPressDelay = (prefs.getString("long_press_delay", null) ?: "3000").toLongOrNull()
    if (longPressDelay == null || longPressDelay <= 0L) {
        return
    }
    val timer = object : CountDownTimer(longPressDelay, 1) {
        override fun onTick(tick: Long) {
            if (node == keyboardNode && isFingerOnScreen) {
                return
            }
            cancel()
        }
        override fun onFinish() {
            if (node != keyboardNode || !isFingerOnScreen) {
                return
            }
            keyboardNode?.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
            keyboardNode = null
        }
    }.start()
}


    fun handleKeyboardInput(event: AccessibilityEvent) {
        val windowChanges = event.windowChanges
        val keyboardWindow = findWindowByType(AccessibilityWindowInfo.TYPE_INPUT_METHOD)
        if (keyboardWindow == null) {
            enableTouch()
        } else {
            processInputWindow(keyboardWindow)
        }
    }

    fun processInputWindow(keyboardWindow: AccessibilityWindowInfo) {
        if (isKeyboardShowed && !keyboardWindow.isAccessibilityFocused && prefs.getBoolean("resume_browse_by_touch_and_gesture_detection_when_none_kbd_element_touched", true)) {
            enableTouch()
            return
        }
        disableTouch(keyboardWindow)
    }

    fun findWindowById(windowId: Int): AccessibilityWindowInfo? {
        windows?.forEach {
            if (windowId == it.id) {
                return it
            }
        }
        return null
    }

    fun findWindowByType(windowType: Int): AccessibilityWindowInfo? {
        windows?.forEach {
            if (it.type == windowType) {
                return it
            }
        }
        return null
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
    isKeyboardShowed = true
    isTEAndGDResumed = false
    val region = calculateRegion(keyboardWindow)
    if (disableExploreByTouch) {
        setTouchExplorationPassthroughRegionInternal(region)
    } else if (disableGestureDetection) {
        setGestureDetectionPassthroughRegionInternal(region)
        }
    }

    fun calculateRegion(keyboardWindow: AccessibilityWindowInfo): Region {
    val rect = Rect()
        val region = Region()
    keyboardWindow?.getBoundsInScreen(rect)
        region.set(rect)
    return region
    }

fun disableBrowseByTouch(window: AccessibilityWindowInfo) {
    val region = calculateRegion(window)
    isKeyboardShowed = true
    setTouchExplorationPassthroughRegionInternal(region)
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

    fun handleKeyClick(event: AccessibilityEvent) {

        if (accessibilityManager.isTouchExplorationEnabled || !prefs.getBoolean("speak_keyboard_input_aloud", false)) {
            return
        }

        var text = event.text?.getOrNull(0) ?: event.contentDescription ?: event.beforeText ?: return
        val beforeText = event.beforeText ?: return
        var typedLetter = try { text.substring(event.fromIndex + event.removedCount, event.fromIndex + event.addedCount) } catch (e: Exception) {
            ""
        }

        if (typedLetter == "") {
            typedLetter = try { beforeText.substring(event.fromIndex + event.addedCount, event.fromIndex + event.removedCount) } catch (e: java.lang.Exception) { "" }
        }

        val currentCaretPosition = event.fromIndex + event.addedCount ?: -1

        var i = 0

        while (typedLetter == "") {
            val currentTextLetter = text.getOrNull(i)?.toString()
            val beforeTextLetter = beforeText.getOrNull(i)?.toString()


            if (currentTextLetter == null && beforeTextLetter == null) {
                break
            }

            // Text was removed by user
            if (currentTextLetter == null && beforeTextLetter != null) {
                typedLetter = beforeText.substring(i)
                break
            }
            // User is typing
            if (beforeTextLetter == null && currentTextLetter != null) {
                typedLetter = text.substring(i)
                break
            }
            i++
            if (currentTextLetter == beforeTextLetter) {
                continue
            }


                break
        }

        if (prefs.getBoolean("speak_word_after_space", true) && currentCaretPosition > -1 && (typedLetter == " " || typedLetter == "\n")) {
            val words = text.substring(0, currentCaretPosition).trimEnd().split("\\s+".toRegex())
            typedLetter = words.last()
        }

        speak(typedLetter)
    }

    fun speak(text: CharSequence) {
        if (text.trim().filterNot { it.isWhitespace() }.isEmpty() || text.isBlank()) {
            return
        }

        if (text.length == 1 && text[0].isLetter() &&  text[0].isUpperCase() && prefs.getBoolean("increase_pitch_for_capital_letters", true)) {
            tts.setPitch(1.5f)
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        tts.setPitch(1.0f)
    }

}