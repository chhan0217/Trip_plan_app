package com.example.tripplan.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey val currencyCode: String,
    val currencyName: String,
    val krwPerUnit: String,
    val rateDate: String,
    val fetchedAt: Long,
)
