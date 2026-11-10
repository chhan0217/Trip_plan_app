package com.example.tripplan.presentation.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tripplan.R

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onAddTripClick: () -> Unit = {},
    onTripClick: (Long) -> Unit = {},
    onImportFileSelected: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.toString()?.let(onImportFileSelected)
    }
    HomeScreenContent(
        uiState = uiState,
        modifier = modifier,
        onAddTripClick = onAddTripClick,
        onTripClick = onTripClick,
        onImportClick = { importLauncher.launch(arrayOf("application/json", "text/json")) },
        onSettingsClick = onSettingsClick,
        onExchangeRateSelectClick = viewModel::showCurrencySelector,
        onExchangeRateRefreshClick = viewModel::refreshExchangeRates,
        onCurrencySelectionChanged = viewModel::setCurrencySelected,
        onCurrencySelectorDismiss = viewModel::hideCurrencySelector,
    )
}

@Composable
internal fun HomeScreenContent(
    uiState: HomeUiState,
    modifier: Modifier = Modifier,
    onAddTripClick: () -> Unit = {},
    onTripClick: (Long) -> Unit = {},
    onImportClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onExchangeRateSelectClick: () -> Unit = {},
    onExchangeRateRefreshClick: () -> Unit = {},
    onCurrencySelectionChanged: (String, Boolean) -> Unit = { _, _ -> },
    onCurrencySelectorDismiss: () -> Unit = {},
) {
    if (uiState.isCurrencySelectorVisible) {
        CurrencySelectorDialog(
            currencies = uiState.availableCurrencies,
            selectedCurrencyUnits = uiState.selectedCurrencyUnits,
            onSelectionChanged = onCurrencySelectionChanged,
            onDismiss = onCurrencySelectorDismiss,
        )
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column {
                TextButton(onClick = onSettingsClick, modifier = Modifier.align(Alignment.End)) { Text("설정") }
                AddTripCard(onClick = onAddTripClick, onImportClick = onImportClick)
            }
        }
        item {
            ExchangeRateCard(
                uiState = uiState,
                onSelectClick = onExchangeRateSelectClick,
                onRefreshClick = onExchangeRateRefreshClick,
            )
        }
        item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }

        when {
            uiState.isLoading -> item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.isLoadFailed -> item {
                Text(
                    text = "여행 목록을 불러오지 못했습니다. 앱을 다시 실행해 주세요.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            else -> {
                val trips = uiState.trips.ifEmpty {
                    uiState.ongoingTrips + uiState.upcomingTrips + uiState.pastTrips
                }
                tripSection(
                    title = "최근 여행",
                    trips = trips,
                    onTripClick = onTripClick,
                    emptyMessage = "등록된 여행이 없습니다.",
                )
            }
        }
    }
}

@Composable
private fun ExchangeRateCard(
    uiState: HomeUiState,
    onSelectClick: () -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("오늘의 환율", style = MaterialTheme.typography.titleMedium)
                    if (uiState.exchangeRateDateText.isNotBlank()) {
                        Text(
                            uiState.exchangeRateDateText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TextButton(onClick = onSelectClick) { Text("통화 선택") }
            }

            if (uiState.exchangeRates.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.isExchangeRateRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        if (uiState.isExchangeRateRefreshing) "최신 환율을 확인하고 있습니다."
                        else "표시할 환율 정보가 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                uiState.exchangeRates.forEach { rate ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            rate.currencyName,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "${rate.displayUnit} ${rate.currencyCode} = ${rate.displayRateText}원",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            uiState.exchangeRateMessage?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = onRefreshClick,
                enabled = !uiState.isExchangeRateRefreshing,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(if (uiState.isExchangeRateRefreshing) "갱신 중" else "새로고침")
            }
        }
    }
}

