package com.example.tripplan.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CountryCurrencyCatalogTest {
    @Test
    fun commonCountries_mapToOfflineCurrencies() {
        assertEquals("KRW", CountryCurrencyCatalog.find("KR")?.currencyCode)
        assertEquals("JPY", CountryCurrencyCatalog.find("JP")?.currencyCode)
        assertEquals("USD", CountryCurrencyCatalog.find("US")?.currencyCode)
        assertEquals("VND", CountryCurrencyCatalog.find("VN")?.currencyCode)
        assertEquals("EUR", CountryCurrencyCatalog.find("FR")?.currencyCode)
    }

    @Test
    fun catalog_containsKoreanCountryNamesAndUniqueCodes() {
        assertEquals("대한민국", CountryCurrencyCatalog.default.countryName)
        assertTrue(CountryCurrencyCatalog.countries.size > 100)
        assertEquals(
            CountryCurrencyCatalog.countries.size,
            CountryCurrencyCatalog.countries.map { it.countryCode }.distinct().size,
        )
    }
}
