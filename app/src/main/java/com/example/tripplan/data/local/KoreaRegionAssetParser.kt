package com.example.tripplan.data.local

object KoreaRegionAssetParser {
    private const val VERSION_PREFIX = "# korea_version="
    private const val COLUMN_COUNT = 12

    fun parseVersion(line: String): String? {
        return line
            .takeIf { it.startsWith(VERSION_PREFIX) }
            ?.removePrefix(VERSION_PREFIX)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    fun parseRegion(line: String): RegionEntity? {
        if (line.isBlank() || line.startsWith("#")) return null

        val columns = line.split('\t')
        require(columns.size == COLUMN_COUNT) {
            "Korea region row must have $COLUMN_COUNT columns, but was ${columns.size}."
        }

        val placeName = columns[2]
        val provinceName = columns[3]
        val provinceCode = columns[4]
        val cityName = columns[5]
        val cityCode = columns[6]
        val districtName = columns[7]
        val districtCode = columns[8]
        val latitudeText = columns[9]
        val longitudeText = columns[10]

        return RegionEntity(
            id = buildId(
                placeName = placeName,
                provinceName = provinceName,
                provinceCode = provinceCode,
                cityName = cityName,
                cityCode = cityCode,
                districtName = districtName,
                districtCode = districtCode,
                latitudeText = latitudeText,
                longitudeText = longitudeText,
            ),
            placeName = placeName,
            provinceName = provinceName,
            provinceCode = provinceCode,
            cityName = cityName,
            cityCode = cityCode,
            districtName = districtName,
            districtCode = districtCode,
            latitude = latitudeText.toDouble(),
            longitude = longitudeText.toDouble(),
        )
    }

    private fun buildId(
        placeName: String,
        provinceName: String,
        provinceCode: String,
        cityName: String,
        cityCode: String,
        districtName: String,
        districtCode: String,
        latitudeText: String,
        longitudeText: String,
    ): String {
        return listOf(
            placeName,
            provinceName,
            provinceCode,
            cityName,
            cityCode,
            districtName,
            districtCode,
            latitudeText,
            longitudeText,
        ).joinToString("|")
    }
}
