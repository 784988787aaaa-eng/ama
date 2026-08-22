package com.example.ui.viewmodel.ledger

import android.content.Context
import com.example.data.local.entities.DeletedItemEntity
import org.json.JSONObject

object TrashRestoreHandler {
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
            e.printStackTrace()
        }
    }
}
