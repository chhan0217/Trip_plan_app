package com.example.tripplan.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tripplan.domain.model.CountryCurrencyCatalog
import com.example.tripplan.domain.model.TravelCountry

@Composable
fun CountrySelector(
    countryCode: String,
    currencyCode: String,
    modifier: Modifier = Modifier,
    onCountrySelected: (TravelCountry) -> Unit,
) {
    var isDialogVisible by rememberSaveable { mutableStateOf(false) }
    val selected = CountryCurrencyCatalog.find(countryCode) ?: CountryCurrencyCatalog.default
    OutlinedButton(
        onClick = { isDialogVisible = true },
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("여행 국가", style = MaterialTheme.typography.labelSmall)
                Text(selected.countryName)
            }
            Text("$currencyCode · ${selected.currencyName}")
        }
    }
    if (isDialogVisible) {
        CountrySelectionDialog(
            onDismiss = { isDialogVisible = false },
            onSelect = {
                onCountrySelected(it)
                isDialogVisible = false
            },
        )
    }
}

@Composable
private fun CountrySelectionDialog(
    onDismiss: () -> Unit,
    onSelect: (TravelCountry) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val countries = CountryCurrencyCatalog.countries.filter {
        query.isBlank() || it.countryName.contains(query.trim(), ignoreCase = true) ||
            it.countryCode.contains(query.trim(), ignoreCase = true) ||
            it.currencyCode.contains(query.trim(), ignoreCase = true)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("여행 국가 선택") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("국가 또는 통화 검색") },
                    singleLine = true,
                )
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(countries, key = { it.countryCode }) { country ->
                        TextButton(
                            onClick = { onSelect(country) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(country.countryName)
                                Text(country.currencyCode)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}
