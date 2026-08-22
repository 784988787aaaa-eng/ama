package com.example.ui.viewmodel

/**
 * الثوابت المحاسبية والمفاتيح المشتركة لطبقة إدارة الأموال (Finance Contracts & Constants)
 *
 * التوثيق المعماري:
 * 1. قيم `DEFAULT_CURRENCY_CODE` و `FALLBACK_CURRENCY_SYMBOL` تمثل العقود القياسية للنظام في حال غياب رمز العملة.
 * 2. مفاتيح التفضيلات (`PREFS_NAME`, `KEY_ONBOARDING_SHOWN`, `KEY_LINK_HABAYEB_DEBTS`) ثابتة لضمان التوافقية العكسية.
 * 3. قيم الأنواع والتكرار (`FREQ_DAILY`, `TYPE_OWED_BY_THEM`, الخ) تتطابق مع عقود قاعدة البيانات وسلسلة الـ JSON.
 */
object FinanceConstants {
    const val PREFS_NAME = "mizan_sec_prefs"
    const val KEY_ONBOARDING_SHOWN = "onboarding_shown"
    const val KEY_CATEGORIES_POPULATED = "categories_populated"
    const val KEY_ACTIVATION_CODE = "m_act_code"
    const val KEY_LINK_HABAYEB_DEBTS = "KEY_LINK_HABAYEB_DEBTS"
    
    // قيم افتراضية آمنة
    const val DEFAULT_CURRENCY_CODE = "DEFAULT"
    const val FALLBACK_CURRENCY_SYMBOL = "ر.ي"

    const val CATEGORY_CLOSED = "CLOSED"
    const val TYPE_OWED_TO_THEM = "OWED_TO_THEM"
    const val TYPE_OWED_BY_THEM = "OWED_BY_THEM"

    const val FREQ_DAILY = "DAILY"
    const val FREQ_WEEKLY = "WEEKLY"
    const val FREQ_MONTHLY = "MONTHLY"
}

enum class HabayebTransactionType {
    OWED_BY_THEM,
    PAYMENT_BY_THEM,
    OWED_TO_THEM,
    PAYMENT_TO_THEM
}

