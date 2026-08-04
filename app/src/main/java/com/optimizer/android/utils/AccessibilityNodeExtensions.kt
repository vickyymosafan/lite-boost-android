package com.optimizer.android.utils

import android.view.accessibility.AccessibilityNodeInfo

fun AccessibilityNodeInfo.clickIfMatches(texts: List<String>): Int {
    var clickCount = 0
    texts.forEach { text ->
        val nodes = this.findAccessibilityNodeInfosByText(text)
        for (node in nodes) {
            try {
                if (node.isClickable && node.isEnabled) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    clickCount++
                }
            } finally {
                node.recycle()
            }
        }
    }
    return clickCount
}
