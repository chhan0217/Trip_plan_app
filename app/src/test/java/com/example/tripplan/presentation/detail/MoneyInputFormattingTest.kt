package com.example.tripplan.presentation.detail

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyInputFormattingTest {
    @Test
    fun formatMoneyInput_addsGroupingAndKeepsDecimalInput() {
        assertEquals("3,800", "3800".formatMoneyInput())
        assertEquals("1,234,567", "1,234567".formatMoneyInput())
        assertEquals("10,500.", "10500.".formatMoneyInput())
        assertEquals("10,500.25", "10500.25".formatMoneyInput())
    }

    @Test
    fun formatMoneyInput_removesNonMoneyCharacters() {
        assertEquals("12,345", "12원345abc".formatMoneyInput())
        assertEquals("", "원".formatMoneyInput())
    }
}
