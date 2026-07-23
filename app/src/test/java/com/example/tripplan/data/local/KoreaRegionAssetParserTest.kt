package com.example.tripplan.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KoreaRegionAssetParserTest {
    @Test
    fun parseVersion_readsVersionMarker() {
        val result = KoreaRegionAssetParser.parseVersion("# korea_version=2026-07-23-1")

        assertEquals("2026-07-23-1", result)
    }

    @Test
    fun parseRegion_skipsVersionLine() {
        val result = KoreaRegionAssetParser.parseRegion("# korea_version=2026-07-23-1")

        assertNull(result)
    }

    @Test
    fun parseRegion_mapsColumnsExceptCountryPostalCodeAndAccuracy() {
        val row = "KR\t63002\t한경면\t제주특별자치도\t01\t제주시\t39010\t한경면\t3901031\t33.3453\t126.1878\t6"

        val result = requireNotNull(KoreaRegionAssetParser.parseRegion(row))

        assertEquals("한경면", result.placeName)
        assertEquals("제주특별자치도", result.provinceName)
        assertEquals("01", result.provinceCode)
        assertEquals("제주시", result.cityName)
        assertEquals("39010", result.cityCode)
        assertEquals("한경면", result.districtName)
        assertEquals("3901031", result.districtCode)
        assertEquals(33.3453, result.latitude, 0.0)
        assertEquals(126.1878, result.longitude, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parseRegion_rejectsUnexpectedColumnCount() {
        KoreaRegionAssetParser.parseRegion("KR\t63002\t한경면")
    }
}
