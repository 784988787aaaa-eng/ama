package com.example.ui.viewmodel.ledger

import android.content.Context
import android.util.Log
import com.example.data.local.entities.DeletedItemEntity
import org.json.JSONObject

/**
 * معالج استعادة تفضيلات وسجلات سلة المهملات (Trash Restore Preferences Handler)
 *
 * المسؤولية المعمارية:
 * 1. استخراج واستعادة البيانات الوصفية (Metadata) المرتبطة بالعميل المستعاد من سلة المهملات مثل (روابط التصنيف CAT_LINK_ وتثبيتات الفئات).
 * 2. عزل مسؤولية تحديث التفضيلات المشتركة خارج ViewModel لمنع تضخم الكود وفصل إدارة الحالة عن تخزين الإعدادات.
 * 3. حماية المعاملات من الانهيار مع تسجيل أي استثناءات تالفة دون إخفاء الأخطاء.
 */
object TrashRestoreHandler {
    private const val TAG = "TrashRestoreHandler"
    private const val PREFS_MIZAN_SEC = "mizan_sec_prefs"
    private const val TABLE_HABAYEB_BUNDLE = "habayeb_bundle"

    fun restorePrefsForDeletedItem(context: Context, item: DeletedItemEntity) {
        try {
            if (item.originalTableName == TABLE_HABAYEB_BUNDLE) {
                val root = JSONObject(item.jsonData)
                val custData = root.getJSONObject("customer")
                val cId = custData.getString("id")
                val sharedPrefs = context.getSharedPreferences(PREFS_MIZAN_SEC, Context.MODE_PRIVATE)

                if (custData.has("categoryLink")) {
                    val catLink = custData.getString("categoryLink")
                    sharedPrefs.edit().putString("CAT_LINK_$cId", catLink).apply()
                }

                if (custData.has("pinnedCategories")) {
                    val pinnedCats = custData.getJSONArray("pinnedCategories")
                    for (i in 0 until pinnedCats.length()) {
                        val catKey = pinnedCats.getString(i)
                        val key = "KEY_PINNED_IN_$catKey"
                        val existingSet = sharedPrefs.getStringSet(key, emptySet()) ?: emptySet()
                        val newSet = existingSet.toMutableSet().apply { add(cId) }
                        sharedPrefs.edit().putStringSet(key, newSet).apply()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring preferences for trash item ${item.id}", e)
        }
    }
}
