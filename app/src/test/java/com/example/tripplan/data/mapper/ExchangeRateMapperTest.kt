package com.example.tripplan.data.mapper

import com.example.tripplan.data.local.ExchangeRateEntity
import com.example.tripplan.data.network.exchange.FrankfurterRateDto
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExchangeRateMapperTest {
    @Test
    fun frankfurterRate_invertsKrwBaseRateAndUsesIsoCurrencyCode() {
        val entity = FrankfurterRateDto(
            date = "2026-11-10",
            base = "KRW",
            quote = "vnd",
            rate = 18.969,
        ).toEntityOrNull(
            rateDate = LocalDate.of(2026, 11, 10),
            fetchedAt = Instant.ofEpochMilli(123L),
        )

        requireNotNull(entity)
        assertEquals("VND", entity.currencyCode)
        assertEquals("베트남 동", entity.currencyName)
        assertEquals("0.05271759186", entity.krwPerUnit)
        assertEquals("2026-11-10", entity.rateDate)
        assertEquals(123L, entity.fetchedAt)
    }

    @Test
    fun frankfurterRate_rejectsNonIsoCurrencyAssets() {
        val entity = FrankfurterRateDto(
            date = "2026-11-10",
            base = "KRW",
            quote = "BTC",
            rate = 0.0000005,
        ).toEntityOrNull(LocalDate.of(2026, 11, 10), Instant.EPOCH)

        assertNull(entity)
    }

    @Test
    fun entity_mapsStoredDecimalToDomainBigDecimal() {
        val domain = ExchangeRateEntity(
            currencyCode = "USD",
            currencyName = "미국 달러",
            krwPerUnit = "1352.576",
            rateDate = "2026-11-10",
            fetchedAt = 1L,
        ).toDomainOrNull()

        requireNotNull(domain)
        assertEquals("USD", domain.currencyCode)
        assertEquals(BigDecimal("1352.576"), domain.krwPerUnit)
    }
}
