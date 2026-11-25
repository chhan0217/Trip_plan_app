package com.example.tripplan.presentation.itinerarymap

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tripplan.BuildConfig
import com.example.tripplan.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun ItineraryMapScreen(
    tripPlanId: Long,
    onBackClick: () -> Unit,
    onEditItinerary: ((Long) -> Unit)?,
    viewModel: ItineraryMapViewModel = hiltViewModel(key = "itinerary-map-$tripPlanId"),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel, tripPlanId) { viewModel.load(tripPlanId) }
    ItineraryMapScreenContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onDaySelected = viewModel::selectDay,
        onItemSelected = viewModel::selectItem,
        onEditItinerary = onEditItinerary,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ItineraryMapScreenContent(
    uiState: ItineraryMapUiState,
    onBackClick: () -> Unit = {},
    onDaySelected: (LocalDate?) -> Unit = {},
    onItemSelected: (Long?) -> Unit = {},
    onEditItinerary: ((Long) -> Unit)? = null,
    mapConfigured: Boolean = BuildConfig.MAPS_API_KEY.isNotBlank() &&
        BuildConfig.MAPS_API_KEY != "DEFAULT_API_KEY",
) {
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = ImageVectorBack,
                        contentDescription = "뒤로",
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("일정 지도", style = MaterialTheme.typography.titleLarge)
                    if (uiState.tripTitle.isNotBlank()) {
                        Text(uiState.tripTitle, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            when {
                uiState.isLoading -> CenterMessage { CircularProgressIndicator() }
                uiState.isNotFound -> CenterMessage { Text("여행을 찾을 수 없습니다.") }
                uiState.isLoadFailed -> CenterMessage { Text("일정 지도를 불러오지 못했습니다.") }
                else -> {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            FilterChip(
                                selected = uiState.selectedDate == null,
                                onClick = { onDaySelected(null) },
                                label = { Text("전체") },
                            )
                        }
                        items(uiState.days, key = { it.date.toString() }) { day ->
                            FilterChip(
                                selected = uiState.selectedDate == day.date,
                                onClick = { onDaySelected(day.date) },
                                label = { Text("${day.dayNumber}일차") },
                            )
                        }
                    }
                    BottomSheetScaffold(
                        modifier = Modifier.fillMaxSize(),
                        sheetPeekHeight = 164.dp,
                        sheetContent = {
                            ScheduleSheet(
                                uiState = uiState,
                                onItemSelected = onItemSelected,
                                onEditItinerary = onEditItinerary,
                            )
                        },
                    ) { sheetPadding ->
                        Box(Modifier.fillMaxSize().padding(bottom = sheetPadding.calculateBottomPadding())) {
                            when {
                                !mapConfigured -> MapMessage("MAPS_API_KEY를 설정하면 지도가 표시됩니다.")
                                uiState.markers.isEmpty() -> MapMessage("좌표가 저장된 일정이 없습니다.\n장소 검색에서 위치를 선택해 주세요.")
                                else -> PlannedGoogleMap(uiState, onItemSelected)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlannedGoogleMap(
    uiState: ItineraryMapUiState,
    onItemSelected: (Long?) -> Unit,
) {
    val cameraState = rememberCameraPositionState()
    var mapLoaded by remember { mutableStateOf(false) }
    val selected = uiState.selectedItem?.takeIf { it.hasCoordinates }
    LaunchedEffect(mapLoaded, uiState.selectedDate, uiState.markers, selected?.id) {
        if (!mapLoaded || uiState.markers.isEmpty()) return@LaunchedEffect
        if (selected != null) {
            cameraState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(selected.latitude!!, selected.longitude!!), 15.5f),
                450,
            )
        } else if (uiState.markers.size == 1) {
            val marker = uiState.markers.single()
            cameraState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(marker.latitude!!, marker.longitude!!), 14f),
                450,
            )
        } else {
            val bounds = LatLngBounds.builder().apply {
                uiState.markers.forEach { include(LatLng(it.latitude!!, it.longitude!!)) }
            }.build()
            cameraState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 120), 450)
        }
    }
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraState,
        uiSettings = MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = false),
        onMapLoaded = { mapLoaded = true },
        onMapClick = { onItemSelected(null) },
    ) {
        uiState.lineSegments.forEachIndexed { index, segment ->
            Polyline(
                points = segment.map { LatLng(it.latitude!!, it.longitude!!) },
                color = dayColor(segment.first().dayNumber),
                width = 9f,
                zIndex = index.toFloat(),
            )
        }
        uiState.markers.forEach { item ->
            val selectedMarker = item.id == uiState.selectedItemId
            val markerPosition = LatLng(item.latitude!!, item.longitude!!)
            val markerState = remember(item.id) { MarkerState(markerPosition) }.also {
                it.position = markerPosition
            }
            Marker(
                state = markerState,
                title = item.title,
                snippet = item.placeName,
                icon = numberedMarkerIcon(item.markerLabel, dayColor(item.dayNumber), selectedMarker),
                zIndex = if (selectedMarker) 10f else 1f,
                onClick = {
                    onItemSelected(item.id)
                    true
                },
            )
        }
    }
}

