package com.android.talkback
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction
import android.util.Log




val AccessibilityNodeInfo.canScroll: Boolean 
get() {
val cln = className ?: ""
return isScrollable && ( (cln.indexOf("android.") == 0
|| cln.indexOf("androidx.") == 0))
}

val AccessibilityNodeInfo.isEditText: Boolean
get() {
return (className == "android.widget.EditText" ||
actionList.indexOf(AccessibilityAction.ACTION_PASTE) != -1 ||
actionList.indexOf(AccessibilityAction.ACTION_COPY) != -1 ||
actionList.indexOf(AccessibilityAction.ACTION_CUT) != -1 ||
actionList.indexOf(AccessibilityAction.ACTION_SET_TEXT) != -1)
}



val AccessibilityNodeInfo.hasClickable: Boolean
get() {
return isClickable || actionList.indexOf(AccessibilityAction.ACTION_CLICK) != -1
}
val AccessibilityNodeInfo.hasLongClickable: Boolean
get() {
return isLongClickable || actionList.indexOf(AccessibilityAction.ACTION_LONG_CLICK) != -1
}
val AccessibilityNodeInfo.hasAnyClickable: Boolean
get() = hasClickable || hasLongClickable

val AccessibilityNodeInfo.hasFocusable: Boolean
get() {
return isFocusable || actionList.indexOf(AccessibilityAction.ACTION_FOCUS) != -1
}

val AccessibilityNodeInfo.isSeekBar: Boolean
get() {
return (
rangeInfo != null
|| className == "android.widget.SeekBar"
|| className == "android.widget.ProgressBar"
 || actionList.indexOf(AccessibilityAction.ACTION_SET_PROGRESS) != -1)
}

val AccessibilityNodeInfo.isCollection: Boolean
get() {
val cln = className ?: ""
return collectionInfo != null || cln.indexOf("android") == 0 &&
(cln.indexOf("RecyclerView") != -1
|| cln.indexOf("ListView") != -1
|| cln.indexOf("GridView") != -1)
}

val AccessibilityNodeInfo.isScrollView: Boolean
get() {
if (isSeekBar || className == "android.widget.Spinner") {
return false
}
val cln = className ?: ""
if (cln.indexOf("android") == 0 && (
cln.indexOf("ScrollView") != -1 || cln.indexOf("ViewPager") != -1)
|| isScrollable) {
return true
}
    val scrollActions = listOf(
        AccessibilityAction.ACTION_SCROLL_UP,
        AccessibilityAction.ACTION_SCROLL_DOWN,
        AccessibilityAction.ACTION_SCROLL_LEFT,
        AccessibilityAction.ACTION_SCROLL_RIGHT,
        AccessibilityAction.ACTION_SCROLL_FORWARD,
        AccessibilityAction.ACTION_SCROLL_BACKWARD,
        if (Build.VERSION.SDK_INT > 33) AccessibilityAction.ACTION_SCROLL_IN_DIRECTION else AccessibilityAction.ACTION_SCROLL_UP,
        AccessibilityAction.ACTION_SCROLL_TO_POSITION
    )

    for (action in scrollActions) {
        if (actionList.indexOf(action) != -1) {
return true
        }
    }
return false
}

val AccessibilityNodeInfo.hasLabel: Boolean
get() = contentDescription?.isNotEmpty() == true || text?.isNotEmpty() == true

val AccessibilityNodeInfo.isHTMLElement: Boolean
get() = !isWebView && (actionList.indexOf(AccessibilityAction.ACTION_NEXT_HTML_ELEMENT) != -1 || actionList.indexOf(AccessibilityAction.ACTION_PREVIOUS_HTML_ELEMENT) != -1)

val AccessibilityNodeInfo.hasAvailableNode: Boolean
get() {
    if (isCollection || isScrollView) {
        return false
    }
for (i in 0 until childCount) {
val child = getChild(i) ?: continue
if (!child.isVisibleToUser || child.isClickable || child.isLongClickable || child.isFocusable || child.isScreenReaderFocusable || child.isWebView 
|| !child.hasLabel && child.childCount == 0
|| child.childCount > 0 && !child.hasAvailableNode) {
continue
}
if (child.childCount == 0 && !child.hasLabel 
|| contentDescription?.isNotEmpty() == true && child.contentDescription?.isNotEmpty() == true) {
continue
}
return true
}
return false
}

val AccessibilityNodeInfo.isWebView: Boolean
get() = extras.getString("AccessibilityNodeInfo.chromeRole", "") == "rootWebArea"

val AccessibilityNodeInfo.isAccessibilityFocusable: Boolean
get() {
    if (!isVisibleToUser || isWebView) {
        return false
    }
    if (isHTMLElement) {
        return isClickable || isLongClickable || isFocusable || hasLabel
    }

    val hasNodes = hasAvailableNode
    val permission = (isVisibleToUser && (hasNodes || contentDescription?.isNotEmpty() == true || childCount == 0) &&
    (isClickable || isLongClickable || isFocusable || isScreenReaderFocusable || isSeekBar) && 
    (!isScrollView && !isCollection))
    if (permission || hasNodes && (collectionItemInfo != null || parent?.isCollection == true || parent?.isScrollView == true)) {
        return true
    }
    val lp = logicParent
    val permission2 = (lp == null && (hasLabel || isCheckable)
    || lp != null && lp.contentDescription?.isNotEmpty() == true && contentDescription?.isNotEmpty() == true &&
    !lp.isClickable && !lp.isLongClickable && !lp.isFocusable)
    if (permission2) {
        return true
    }
    return false
}

val AccessibilityNodeInfo.logicParent: AccessibilityNodeInfo?
get() {
var p = parent
while (p != null) {
if (p.isAccessibilityFocusable) {
return p
}
p = p.parent
}
return null
}

fun AccessibilityNodeInfo.performFocus(): Boolean = performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)

