package com.optimizer.android.utils

import android.view.accessibility.AccessibilityNodeInfo

object AccessibilityHelper {

    fun performClickOnNodesByText(rootNode: AccessibilityNodeInfo, texts: List<String>): Int {
        var clickCount = 0
        texts.forEach { text ->
            val nodes = rootNode.findAccessibilityNodeInfosByText(text)
            for (node in nodes) {
                if (node.isClickable && node.isEnabled) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    clickCount++
                }
                node.recycle()
            }
        }
        return clickCount
    }
}
