package com.example.tripplan.presentation.lodging

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tripplan.presentation.itinerary.ScheduleTimePickerDialog
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("MM.dd")

@Composable
internal fun LodgingSection(
    lodgings: List<LodgingUiModel>,
    modifier: Modifier = Modifier,
    onAdd: (() -> Unit)? = null,
    onEdit: ((LodgingUiModel) -> Unit)? = null,
) {
    if (lodgings.isEmpty() && onAdd == null) return
    OutlinedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("숙소", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                if (onAdd != null) TextButton(onClick = onAdd) { Text("숙소 추가") }
            }
            lodgings.forEach { lodging ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(lodging.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${lodging.checkIn.format(dateFormat)}–${lodging.checkOut.format(dateFormat)} · ${lodging.nights}박",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    LodgingMapButton(lodging, label = "지도")
                    if (onEdit != null) TextButton(onClick = { onEdit(lodging) }) { Text("수정") }
                }
            }
        }
    }
}

@Composable
internal fun LodgingDayInfo(lodgings: List<LodgingUiModel>, date: LocalDate?, modifier: Modifier = Modifier) {
    if (date == null) return
    val departing = lodgings.firstOrNull { it.checkOut == date }
    val arriving = lodgings.firstOrNull { it.checkIn == date }
    val staying = lodgings.firstOrNull { date > it.checkIn && date < it.checkOut }
    val message = when {
        departing != null && arriving != null -> "숙소 이동 · ${departing.name} 체크아웃 → ${arriving.name} 체크인"
        arriving != null -> "체크인 · ${arriving.name}"
        departing != null -> "체크아웃 · ${departing.name}"
        staying != null -> "오늘 숙소 · ${staying.name}"
        else -> return
    }
    var expanded by rememberSaveable(date.toString()) { mutableStateOf(false) }
    OutlinedCard(onClick = { expanded = !expanded }, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            if (expanded) {
                listOfNotNull(departing, arriving, staying).forEach { lodging ->
                    Text(lodging.name, style = MaterialTheme.typography.labelLarge)
                    if (lodging.address.isNotBlank()) Text(lodging.address)
                    lodging.checkInTime?.let { Text("체크인 시간 ${it.format(timeFormat)}") }
                    lodging.checkOutTime?.let { Text("체크아웃 시간 ${it.format(timeFormat)}") }
                    if (lodging.note.isNotBlank()) Text(lodging.note)
                    LodgingMapButton(lodging, label = "Google 지도에서 보기")
                }
            }
        }
    }
}

@Composable
private fun LodgingMapButton(lodging: LodgingUiModel, label: String) {
    val context = LocalContext.current
    var showError by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, lodging.googleMapsUri()))
        } catch (_: ActivityNotFoundException) {
            showError = true
        }
    }) { Text(label) }
    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            text = { Text("Google 지도를 열 수 없습니다.") },
            confirmButton = { TextButton(onClick = { showError = false }) { Text("확인") } },
        )
    }
}

internal fun LodgingUiModel.googleMapsUri(): Uri {
    val textQuery = listOf(name, address).filter(String::isNotBlank).joinToString(" ")
    val query = if (googlePlaceId == null && latitude != null && longitude != null) {
        "$latitude,$longitude"
    } else {
        textQuery
    }
    return Uri.parse("https://www.google.com/maps/search/").buildUpon()
        .appendQueryParameter("api", "1")
        .appendQueryParameter("query", query)
        .apply { googlePlaceId?.let { appendQueryParameter("query_place_id", it) } }
        .build()
}

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

