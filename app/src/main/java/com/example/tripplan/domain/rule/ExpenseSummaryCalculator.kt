package com.example.tripplan.domain.rule

import com.example.tripplan.domain.model.Expense
import com.example.tripplan.domain.model.ExpenseCategory
import java.math.BigDecimal

data class ExpenseCategoryTotal(
    val category: ExpenseCategory,
    val krwAmount: BigDecimal,
    val localAmount: BigDecimal,
)

object ExpenseSummaryCalculator {
    fun calculate(
        expenses: List<Expense>,
        tripCurrencyCode: String,
        fallbackKrwPerUnit: BigDecimal?,
    ): List<ExpenseCategoryTotal> = expenses
        .groupBy { it.category }
        .map { (category, categoryExpenses) ->
            ExpenseCategoryTotal(
                category = category,
                krwAmount = categoryExpenses.sumOf { it.convertedKrwAmount },
                localAmount = categoryExpenses.sumOf { expense ->
                    when {
                        expense.currencyCode == tripCurrencyCode -> expense.amount
                        expense.currencyCode == "KRW" ->
                            BudgetCalculator.convertFromKrw(
                                expense.convertedKrwAmount,
                                expense.krwPerUnit,
                            )
                        fallbackKrwPerUnit != null ->
                            BudgetCalculator.convertFromKrw(
                                expense.convertedKrwAmount,
                                fallbackKrwPerUnit,
                            )
                        else -> BigDecimal.ZERO
                    }
                },
            )
        }
        .sortedByDescending { it.krwAmount }
}
