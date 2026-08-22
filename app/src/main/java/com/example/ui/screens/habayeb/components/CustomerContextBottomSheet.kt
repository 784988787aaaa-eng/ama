package com.example.ui.screens.habayeb.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.local.entities.CustomCategory
import com.example.ui.state.CustomerUiState
import com.example.ui.viewmodel.FinanceConstants

/**
 * نافذة تحكم الحساب المبتكرة والأنيقة للغاية:
 * تم تصميمها لتعبر عن طابع عصري رشيق، وتضم خيار الحذف المباشر السهل والسريع.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CustomerContextBottomSheet(
    customer: CustomerUiState,
    customCategories: List<CustomCategory>,
    isPinned: Boolean,
    activeThemeColor: Color,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onAssignCategory: (String?) -> Unit,
    onEnableMultiSelect: () -> Unit,
    onDelete: () -> Unit,
    onEditClick: () -> Unit,
    onUpdateCustomerType: (String) -> Unit,
    currentActiveCategory: String? = null
) {
    var showCategoriesState by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .width(310.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            AnimatedContent(
                targetState = showCategoriesState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(180)) with fadeOut(animationSpec = tween(180))
                },
                label = "contextMenuTransition"
            ) { isCategoryScreen ->
                if (!isCategoryScreen) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // رأس النافذة: أزرار الركن العلوية بدون خلفيات دائرية، مع توسيط عنوان النافذة واسم العميل بالكامل
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // أزرار الحذف والتعديل في الركن العلوي بدون خلفية دائرية
                            Row(
                                modifier = Modifier.align(Alignment.TopStart),
                                horizontalArrangement = Arrangement.spacedBy(0.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        onDismiss()
                                        onDelete()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.context_menu_delete_customer),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        onDismiss()
                                        onEditClick()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.habayeb_edit_name_title),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // عنوان النافذة واسم العميل في المنتصف تماماً
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 64.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(id = R.string.habayeb_manage_account_title),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = customer.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // فاصل بصري رقيق للغاية أسفل صف الاسم
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )

                        // خيارات الإدارة مرتفعة للأعلى بمكان احترافي وبلا مساحات ضائعة
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            // 1. تثبيت / إلغاء تثبيت الحساب
                            ContextMenuItem(
                                icon = Icons.Default.Star,
                                text = stringResource(if (isPinned) R.string.context_menu_unpin_customer else R.string.context_menu_pin_customer),
                                iconTint = activeThemeColor,
                                iconBgColor = activeThemeColor.copy(alpha = 0.1f),
                                onClick = {
                                    onTogglePin()
                                    onDismiss()
                                }
                            )

                            // 2. تصنيف الحساب
                            ContextMenuItem(
                                icon = Icons.Default.Folder,
                                text = stringResource(R.string.context_menu_add_to_category),
                                iconTint = activeThemeColor,
                                iconBgColor = activeThemeColor.copy(alpha = 0.1f),
                                onClick = {
                                    showCategoriesState = true
                                }
                            )

                            // إزالة من هذا التصنيف (تظهر فقط عند تصفح الحساب داخل تصنيف مخصص)
                            if (currentActiveCategory != null && currentActiveCategory != FinanceConstants.CATEGORY_CLOSED) {
                                ContextMenuItem(
                                    icon = Icons.Default.Close,
                                    text = stringResource(R.string.context_menu_remove_from_category, currentActiveCategory),
                                    iconTint = MaterialTheme.colorScheme.error,
                                    iconBgColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                    textColor = MaterialTheme.colorScheme.error,
                                    onClick = {
                                        onAssignCategory(null)
                                        onDismiss()
                                    }
                                )
                            }

                            // 3. تحديد متعدد للحسابات
                            ContextMenuItem(
                                icon = Icons.Default.Check,
                                text = stringResource(R.string.context_menu_multi_select),
                                iconTint = activeThemeColor,
                                iconBgColor = activeThemeColor.copy(alpha = 0.1f),
                                onClick = {
                                    onEnableMultiSelect()
                                    onDismiss()
                                }
                            )
                        }

                        // فاصل بصري رقيق للغاية قبل خيار تغيير نوع الحساب بالأسفل
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )

                        // قسم تغيير نوع الحساب معزول بصرياً بالأسفل بشكل فائق الأناقة والرشاقة
                        CustomerTypeChangeSection(
                            currentType = customer.originalCustomer.initialType,
                            activeThemeColor = activeThemeColor,
                            onUpdateCustomerType = onUpdateCustomerType,
                            onDismiss = onDismiss
                        )
                    }
                } else {
                    CustomerCategoryPickerSection(
                        customCategories = customCategories,
                        activeThemeColor = activeThemeColor,
                        onBack = { showCategoriesState = false },
                        onAssignCategory = onAssignCategory,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}
