package com.example.tripplan.presentation.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tripplan.domain.model.TravelCityCatalog

@Composable
fun TravelCitySuggestions(
    countryCode: String,
    onCitySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cities = TravelCityCatalog.cities(countryCode)
    if (cities.isEmpty()) return

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "추천 도시",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            cities.forEach { city ->
                OutlinedButton(onClick = { onCitySelected(city) }) {
                    Text(city)
                }
            }
        }
    }
}
