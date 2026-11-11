package com.example.tripplan.domain.model

import java.util.Currency
import java.util.Locale

data class TravelCountry(
    val countryCode: String,
    val countryName: String,
    val currencyCode: String,
    val currencyName: String,
)

object CountryCurrencyCatalog {
    val countries: List<TravelCountry> by lazy {
        Locale.getISOCountries().mapNotNull { countryCode ->
            val locale = Locale.Builder().setRegion(countryCode).build()
            val currency = runCatching { Currency.getInstance(locale) }.getOrNull()
                ?: return@mapNotNull null
            val countryName = locale.getDisplayCountry(Locale.KOREAN)
            if (countryName.isBlank()) return@mapNotNull null
            TravelCountry(
                countryCode = countryCode,
                countryName = countryName,
                currencyCode = currency.currencyCode,
                currencyName = currency.getDisplayName(Locale.KOREAN),
            )
        }.sortedBy { it.countryName }
    }

    fun find(countryCode: String): TravelCountry? =
        countries.firstOrNull { it.countryCode == countryCode.uppercase() }

    val default: TravelCountry
        get() = checkNotNull(find("KR"))
}
