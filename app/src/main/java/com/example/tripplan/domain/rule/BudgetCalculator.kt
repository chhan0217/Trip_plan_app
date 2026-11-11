package com.example.tripplan.domain.rule

import com.example.tripplan.domain.model.AddBudgetEntryInput
import com.example.tripplan.domain.model.CreateTripBudgetInput
import java.math.BigDecimal
import java.math.RoundingMode

enum class BudgetValidationError {
    InvalidCurrencyCode,
    InvalidAmount,
    InvalidExchangeRate,
    InvalidKrwExchangeRate,
}

object BudgetCalculator {
    private val CurrencyCodePattern = Regex("^[A-Z]{3}$")

    fun validate(input: CreateTripBudgetInput): Set<BudgetValidationError> = buildSet {
        if (!CurrencyCodePattern.matches(input.currencyCode)) add(BudgetValidationError.InvalidCurrencyCode)
        addAmountAndRateErrors(input.krwAmount, input.krwPerUnit)
        if (input.currencyCode == "KRW" && input.krwPerUnit.compareTo(BigDecimal.ONE) != 0) {
            add(BudgetValidationError.InvalidKrwExchangeRate)
        }
    }

    fun validate(input: AddBudgetEntryInput): Set<BudgetValidationError> = buildSet {
        addAmountAndRateErrors(input.krwAmount, input.krwPerUnit)
    }

    fun convertToKrw(amount: BigDecimal, krwPerUnit: BigDecimal): BigDecimal =
        amount.multiply(krwPerUnit).setScale(0, RoundingMode.HALF_UP)

    fun convertFromKrw(krwAmount: BigDecimal, krwPerUnit: BigDecimal): BigDecimal =
        krwAmount.divide(krwPerUnit, 2, RoundingMode.HALF_UP)

    private fun MutableSet<BudgetValidationError>.addAmountAndRateErrors(
        amount: BigDecimal,
        krwPerUnit: BigDecimal,
    ) {
        if (amount <= BigDecimal.ZERO) add(BudgetValidationError.InvalidAmount)
        if (krwPerUnit <= BigDecimal.ZERO) add(BudgetValidationError.InvalidExchangeRate)
    }
}
