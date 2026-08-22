package com.example.data.serialization.pdf

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.R
import org.json.JSONArray

data class BusinessHeaderData(
    val displayedName: String,
    val displayedDesc: String,
    val phonesStr: String,
    val hasLogo: Boolean,
    val logoW: Float,
    val logoH: Float,
    val scaledLogo: Bitmap?,
    val rawBitmap: Bitmap?
)

object BusinessProfileLoader {
    private const val TAG = "BusinessProfileLoader"
    private const val PREF_BUSINESS_PROFILE = "business_profile"
    private const val PREF_BUSINESS_PROFILE_ALT = "business_profile_prefs"
    private const val KEY_BIZ_NAME = "biz_name"
    private const val KEY_BIZ_DESC = "biz_desc"
    private const val KEY_BIZ_LOGO_PATH = "biz_logo_path"
    private const val KEY_BIZ_PHONES = "biz_phones"
    private const val KEY_ALT_NAME = "business_name"
    private const val KEY_ALT_SLOGAN = "business_slogan"
    private const val KEY_ALT_LOGO_PATH = "logo_path"
    private const val KEY_ALT_PHONE = "business_phone"

    fun load(context: Context): BusinessHeaderData {
        val prefs = context.getSharedPreferences(PREF_BUSINESS_PROFILE, Context.MODE_PRIVATE)
        val altPrefs = context.getSharedPreferences(PREF_BUSINESS_PROFILE_ALT, Context.MODE_PRIVATE)

        var bizName = prefs.getString(KEY_BIZ_NAME, "")?.trim().orEmpty()
        if (bizName.isBlank()) {
            bizName = altPrefs.getString(KEY_ALT_NAME, "")?.trim().orEmpty()
        }

        var bizDesc = prefs.getString(KEY_BIZ_DESC, "")?.trim().orEmpty()
        if (bizDesc.isBlank()) {
            bizDesc = altPrefs.getString(KEY_ALT_SLOGAN, "")?.trim().orEmpty()
        }

        var bizLogoPath = prefs.getString(KEY_BIZ_LOGO_PATH, "")?.trim().orEmpty()
        if (bizLogoPath.isBlank()) {
            bizLogoPath = altPrefs.getString(KEY_ALT_LOGO_PATH, "")?.trim().orEmpty()
        }

        val bizPhones = mutableListOf<String>()
        try {
            val phonesJson = prefs.getString(KEY_BIZ_PHONES, "[]") ?: "[]"
            val jsonArray = JSONArray(phonesJson)
            for (i in 0 until jsonArray.length()) {
                val p = jsonArray.getString(i).trim()
                if (p.isNotBlank()) bizPhones.add(p)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading business phones", e)
        }

        if (bizPhones.isEmpty()) {
            val altPhone = altPrefs.getString(KEY_ALT_PHONE, "")?.trim().orEmpty()
            if (altPhone.isNotBlank()) {
                bizPhones.add(altPhone)
            }
        }

        val displayedName = if (bizName.isNotBlank()) bizName else context.getString(R.string.app_name)
        val displayedDesc = if (bizDesc.isNotBlank()) bizDesc else context.getString(R.string.pdf_default_desc)

        val logoResult = PdfDrawingUtils.loadAndScaleLogo(context, bizLogoPath)
        val phonesToDraw = if (bizPhones.isNotEmpty()) bizPhones else listOf(context.getString(R.string.pdf_certified_identity))
        val phonesStr = if (bizPhones.isNotEmpty()) context.getString(R.string.pdf_phone_prefix) + " " + phonesToDraw.joinToString(" - ") else phonesToDraw.joinToString(" - ")

        return BusinessHeaderData(
            displayedName = displayedName,
            displayedDesc = displayedDesc,
            phonesStr = phonesStr,
            hasLogo = logoResult.hasLogo,
            logoW = logoResult.width,
            logoH = logoResult.height,
            scaledLogo = logoResult.bitmap,
            rawBitmap = logoResult.rawBitmapToRecycle
        )
    }
}
