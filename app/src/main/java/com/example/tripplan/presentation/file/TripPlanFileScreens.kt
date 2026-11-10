package com.example.tripplan.presentation.file

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tripplan.R
import com.example.tripplan.domain.model.TripPlanFileFailure
import com.example.tripplan.domain.model.TripPlanImportPreview
import java.time.LocalDate

@Composable
fun TripPlanExportScreen(
    tripPlanId: Long,
    onBackClick: () -> Unit,
    viewModel: TripPlanExportViewModel = hiltViewModel(key = "trip-export-$tripPlanId"),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(TRIP_PLAN_MIME_TYPE),
        { uri -> viewModel.onSaveDestinationSelected(uri?.toString()) },
    )
    LaunchedEffect(viewModel, tripPlanId) { viewModel.load(tripPlanId) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TripPlanExportEvent.Share -> shareTripPlan(context, event.uri)
            }
        }
    }
    TripPlanExportContent(
        state = state,
        onBackClick = onBackClick,
        onSaveClick = { saveLauncher.launch(state.fileName) },
        onShareClick = viewModel::onShareClick,
    )
}

@Composable
internal fun TripPlanExportContent(
    state: TripPlanExportUiState,
    onBackClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
) {
    FileScaffold(title = "여행 파일 공유", onBackClick = onBackClick) {
        when {
            state.isLoading -> CircularProgressIndicator()
            state.failure != null && state.fileName.isBlank() -> Text(
                fileFailureMessage(state.failure), color = MaterialTheme.colorScheme.error,
            )
            else -> Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("같은 계획 파일을 친구와 공유합니다.", style = MaterialTheme.typography.titleMedium)
                Text(state.fileName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "파일에는 여행과 일정 정보만 들어가며 계정이나 기기 정보는 포함되지 않습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(
                    onClick = onShareClick,
                    enabled = !state.isWorking && state.fileName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("친구에게 공유") }
                OutlinedButton(
                    onClick = onSaveClick,
                    enabled = !state.isWorking && state.fileName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("기기에 파일 저장") }
                if (state.isWorking) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                if (state.isSaved) Text("파일을 저장했습니다.", color = MaterialTheme.colorScheme.primary)
                state.failure?.let {
                    Text(fileFailureMessage(it), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun TripPlanImportScreen(
    uri: String,
    onBackClick: () -> Unit,
    onImported: (Long) -> Unit,
    viewModel: TripPlanImportViewModel = hiltViewModel(key = "trip-import-$uri"),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel, uri) { viewModel.load(uri) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TripPlanImportEvent.Imported -> onImported(event.tripPlanId)
            }
        }
    }
    TripPlanImportContent(
        state = state,
        onBackClick = onBackClick,
        onImportClick = viewModel::onImportClick,
    )
}

@Composable
internal fun TripPlanImportContent(
    state: TripPlanImportUiState,
    onBackClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
) {
    FileScaffold(title = "여행 파일 가져오기", onBackClick = onBackClick) {
        when {
            state.isLoading -> CircularProgressIndicator()
            state.preview != null -> {
                val preview = state.preview
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("가져오기 전에 내용을 확인해 주세요.", style = MaterialTheme.typography.titleMedium)
                    Text(preview.title, style = MaterialTheme.typography.headlineSmall)
                    if (preview.destination.isNotBlank()) Text(preview.destination)
                    Text("${preview.startDate} ~ ${preview.endDate}")
                    Text("${preview.dayCount}일 · 일정 ${preview.itineraryItemCount}개")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "기존 여행은 변경하지 않고 새 여행으로 추가합니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = onImportClick,
                        enabled = !state.isImporting,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (state.isImporting) "가져오는 중" else "새 여행으로 가져오기") }
                    if (state.failure != null) {
                        Text(fileFailureMessage(state.failure), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            state.failure != null -> Text(
                fileFailureMessage(state.failure), color = MaterialTheme.colorScheme.error,
            )
            else -> Text("파일 내용을 확인할 수 없습니다.")
        }
    }
}

@Composable
private fun FileScaffold(
    title: String,
    onBackClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.ic_arrow_back_24),
                        contentDescription = "뒤로가기",
                    )
                }
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.TopCenter,
            ) { content() }
        }
    }
}

private fun shareTripPlan(context: Context, uriValue: String) {
    val uri = Uri.parse(uriValue)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = TRIP_PLAN_MIME_TYPE
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri("여행 계획", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "여행 계획 공유"))
}

private fun fileFailureMessage(failure: TripPlanFileFailure): String = when (failure) {
    TripPlanFileFailure.TripNotFound -> "여행을 찾을 수 없습니다."
    TripPlanFileFailure.InvalidFormat -> "여행 계획 파일 형식이 아닙니다."
    TripPlanFileFailure.UnsupportedVersion -> "현재 앱에서 지원하지 않는 파일 버전입니다."
    TripPlanFileFailure.InvalidData -> "파일의 여행 또는 일정 정보가 올바르지 않습니다."
    TripPlanFileFailure.ReadFailed -> "파일을 읽지 못했습니다."
    TripPlanFileFailure.WriteFailed -> "파일을 저장하지 못했습니다."
}

private const val TRIP_PLAN_MIME_TYPE = "application/json"

@Preview(showBackground = true)
@Composable
private fun ExportReadyPreview() {
    MaterialTheme { TripPlanExportContent(TripPlanExportUiState(fileName = "제주_여행.tripplan.json")) }
}

@Preview(showBackground = true)
@Composable
private fun ImportPreview() {
    MaterialTheme {
        TripPlanImportContent(
            TripPlanImportUiState(
                preview = TripPlanImportPreview(
                    title = "제주 여행",
                    destination = "제주",
                    startDate = LocalDate.of(2026, 11, 9),
                    endDate = LocalDate.of(2026, 11, 11),
                    dayCount = 3,
                    itineraryItemCount = 4,
                )
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ImportErrorPreview() {
    MaterialTheme {
        TripPlanImportContent(TripPlanImportUiState(failure = TripPlanFileFailure.InvalidFormat))
    }
}
