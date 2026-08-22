package com.example.data.local

import com.example.ui.navigation.Screen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * تفضيلات التنقل والشاشة الافتراضية للتطبيق.
 * تم تثبيت الشاشة الافتراضية حتمياً على شاشة "حبايب" (HABAYEB).
 */
class NavigationPreferences {
    companion object {
        val DEFAULT_START = Screen.HABAYEB.name
    }

    val defaultStartFlow: Flow<String> = flowOf(DEFAULT_START)
}
