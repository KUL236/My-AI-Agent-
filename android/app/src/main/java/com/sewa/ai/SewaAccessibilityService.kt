package com.sewa.ai

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Phone-control bridge. It does nothing on its own: it only acts when
 * LocalCommandEngine calls it after an explicit user command.
 * The user must enable it manually in Settings > Accessibility.
 */
class SewaAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* intentionally unused */ }
    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        if (instance === this) instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    fun global(action: Int): Boolean = performGlobalAction(action)

    /** Scrolls the first scrollable view of the foreground app (never SEWA itself). */
    fun scroll(forward: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        if (root.packageName == packageName) return false
        val node = findScrollable(root) ?: return false
        val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        return node.performAction(action)
    }

    /** Taps the first clickable element whose text/description matches (never inside SEWA itself). */
    fun clickText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        if (root.packageName == packageName) return false
        for (match in root.findAccessibilityNodeInfosByText(text)) {
            var node: AccessibilityNodeInfo? = match
            while (node != null) {
                if (node.isClickable && node.isEnabled) return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                node = node.parent
            }
        }
        return false
    }

    private fun findScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findScrollable(child)?.let { return it }
        }
        return null
    }

    companion object {
        @Volatile var instance: SewaAccessibilityService? = null
        val isEnabled: Boolean get() = instance != null
    }
}
