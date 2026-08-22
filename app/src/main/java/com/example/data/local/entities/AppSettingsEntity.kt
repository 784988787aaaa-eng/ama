package com.example.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

object DatabaseDefaults {
    const val DEFAULT_CURRENCY_SYMBOL = "ر.ي"
}

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey @ColumnInfo(name = "id") val id: Int = 1,
    @ColumnInfo(name = "currencySymbol") val currencySymbol: String = DatabaseDefaults.DEFAULT_CURRENCY_SYMBOL,
    @ColumnInfo(name = "schoolExpensesEnabled") val schoolExpensesEnabled: Boolean = true,
    @ColumnInfo(name = "themeMode") val themeMode: Int = 0, // 0 = Auto, 1 = Light, 2 = Dark
    @ColumnInfo(name = "doubleCheckExit") val doubleCheckExit: Boolean = true,
    @ColumnInfo(name = "isPasscodeEnabled") val isPasscodeEnabled: Boolean = false,
    @ColumnInfo(name = "passcodeHash") val passcodeHash: String? = null,
    @ColumnInfo(name = "recoveryPhraseHash") val recoveryPhraseHash: String? = null,
    @ColumnInfo(name = "recoveryHint") val recoveryHint: String? = null,
    @ColumnInfo(name = "tempPart") val tempPart: String = "",
    @ColumnInfo(name = "permPart") val permPart: String = "",
    @ColumnInfo(name = "unifiedDeviceId") val unifiedDeviceId: String = "",
    @ColumnInfo(name = "isFirstLaunch") val isFirstLaunch: Boolean = true,
    @ColumnInfo(name = "isAutoBackupEnabled") val isAutoBackupEnabled: Boolean = true,
    @ColumnInfo(name = "isCloudSyncEnabled") val isCloudSyncEnabled: Boolean = false,
    @ColumnInfo(name = "exchangeRateSar") val exchangeRateSar: Double = 1.0,
    @ColumnInfo(name = "exchangeRateUsd") val exchangeRateUsd: Double = 1.0,
    @ColumnInfo(name = "exchangeRateYer") val exchangeRateYer: Double = 1.0,
    @ColumnInfo(name = "exchangeRatesJson") val exchangeRatesJson: String = "{}"
) {
    val exchangeRateSarBigDecimal: BigDecimal get() = BigDecimal.valueOf(exchangeRateSar)
    val exchangeRateUsdBigDecimal: BigDecimal get() = BigDecimal.valueOf(exchangeRateUsd)
    val exchangeRateYerBigDecimal: BigDecimal get() = BigDecimal.valueOf(exchangeRateYer)
}
