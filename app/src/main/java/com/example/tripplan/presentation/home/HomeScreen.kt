package com.example.tripplan.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tripplan.R

data class HomeTripUiModel(
    val id: Long,
    val title: String,
    val dateText: String,
    val placeText: String,
)

private object HomeDummyTrips {
    val ongoing = listOf(
        HomeTripUiModel(
            id = 1L,
            title = "제주 여름 여행",
            dateText = "2026.07.21 - 2026.07.25",
            placeText = "제주"
        )
    )

    val upcoming = listOf(
        HomeTripUiModel(
            id = 2L,
            title = "부산 주말 여행",
            dateText = "2026.08.14 - 2026.08.16",
            placeText = "부산"
        ),
        HomeTripUiModel(
            id = 3L,
            title = "도쿄 가을 여행",
            dateText = "2026.10.03 - 2026.10.07",
            placeText = "도쿄"
        )
    )
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    ongoingTrips: List<HomeTripUiModel> = HomeDummyTrips.ongoing,
    upcomingTrips: List<HomeTripUiModel> = HomeDummyTrips.upcoming,
    onAddTripClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        AddTripCard(onClick = onAddTripClick)

        SectionDivider()

        if (ongoingTrips.isNotEmpty()) {
            TripSection(
                title = "현재 여행중",
                trips = ongoingTrips,
                emptyMessage = null
            )
            SectionDivider()
        }

        TripSection(
            title = "다가오는 여행",
            trips = upcomingTrips,
            emptyMessage = "예정된 여행이 없습니다."
        )
    }
}

@Composable
private fun AddTripCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AddTripIcon()
            Text(
                text = "새 여행 추가",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "날짜별 일정, 장소, 메모를 담을 여행 계획을 시작합니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddTripIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_add_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun SectionDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun TripSection(
    title: String,
    trips: List<HomeTripUiModel>,
    emptyMessage: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )

        if (trips.isEmpty()) {
            if (emptyMessage != null) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                trips.forEach { trip ->
                    TripCard(trip = trip)
                }
            }
        }
    }
}

@Composable
private fun TripCard(
    trip: HomeTripUiModel,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = trip.title,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = trip.dateText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = trip.placeText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenWithoutOngoingTripPreview() {
    MaterialTheme {
        HomeScreen(
            ongoingTrips = emptyList(),
            upcomingTrips = HomeDummyTrips.upcoming
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenWithOngoingTripPreview() {
    MaterialTheme {
        HomeScreen()
    }
}
