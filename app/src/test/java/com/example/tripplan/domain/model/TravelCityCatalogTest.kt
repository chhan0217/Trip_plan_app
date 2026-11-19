package com.example.tripplan.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TravelCityCatalogTest {
    @Test
    fun thailand_returnsRepresentativeTravelCities() {
        val cities = TravelCityCatalog.cities("TH")

        assertTrue("방콕" in cities)
        assertTrue("파타야" in cities)
        assertTrue("코사무이" in cities)
    }

    @Test
    fun countryCode_isCaseInsensitiveAndUnknownCountryIsEmpty() {
        assertEquals(TravelCityCatalog.cities("JP"), TravelCityCatalog.cities("jp"))
        assertTrue(TravelCityCatalog.cities("XX").isEmpty())
    }
}