@Composable
private fun CurrencySelectorDialog(
    currencies: List<HomeExchangeRateUiModel>,
    selectedCurrencyUnits: Set<String>,
    onSelectionChanged: (String, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("메인에 표시할 통화") },
        text = {
            if (currencies.isEmpty()) {
                Text("환율을 먼저 불러온 뒤 통화를 선택할 수 있습니다.")
            } else {
                LazyColumn {
                    items(currencies, key = { it.currencyCode }) { currency ->
                        val selected = currency.currencyCode in selectedCurrencyUnits
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (selectedCurrencyUnits.size > 1 || !selected) {
                                    onSelectionChanged(currency.currencyCode, !selected)
                                }
                            }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = selected,
                                onCheckedChange = { checked ->
                                    if (selectedCurrencyUnits.size > 1 || checked) {
                                        onSelectionChanged(currency.currencyCode, checked)
                                    }
                                },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${currency.currencyName} (${currency.currencyCode})")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("완료") }
        },
    )
}

private fun LazyListScope.tripSection(
    title: String,
    trips: List<HomeTripUiModel>,
    onTripClick: (Long) -> Unit,
    emptyMessage: String? = null,
) {
    item { Text(text = title, style = MaterialTheme.typography.titleMedium) }
    if (trips.isEmpty()) {
        if (emptyMessage != null) {
            item {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        items(trips, key = { it.id }) { trip ->
            TripCard(trip = trip, onClick = { onTripClick(trip.id) })
        }
    }
}

@Composable
private fun AddTripCard(
    onClick: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_add_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text("새 여행 추가", style = MaterialTheme.typography.titleMedium)
            Text(
                "날짜별 일정, 장소, 메모를 담을 여행 계획을 시작합니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = onImportClick,
                modifier = Modifier.align(Alignment.End),
            ) { Text("여행 파일 가져오기") }
        }
    }
}

@Composable
private fun TripCard(
    trip: HomeTripUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(trip.title, style = MaterialTheme.typography.titleSmall)
            if (trip.statusText.isNotBlank()) {
                Text(
                    trip.statusText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                trip.dateText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (trip.placeText.isNotBlank()) {
                Text(
                    trip.placeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val previewTrip = HomeTripUiModel(
    id = 1L,
    title = "제주 여름 여행",
    dateText = "2026.07.21 - 2026.07.25",
    placeText = "제주",
)

private val previewExchangeRates = listOf(
    HomeExchangeRateUiModel("USD", "미국 달러", 1, "1,466.8"),
    HomeExchangeRateUiModel("JPY", "일본 엔", 100, "952.3"),
    HomeExchangeRateUiModel("EUR", "유로", 1, "1,692.4"),
    HomeExchangeRateUiModel("VND", "베트남 동", 1_000, "52.7"),
)

@Preview(showBackground = true)
@Composable
private fun HomeScreenEmptyPreview() {
    MaterialTheme { HomeScreenContent(uiState = HomeUiState(isLoading = false)) }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenWithTripsPreview() {
    MaterialTheme {
        HomeScreenContent(
            uiState = HomeUiState(
                isLoading = false,
                ongoingTrips = listOf(previewTrip),
                upcomingTrips = listOf(previewTrip.copy(id = 2L, title = "부산 주말 여행")),
                pastTrips = listOf(previewTrip.copy(id = 3L, title = "지난 서울 여행")),
                exchangeRates = previewExchangeRates,
                availableCurrencies = previewExchangeRates,
                selectedCurrencyUnits = previewExchangeRates.mapTo(linkedSetOf()) { it.currencyCode },
                exchangeRateDateText = "2026.11.10 기준",
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeCurrencySelectorPreview() {
    MaterialTheme {
        HomeScreenContent(
            uiState = HomeUiState(
                isLoading = false,
                exchangeRates = previewExchangeRates,
                availableCurrencies = previewExchangeRates,
                selectedCurrencyUnits = setOf("USD", "EUR"),
                exchangeRateDateText = "2026.11.10 기준",
                isCurrencySelectorVisible = true,
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenErrorPreview() {
    MaterialTheme {
        HomeScreenContent(uiState = HomeUiState(isLoading = false, isLoadFailed = true))
    }
}
