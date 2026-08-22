package com.example.domain.usecase.habayeb

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import com.example.R
import com.example.data.local.entities.CustomCategory
import com.example.data.repository.FinanceRepository
import com.example.ui.helper.VibrationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * مدير تصنيفات وتثبيتات عملاء الحبايب (Habayeb Category & Pinning Manager)
 *
 * المسؤوليات المعمارية الحاكمة:
 * 1. عزل إدارة التصنيفات والتثبيتات وقواعد الترتيب خارج ViewModel في طبقة Domain/UseCase.
 * 2. الاحتفاظ بذاكرة تخزين مؤقتة سريعة وآمنة للخيوط (ConcurrentHashMap) للروابط والتثبيتات لتجنب استدعاء SharedPreferences المتكرر أثناء تصفح القوائم.
 * 3. إدارة عمليات الحذف وإعادة التسمية والترتيب الأفقي بدقة ومراعاة اتجاه واجهة المستخدم من اليمين إلى اليسار (RTL).
 * 4. الحفاظ التام على المفاتيح التوافقية (PREFIX_CAT_LINK و PREFIX_KEY_PINNED_IN) لمنع كسر أي روابط سابقة.
 */
class HabayebCategoryManager(
    private val application: Application,
    private val repository: FinanceRepository,
    private val sharedPrefs: SharedPreferences
) {
    private val categoryMapCache = ConcurrentHashMap<String, String>()
    private val pinnedMapCache = ConcurrentHashMap<String, Set<String>>()

    init {
        // تحميل مسبق لكافة الروابط والتثبيتات في ذاكرة الوصول السريع مرة واحدة عند التهيئة
        try {
            sharedPrefs.all.forEach { (key, value) ->
                if (key.startsWith(PREFIX_CAT_LINK) && value is String) {
                    categoryMapCache[key.removePrefix(PREFIX_CAT_LINK)] = value
                } else if (key.startsWith(PREFIX_KEY_PINNED_IN) && value is Set<*>) {
                    @Suppress("UNCHECKED_CAST")
                    val set = (value as? Set<String>) ?: emptySet()
                    pinnedMapCache[key.removePrefix(PREFIX_KEY_PINNED_IN)] = set
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing category and pinned cache from SharedPreferences", e)
        }
    }

    private val _pinnedCustomerIds = MutableStateFlow<Set<String>>(getPinnedForCategory(null))
    val pinnedCustomerIds = _pinnedCustomerIds.asStateFlow()

    private val _categoryUpdateTrigger = MutableStateFlow(0)
    val categoryUpdateTrigger = _categoryUpdateTrigger.asStateFlow()

    fun triggerUpdate() {
        _categoryUpdateTrigger.value++
    }

    fun getCategoryMap(): Map<String, String> = categoryMapCache

    fun getPinnedForCategory(category: String?): Set<String> {
        val catKey = category ?: KEY_GLOBAL_ALL
        return pinnedMapCache[catKey] ?: run {
            val fromPrefs = sharedPrefs.getStringSet("$PREFIX_KEY_PINNED_IN$catKey", emptySet())?.toSet() ?: emptySet()
            pinnedMapCache[catKey] = fromPrefs
            fromPrefs
        }
    }

    suspend fun ensureClosedCategoryExists() = withContext(Dispatchers.IO) {
        val categories = repository.customCategoriesFlow.first()
        val hasClosed = categories.any { it.isSystemClosed }
        if (!hasClosed) {
            val defaultClosedName = application.getString(R.string.category_system_closed)
            val currentClosedName = sharedPrefs.getString(KEY_CLOSED_CUSTOM_NAME, defaultClosedName) ?: defaultClosedName
            repository.saveCustomCategory(
                CustomCategory(
                    name = currentClosedName,
                    tabType = TAB_TYPE_HABAYEB,
                    iconEmoji = DEFAULT_EMOJI,
                    displayOrder = 0,
                    isSystemClosed = true
                )
            )
        }
    }

    fun loadPinnedForCategory(category: String?) {
        val pinnedSet = getPinnedForCategory(category)
        _pinnedCustomerIds.value = pinnedSet
    }

    suspend fun togglePinCustomer(customerId: String, selectedCategory: String?): Boolean = withContext(Dispatchers.IO) {
        val catKey = selectedCategory ?: KEY_GLOBAL_ALL
        val activePinnedSet = (pinnedMapCache[catKey] ?: getPinnedForCategory(selectedCategory)).toMutableSet()
        if (activePinnedSet.contains(customerId)) {
            activePinnedSet.remove(customerId)
        } else {
            if (activePinnedSet.size >= MAX_PINNED_COUNT) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        application,
                        application.getString(R.string.habayeb_pin_limit_toast),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@withContext false
            }
            activePinnedSet.add(customerId)
        }
        val immutableSet = activePinnedSet.toSet()
        pinnedMapCache[catKey] = immutableSet
        sharedPrefs.edit().putStringSet("$PREFIX_KEY_PINNED_IN$catKey", activePinnedSet).apply()
        _pinnedCustomerIds.value = immutableSet
        triggerUpdate()
        true
    }

    suspend fun assignCategoryToCustomers(customerIds: List<String>, category: String?) = withContext(Dispatchers.IO) {
        val editor = sharedPrefs.edit()
        customerIds.forEach { id ->
            if (category == null) {
                categoryMapCache.remove(id)
                editor.remove("$PREFIX_CAT_LINK$id")
            } else {
                categoryMapCache[id] = category
                editor.putString("$PREFIX_CAT_LINK$id", category)
            }
        }
        editor.apply()
        triggerUpdate()
    }

    fun getCustomerCategory(customerId: String): String? = categoryMapCache[customerId]

    suspend fun renameClosedCategory(newName: String) = withContext(Dispatchers.IO) {
        val systemClosed = repository.customCategoriesFlow.first().find { it.isSystemClosed }
        if (systemClosed != null) {
            repository.saveCustomCategory(systemClosed.copy(name = newName))
        } else {
            repository.saveCustomCategory(
                CustomCategory(
                    name = newName,
                    tabType = TAB_TYPE_HABAYEB,
                    iconEmoji = DEFAULT_EMOJI,
                    displayOrder = 0,
                    isSystemClosed = true
                )
            )
        }
        sharedPrefs.edit().putString(KEY_CLOSED_CUSTOM_NAME, newName).apply()
        triggerUpdate()
    }

    suspend fun saveCustomCategory(name: String) = withContext(Dispatchers.IO) {
        val maxOrder = repository.customCategoriesFlow.first().maxOfOrNull { it.displayOrder } ?: 0
        repository.saveCustomCategory(
            CustomCategory(
                name = name,
                tabType = TAB_TYPE_HABAYEB,
                iconEmoji = "",
                displayOrder = maxOrder + 1
            )
        )
        triggerUpdate()
    }

    suspend fun renameCustomCategory(category: CustomCategory, newName: String) = withContext(Dispatchers.IO) {
        try {
            val oldName = category.name
            if (oldName == newName) return@withContext

            repository.saveCustomCategory(category.copy(name = newName))

            val editor = sharedPrefs.edit()
            categoryMapCache.forEach { (customerId, cat) ->
                if (cat == oldName) {
                    categoryMapCache[customerId] = newName
                    editor.putString("$PREFIX_CAT_LINK$customerId", newName)
                }
            }

            val oldPinnedKey = "$PREFIX_KEY_PINNED_IN$oldName"
            val newPinnedKey = "$PREFIX_KEY_PINNED_IN$newName"
            val pinnedSet = pinnedMapCache.remove(oldName) ?: sharedPrefs.getStringSet(oldPinnedKey, null)?.toSet()
            if (pinnedSet != null) {
                pinnedMapCache[newName] = pinnedSet
                editor.putStringSet(newPinnedKey, pinnedSet).remove(oldPinnedKey)
            }
            editor.apply()

            triggerUpdate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteCustomCategoryWithChoice(category: CustomCategory, deleteLinkedAccounts: Boolean) = withContext(Dispatchers.IO) {
        try {
            val editor = sharedPrefs.edit()
            val targetCategoryName = category.name
            val linkedCustomerIds = categoryMapCache.filter { it.value == targetCategoryName }.keys.toSet()

            if (deleteLinkedAccounts) {
                val allCustomers = repository.getAllCustomersDirect().filter { it.id in linkedCustomerIds }
                for (customer in allCustomers) {
                    val customerTxs = repository.getTransactionsForCustomerDirect(customer.id)
                    repository.softDeleteHabayebBundleToTrash(customer, customerTxs)
                    repository.deleteCustomerAndTransactions(customer.id)
                    categoryMapCache.remove(customer.id)
                    editor.remove("$PREFIX_CAT_LINK${customer.id}")
                }
            } else {
                for (id in linkedCustomerIds) {
                    categoryMapCache.remove(id)
                    editor.remove("$PREFIX_CAT_LINK$id")
                }
            }
            pinnedMapCache.remove(targetCategoryName)
            editor.remove("$PREFIX_KEY_PINNED_IN$targetCategoryName")
            editor.apply()

            repository.deleteCustomCategory(category)
            triggerUpdate()
            VibrationHelper.triggerDeleteVibration(application)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun moveCategoryLeft(currentOrder: List<String>, categoryName: String) = withContext(Dispatchers.IO) {
        val index = currentOrder.indexOf(categoryName)
        // In RTL Arabic layout, moving visually to the LEFT means moving towards higher index (index + 1)
        if (index >= 0 && index < currentOrder.size - 1) {
            val newList = currentOrder.toMutableList()
            val temp = newList[index]
            newList[index] = newList[index + 1]
            newList[index + 1] = temp
            triggerUpdate()
            repository.updateCustomCategoriesOrder(newList)
        }
    }

    suspend fun moveCategoryRight(currentOrder: List<String>, categoryName: String) = withContext(Dispatchers.IO) {
        val index = currentOrder.indexOf(categoryName)
        // In RTL Arabic layout, moving visually to the RIGHT means moving towards lower index (index - 1)
        if (index > 0) {
            val newList = currentOrder.toMutableList()
            val temp = newList[index]
            newList[index] = newList[index - 1]
            newList[index - 1] = temp
            triggerUpdate()
            repository.updateCustomCategoriesOrder(newList)
        }
    }

    suspend fun reorderCategories(newList: List<String>) = withContext(Dispatchers.IO) {
        repository.updateCustomCategoriesOrder(newList)
        triggerUpdate()
    }

    companion object {
        private const val TAG = "HabayebCategoryManager"
        const val PREFIX_CAT_LINK = "CAT_LINK_"
        private const val KEY_GLOBAL_ALL = "GLOBAL_ALL"
        private const val KEY_CLOSED_CUSTOM_NAME = "CLOSED_CUSTOM_NAME_KEY"
        private const val PREFIX_KEY_PINNED_IN = "KEY_PINNED_IN_"
        private const val TAB_TYPE_HABAYEB = "HABAYEB"
        private const val DEFAULT_EMOJI = "📁"
        private const val MAX_PINNED_COUNT = 3
    }
}
