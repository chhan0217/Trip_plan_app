package com.example.tripplan.data.mapper

import com.example.tripplan.data.local.ExchangeRateEntity
import com.example.tripplan.data.network.exchange.FrankfurterRateDto
import com.example.tripplan.domain.model.ExchangeRate
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.util.Currency
import java.util.Locale

fun FrankfurterRateDto.toEntityOrNull(
    rateDate: LocalDate,
    fetchedAt: Instant,
): ExchangeRateEntity? {
    val normalizedCode = quote.trim().uppercase(Locale.ROOT)
    val currency = runCatching { Currency.getInstance(normalizedCode) }.getOrNull()
        ?: return null
    val quotePerKrw = BigDecimal.valueOf(rate)
    if (quotePerKrw.signum() <= 0) return null
    val krwPerUnit = BigDecimal.ONE
        .divide(quotePerKrw, RATE_SCALE, RoundingMode.HALF_UP)
        .stripTrailingZeros()
    return ExchangeRateEntity(
        currencyCode = normalizedCode,
        currencyName = currency.getDisplayName(Locale.KOREAN),
        krwPerUnit = krwPerUnit.toPlainString(),
        rateDate = rateDate.toString(),
        fetchedAt = fetchedAt.toEpochMilli(),
    )
}

fun ExchangeRateEntity.toDomainOrNull(): ExchangeRate? {
    val parsedRate = krwPerUnit.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
        ?: return null
    return ExchangeRate(
        currencyCode = currencyCode,
        currencyName = currencyName,
        krwPerUnit = parsedRate,
    )
}

private const val RATE_SCALE = 12