@Composable
private fun numberedMarkerIcon(label: String, color: Color, selected: Boolean): BitmapDescriptor {
    val density = LocalDensity.current.density
    val fill = if (selected) MaterialTheme.colorScheme.tertiary else color
    return remember(label, fill, density, selected) {
        val width = (56 * density).toInt()
        val height = (68 * density).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = fill.toArgb() }
        val cx = width / 2f
        val radius = width * .38f
        canvas.drawCircle(cx, radius + 2 * density, radius, paint)
        val path = android.graphics.Path().apply {
            moveTo(cx - radius * .45f, radius * 1.55f)
            lineTo(cx, height.toFloat())
            lineTo(cx + radius * .45f, radius * 1.55f)
            close()
        }
        canvas.drawPath(path, paint)
        paint.apply {
            this.color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = (if (label.length > 2) 13 else 17) * density
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val baseline = radius + 2 * density - (paint.ascent() + paint.descent()) / 2
        canvas.drawText(label, cx, baseline, paint)
        BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}

@Composable
private fun ScheduleSheet(
    uiState: ItineraryMapUiState,
    onItemSelected: (Long?) -> Unit,
    onEditItinerary: ((Long) -> Unit)?,
) {
    Column(Modifier.fillMaxWidth().height(420.dp).padding(top = 8.dp)) {
        Text(
            text = if (uiState.selectedDate == null) "전체 일정" else "선택한 일차 일정",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        if (uiState.items.isEmpty()) {
            Text("등록된 일정이 없습니다.", Modifier.padding(20.dp))
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.items, key = { it.id }) { item ->
                    ScheduleMapCard(
                        item = item,
                        selected = item.id == uiState.selectedItemId,
                        onClick = { onItemSelected(if (item.id == uiState.selectedItemId) null else item.id) },
                        onEdit = onEditItinerary?.let { edit -> { edit(item.id) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleMapCard(item: MapItemUiModel, selected: Boolean, onClick: () -> Unit, onEdit: (() -> Unit)?) {
    val context = LocalContext.current
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.markerLabel, color = dayColor(item.dayNumber), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.size(10.dp))
                Text(item.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                item.startTime?.let { Text(it.format(timeFormatter), style = MaterialTheme.typography.labelMedium) }
            }
            Text(
                if (item.hasCoordinates) item.placeName.ifBlank { "장소 이름 없음" } else "위치 미지정 · ${item.placeName}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.hasCoordinates) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            )
            if (selected) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (item.hasCoordinates) {
                        TextButton(onClick = {
                            val uri = Uri.parse(item.googleMapsUrl())
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }) { Text("Google 지도") }
                    }
                    if (onEdit != null) TextButton(onClick = onEdit) { Text("일정 보기") }
                }
            }
        }
    }
}

internal fun MapItemUiModel.googleMapsUrl(): String {
    val query = address.ifBlank { placeName }
    return if (providerName == GOOGLE_PLACES_PROVIDER && !externalId.isNullOrBlank()) {
        "https://www.google.com/maps/search/?api=1&query=${query.urlEncoded()}" +
            "&query_place_id=${externalId.urlEncoded()}"
    } else {
        "https://www.google.com/maps/search/?api=1&query=${latitude},${longitude}"
    }
}

private fun String.urlEncoded(): String =
    URLEncoder.encode(this, StandardCharsets.UTF_8.name()).replace("+", "%20")

@Composable private fun CenterMessage(content: @Composable () -> Unit) =
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }

@Composable private fun MapMessage(message: String) =
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }

private fun dayColor(dayNumber: Int): Color = listOf(
    Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFFC62828),
    Color(0xFF6A1B9A), Color(0xFFEF6C00), Color(0xFF00838F),
)[(dayNumber - 1).mod(6)]

private val ImageVectorBack @Composable get() = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(R.drawable.ic_arrow_back_24)
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private const val GOOGLE_PLACES_PROVIDER = "Google Places"

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun ItineraryMapScreenPreview() {
    MaterialTheme {
        ItineraryMapScreenContent(
            uiState = ItineraryMapUiState(
                tripTitle = "방콕 여행",
                days = listOf(MapDayUiModel(LocalDate.of(2026, 12, 1), 1, "12-01")),
                selectedDate = LocalDate.of(2026, 12, 1),
                items = listOf(
                    MapItemUiModel(1, LocalDate.of(2026, 12, 1), 1, "1", "왕궁", "Grand Palace", "Bangkok", LocalTime.of(10, 0), 13.7500, 100.4913, "Google Places", "place-id"),
                    MapItemUiModel(2, LocalDate.of(2026, 12, 1), 1, "2", "점심", "쏨분 씨푸드", "", LocalTime.of(12, 30), null, null),
                ),
            ),
            mapConfigured = false,
        )
    }
}