@Composable
internal fun LodgingEditor(
    existing: LodgingUiModel?,
    dates: List<LocalDate>,
    state: LodgingUiState,
    onDismiss: () -> Unit,
    onSave: (LodgingUiModel, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onSearch: (String) -> Unit = {},
    onClearSearch: () -> Unit = {},
) {
    if (dates.isEmpty()) return
    val first = dates.first()
    val last = dates.last()
    val next = state.lodgings.lastOrNull()?.checkOut?.takeIf { it >= first && it < last } ?: first
    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var address by rememberSaveable { mutableStateOf(existing?.address.orEmpty()) }
    var note by rememberSaveable { mutableStateOf(existing?.note.orEmpty()) }
    var checkInText by rememberSaveable { mutableStateOf((existing?.checkIn ?: next).toString()) }
    var checkOutText by rememberSaveable { mutableStateOf((existing?.checkOut ?: last).toString()) }
    var checkInTime by rememberSaveable { mutableStateOf(existing?.checkInTime?.format(timeFormat)) }
    var checkOutTime by rememberSaveable { mutableStateOf(existing?.checkOutTime?.format(timeFormat)) }
    var latitude by rememberSaveable { mutableStateOf(existing?.latitude) }
    var longitude by rememberSaveable { mutableStateOf(existing?.longitude) }
    var googlePlaceId by rememberSaveable { mutableStateOf(existing?.googlePlaceId) }
    var resolvePlace by rememberSaveable { mutableStateOf(false) }
    fun clearLocation() {
        latitude = null
        longitude = null
        googlePlaceId = null
        resolvePlace = false
        onClearSearch()
    }
    fun selectPlace(place: LodgingPlaceUiModel, resolve: Boolean) {
        name = place.name
        address = place.address
        latitude = place.latitude
        longitude = place.longitude
        googlePlaceId = place.googlePlaceId
        resolvePlace = resolve
        onClearSearch()
    }
    var dateTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var timeTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var choosePlace by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val checkIn = LocalDate.parse(checkInText)
    val checkOut = LocalDate.parse(checkOutText)

    AlertDialog(
        onDismissRequest = { if (!state.isSaving) onDismiss() },
        title = { Text(if (existing == null) "숙소 추가" else "숙소 수정") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            clearLocation()
                        },
                        label = { Text("숙소 이름") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                item {
                    Row {
                        TextButton(
                            onClick = { onSearch(name) },
                            enabled = name.trim().length >= 2 && !state.isSearching && !state.isSaving,
                        ) { Text("지도 검색") }
                        if (state.savedPlaces.isNotEmpty()) {
                            TextButton(onClick = { choosePlace = true }) { Text("저장한 장소 선택") }
                        }
                    }
                }
                if (state.isSearching) item { Text("숙소를 검색하는 중입니다.") }
                if (state.searchCompleted && state.suggestions.isEmpty()) {
                    item { Text("검색 결과가 없습니다. 숙소 이름과 주소를 직접 입력할 수 있어요.") }
                }
                items(state.suggestions) { place ->
                    TextButton(onClick = { selectPlace(place, resolve = true) }) {
                        Text(listOf(place.name, place.address).filter(String::isNotBlank).joinToString(" · "))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { dateTarget = "체크인" }, modifier = Modifier.weight(1f)) {
                            Text("체크인 ${checkIn.format(dateFormat)}")
                        }
                        TextButton(onClick = { dateTarget = "체크아웃" }, modifier = Modifier.weight(1f)) {
                            Text("체크아웃 ${checkOut.format(dateFormat)}")
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = {
                            address = it
                            clearLocation()
                        },
                        label = { Text("주소 (선택)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                item {
                    LodgingTimeRow(
                        label = "체크인",
                        time = checkInTime,
                        onChoose = { timeTarget = "체크인" },
                        onClear = { checkInTime = null },
                    )
                }
                item {
                    LodgingTimeRow(
                        label = "체크아웃",
                        time = checkOutTime,
                        onChoose = { timeTarget = "체크아웃" },
                        onClear = { checkOutTime = null },
                    )
                }
                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("메모 (선택)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                if (existing != null) {
                    item {
                        TextButton(onClick = { confirmDelete = true }, enabled = !state.isSaving) {
                            Text("숙소 삭제", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        LodgingUiModel(
                            id = existing?.id ?: 0,
                            name = name,
                            checkIn = checkIn,
                            checkOut = checkOut,
                            nights = 0,
                            address = address,
                            checkInTime = checkInTime?.let(LocalTime::parse),
                            note = note,
                            checkOutTime = checkOutTime?.let(LocalTime::parse),
                            latitude = latitude,
                            longitude = longitude,
                            googlePlaceId = googlePlaceId,
                        ),
                        resolvePlace,
                    )
                },
                enabled = name.isNotBlank() && checkIn < checkOut && !state.isSaving,
            ) { Text("숙소 저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.isSaving) { Text("취소") } },
    )

    dateTarget?.let { target ->
        val choosingCheckIn = target == "체크인"
        LodgingOptionsDialog(
            title = "$target 날짜",
            options = dates.filter { if (choosingCheckIn) it < last else it > checkIn },
            label = { it.format(dateFormat) },
            onDismiss = { dateTarget = null },
            onSelect = { date ->
                if (choosingCheckIn) {
                    checkInText = date.toString()
                    if (checkOut <= date) checkOutText = date.plusDays(1).toString()
                } else {
                    checkOutText = date.toString()
                }
                dateTarget = null
            },
        )
    }
    timeTarget?.let { target ->
        val choosingCheckIn = target == "체크인"
        val selectedTime = if (choosingCheckIn) checkInTime else checkOutTime
        ScheduleTimePickerDialog(
            title = "$target 시간 선택",
            initialTime = selectedTime?.let(LocalTime::parse) ?: LocalTime.of(if (choosingCheckIn) 15 else 11, 0),
            onDismiss = { timeTarget = null },
            onConfirm = { time ->
                if (choosingCheckIn) checkInTime = time.format(timeFormat)
                else checkOutTime = time.format(timeFormat)
                timeTarget = null
            },
        )
    }
    if (choosePlace) {
        LodgingOptionsDialog(
            title = "저장한 장소",
            options = state.savedPlaces,
            label = { it.name },
            onDismiss = { choosePlace = false },
            onSelect = { place ->
                selectPlace(place, resolve = false)
                choosePlace = false
            },
        )
    }
    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("숙소 삭제") },
            text = { Text("이 숙소를 여행에서 삭제할까요?") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete(existing.id) }) { Text("삭제") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } },
        )
    }
}

@Composable
private fun LodgingTimeRow(label: String, time: String?, onChoose: () -> Unit, onClear: () -> Unit) {
    Row {
        TextButton(onClick = onChoose) {
            Text(time?.let { "$label 시간 $it" } ?: "$label 시간 선택 (선택)")
        }
        if (time != null) TextButton(onClick = onClear) { Text("시간 지우기") }
    }
}

@Composable
private fun <T : Any> LodgingOptionsDialog(
    title: String,
    options: List<T>,
    label: (T) -> String,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.heightIn(max = 300.dp)) {
                items(options) { option -> TextButton(onClick = { onSelect(option) }) { Text(label(option)) } }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Preview(showBackground = true)
@Composable
private fun LodgingOverviewPreview() {
    val start = LocalDate.of(2026, 11, 26)
    val move = start.plusDays(2)
    val lodgings = listOf(
        LodgingUiModel(1, "마이애미 호텔", start, move, 2),
        LodgingUiModel(2, "다른 호텔", move, move.plusDays(2), 2),
    )
    MaterialTheme {
        Column {
            LodgingSection(lodgings, onAdd = {}, onEdit = {})
            LodgingDayInfo(lodgings, move)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LodgingEditorPreview() {
    val start = LocalDate.of(2026, 11, 26)
    MaterialTheme {
        LodgingEditor(
            existing = null,
            dates = List(5) { start.plusDays(it.toLong()) },
            state = LodgingUiState(),
            onDismiss = {},
            onSave = { _, _ -> },
            onDelete = {},
        )
    }
}
