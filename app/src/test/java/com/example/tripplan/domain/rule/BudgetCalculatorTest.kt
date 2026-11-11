package com.example.tripplan.domain.rule

import com.example.tripplan.domain.model.AddBudgetEntryInput
import com.example.tripplan.domain.model.CreateTripBudgetInput
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetCalculatorTest {
    private val rateDate = LocalDate.of(2026, 11, 11)

    @Test
    fun convertToKrw_usesInputRateAndRoundsToWon() {
        assertEquals(
            BigDecimal("2856000"),
            BudgetCalculator.convertToKrw(BigDecimal("300000"), BigDecimal("9.52")),
        )
        assertEquals(
            BigDecimal("53"),
            BudgetCalculator.convertToKrw(BigDecimal("1000"), BigDecimal("0.05272")),
        )
    }

    @Test
    fun convertFromKrw_usesInputRateAndRoundsToTwoDecimals() {
        assertEquals(
            BigDecimal("105042.02"),
            BudgetCalculator.convertFromKrw(BigDecimal("1000000"), BigDecimal("9.52")),
        )
    }

    @Test
    fun validate_rejectsInvalidMoneyAndCurrency() {
        val errors = BudgetCalculator.validate(
            CreateTripBudgetInput(
                currencyCode = "JP",
                krwAmount = BigDecimal.ZERO,
                krwPerUnit = BigDecimal.ZERO,
                rateDate = rateDate,
            )
        )

        assertEquals(
            setOf(
                BudgetValidationError.InvalidCurrencyCode,
                BudgetValidationError.InvalidAmount,
                BudgetValidationError.InvalidExchangeRate,
            ),
            errors,
        )
    }

    @Test
    fun validate_krwRequiresUnitRate() {
        val errors = BudgetCalculator.validate(
            CreateTripBudgetInput(
                currencyCode = "KRW",
                krwAmount = BigDecimal("1000000"),
                krwPerUnit = BigDecimal("1.01"),
                rateDate = rateDate,
            )
        )

        assertTrue(BudgetValidationError.InvalidKrwExchangeRate in errors)
        assertTrue(
            BudgetCalculator.validate(
                AddBudgetEntryInput(BigDecimal.ONE, BigDecimal.ONE, rateDate)
            ).isEmpty()
        )
    }
}
