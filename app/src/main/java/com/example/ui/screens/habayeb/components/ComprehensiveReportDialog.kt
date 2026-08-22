package com.example.ui.screens.habayeb.components

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.serialization.PdfReportGenerator
import com.example.data.serialization.pdf.MasterBookletPdfEngine
import com.example.ui.state.CustomerUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
fun ComprehensiveReportDialog(
    customers: List<CustomerUiState>,
    currencySymbol: String,
    activeThemeColor: Color,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    selectedCustomerIds: List<String> = emptyList()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isGeneratingPdf by remember { mutableStateOf(false) }

    // Booklet states
    var isGeneratingBooklet by remember { mutableStateOf(false) }
    var bookletProgress by remember { mutableStateOf(0) }
    var bookletTotal by remember { mutableStateOf(0) }
    var bookletJob by remember { mutableStateOf<Job?>(null) }

    val selectedIdsSet = remember(selectedCustomerIds) { selectedCustomerIds.toSet() }

    // Aggregate statistics wrapped in remember to avoid heavy recount during scroll
    val aggregateStats = remember(customers) {
        var owedByThem = BigDecimal.ZERO
        var owedToThem = BigDecimal.ZERO
        val foreignMap = mutableMapOf<String, BigDecimal>()

        customers.forEach { c ->
            val bdVal = c.defaultCurrencyTotal
            val cmp = bdVal.compareTo(BigDecimal.ZERO)
            if (cmp > 0) {
                owedByThem = owedByThem.add(bdVal)
            } else if (cmp < 0) {
                owedToThem = owedToThem.add(bdVal.abs())
            }
            c.foreignDebts.forEach { (curr, valBd) ->
                foreignMap[curr] = (foreignMap[curr] ?: BigDecimal.ZERO).add(valBd)
            }
        }
        val net = owedByThem.subtract(owedToThem)
        val nonZeroForeign = foreignMap.filter { it.value.compareTo(BigDecimal.ZERO) != 0 }
        Triple(owedByThem, owedToThem, Pair(net, nonZeroForeign))
    }

    val totalOwedByThem = aggregateStats.first
    val totalOwedToThem = aggregateStats.second
    val netPrimary = aggregateStats.third.first
    val nonZeroForeign = aggregateStats.third.second

    val backgroundColor = MaterialTheme.colorScheme.background
    val isDark = remember(backgroundColor) { backgroundColor.run { red < 0.5f } }
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = modifier
                    .fillMaxWidth(0.9f)
                    .widthIn(max = 480.dp)
                    .wrapContentHeight()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.drawer_comprehensive_report_label),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeThemeColor
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(id = R.string.report_btn_close),
                                tint = textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    val netPrimaryColor = if (netPrimary.compareTo(BigDecimal.ZERO) > 0) {
                        if (isDark) com.example.ui.theme.SoftGreen else MaterialTheme.colorScheme.primary
                    } else if (netPrimary.compareTo(BigDecimal.ZERO) < 0) {
                        if (isDark) com.example.ui.theme.SoftRed else MaterialTheme.colorScheme.error
                    } else {
                        textSecondary
                    }
                    val netStatus = if (netPrimary.compareTo(BigDecimal.ZERO) > 0) {
                        stringResource(id = R.string.report_status_for_us)
                    } else if (netPrimary.compareTo(BigDecimal.ZERO) < 0) {
                        stringResource(id = R.string.report_status_on_us)
                    } else {
                        stringResource(id = R.string.report_status_balanced)
                    }

                    // Main Hero Card (highly compact)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = activeThemeColor.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(id = R.string.report_net_total_pattern, netStatus),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textSecondary
                                )
                                Text(
                                    text = com.example.ui.helper.HabayebMathHelper.formatSmart(netPrimary.abs()) + " " + currencySymbol,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = netPrimaryColor
                                )
                            }
                            
                            // Active accounts count badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(activeThemeColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.report_active_accounts_count, customers.size),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = activeThemeColor
                                )
                            }
                        }
                    }

                    // Foreign Currencies (if any, super compact row)
                    if (nonZeroForeign.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.report_other_currencies_label),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary
                            )
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                for ((curr, bd) in nonZeroForeign) {
                                    val status = if (bd.compareTo(BigDecimal.ZERO) > 0) {
                                        stringResource(id = R.string.report_status_for_us)
                                    } else {
                                        stringResource(id = R.string.report_status_on_us)
                                    }
                                    val badgeBg = if (bd.compareTo(BigDecimal.ZERO) > 0) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    } else {
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                    }
                                    val badgeTextColor = if (bd.compareTo(BigDecimal.ZERO) > 0) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(badgeBg)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$curr: ${com.example.ui.helper.HabayebMathHelper.formatSmart(bd.abs())} ($status)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeTextColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Scope Indicator Text
                    val scopeText = if (selectedCustomerIds.isNotEmpty()) {
                        stringResource(id = R.string.report_scope_selected_only, selectedCustomerIds.size)
                    } else {
                        stringResource(id = R.string.report_scope_all, customers.size)
                    }

                    Text(
                        text = "📋 $scopeText",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    // Two Buttons Row
                    val bookletErrorToastStr = stringResource(id = R.string.report_booklet_error_toast)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // General Report Button (soft color, tonal style)
                        Button(
                            onClick = {
                                if (!isGeneratingPdf) {
                                    isGeneratingPdf = true
                                    val targetCustomers = if (selectedCustomerIds.isNotEmpty()) {
                                        customers.filter { selectedIdsSet.contains(it.id) }
                                    } else {
                                        customers
                                    }
                                    PdfReportGenerator.generateAndHandleAllCustomersPdfReportAsync(
                                        context = context,
                                        scope = coroutineScope,
                                        customers = targetCustomers,
                                        currencySymbol = currencySymbol,
                                        action = "SHARE",
                                        onFinished = {
                                            isGeneratingPdf = false
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = activeThemeColor.copy(alpha = 0.12f),
                                contentColor = activeThemeColor
                            ),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isGeneratingPdf && !isGeneratingBooklet
                        ) {
                            if (isGeneratingPdf) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = activeThemeColor,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(id = R.string.report_type_general),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Detailed Report Button (solid primary style)
                        Button(
                            onClick = {
                                val job = coroutineScope.launch {
                                    isGeneratingBooklet = true
                                    bookletProgress = 0
                                    bookletTotal = if (selectedCustomerIds.isNotEmpty()) selectedCustomerIds.size else customers.size

                                    val hexColor = "#" + Integer.toHexString(activeThemeColor.toArgb()).substring(2)
                                    MasterBookletPdfEngine.generateBookletPdfAsync(
                                        context = context,
                                        allCustomers = customers,
                                        selectedIds = selectedCustomerIds,
                                        onlySelected = selectedCustomerIds.isNotEmpty(),
                                        currencySymbol = currencySymbol,
                                        primaryColorHex = hexColor,
                                        onProgress = { processed, total ->
                                            bookletProgress = processed
                                            bookletTotal = total
                                        },
                                        onFinished = { file ->
                                            isGeneratingBooklet = false
                                            if (file != null) {
                                                PdfReportGenerator.triggerShareOrViewIntent(context, file, "SHARE")
                                            } else {
                                                Toast.makeText(context, bookletErrorToastStr, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    )
                                }
                                bookletJob = job
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = activeThemeColor),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isGeneratingBooklet && !isGeneratingPdf
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(id = R.string.report_type_detailed),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Modern Progress Dialog for Booklet
    if (isGeneratingBooklet) {
        Dialog(
            onDismissRequest = { /* Prevent dismiss */ },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.report_booklet_generating_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeThemeColor,
                        textAlign = TextAlign.Center
                    )

                    val progressFraction = if (bookletTotal > 0) bookletProgress.toFloat() / bookletTotal.toFloat() else 0f
                    LinearProgressIndicator(
                        progress = progressFraction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = activeThemeColor,
                        trackColor = activeThemeColor.copy(alpha = 0.2f)
                    )

                    Text(
                        text = stringResource(id = R.string.report_booklet_progress_fmt, bookletProgress, bookletTotal),
                        fontSize = 12.sp,
                        color = textSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            bookletJob?.cancel()
                            isGeneratingBooklet = false
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(id = R.string.report_booklet_cancel_btn), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
