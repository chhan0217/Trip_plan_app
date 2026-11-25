package com.example.tripplan.presentation.itineraryphoto

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.tripplan.domain.model.RuntimePermissionStatus

@Composable
internal fun ItineraryPhotoAddDialog(
    itineraryId: Long?,
    canAdd: Boolean,
    state: ItineraryPhotoUiState,
    viewModel: ItineraryPhotoViewModel,
    onDismiss: () -> Unit,
) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    var pendingItineraryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var captureTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var wasCameraRequested by rememberSaveable { mutableStateOf(false) }
    var permissionPrompt by rememberSaveable { mutableStateOf<RuntimePermissionStatus?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { images ->
        val target = pendingItineraryId
        pendingItineraryId = null
        if (target != null) viewModel.addPickedPhotos(target, images.map(Uri::toString))
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val target = captureTarget
        val item = pendingItineraryId
        captureTarget = null
        pendingItineraryId = null
        if (target != null) viewModel.handleCapture(item, target, captured)
    }
    val launchCamera: () -> Unit = {
        viewModel.prepareCapture { target ->
            captureTarget = target
            try {
                camera.launch(Uri.parse(target))
            } catch (_: ActivityNotFoundException) {
                captureTarget = null
                pendingItineraryId = null
                viewModel.handleCapture(null, target, captured = false)
                viewModel.showError("이 기기에서 카메라를 열 수 없습니다.")
            } catch (_: SecurityException) {
                captureTarget = null
                pendingItineraryId = null
                viewModel.handleCapture(null, target, captured = false)
                viewModel.showError("카메라 권한을 확인해 주세요.")
            }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && pendingItineraryId != null && canAdd) {
            launchCamera()
        } else {
            pendingItineraryId = null
            if (!granted) viewModel.showError("카메라 권한이 필요합니다. 사진첩에서는 계속 추가할 수 있어요.")
        }
    }

    if (itineraryId != null && canAdd) {
        PhotoSourceDialog(
            busy = state.isSaving || pendingItineraryId != null,
            error = state.error,
            onDismiss = onDismiss,
            onPick = {
                viewModel.clearError()
                pendingItineraryId = itineraryId
                try {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                } catch (_: ActivityNotFoundException) {
                    pendingItineraryId = null
                    viewModel.showError("이 기기에서 사진첩을 열 수 없습니다.")
                }
            },
            onCapture = {
                viewModel.clearError()
                val shouldShowRationale = activity?.let {
                    ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
                } ?: false
                when (val status = viewModel.cameraStatus(wasCameraRequested, shouldShowRationale)) {
                    RuntimePermissionStatus.Granted -> {
                        pendingItineraryId = itineraryId
                        launchCamera()
                    }
                    RuntimePermissionStatus.Unsupported ->
                        viewModel.showError("이 기기에서는 카메라 촬영을 사용할 수 없습니다.")
                    else -> permissionPrompt = status
                }
            },
        )
    }

    val prompt = permissionPrompt
    if (prompt != null && itineraryId != null && canAdd) {
        val openSettings = prompt == RuntimePermissionStatus.PermanentlyDenied
        AlertDialog(
            onDismissRequest = { permissionPrompt = null },
            title = { Text("카메라 권한") },
            text = {
                Text(
                    if (openSettings) "직접 찍으려면 앱 설정에서 카메라 권한을 허용해 주세요."
                    else "이 일정에 사진을 찍어 추가하려면 카메라 권한이 필요합니다."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    permissionPrompt = null
                    if (openSettings) {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                    } else {
                        pendingItineraryId = itineraryId
                        wasCameraRequested = true
                        permission.launch(Manifest.permission.CAMERA)
                    }
                }) { Text(if (openSettings) "설정 열기" else "계속") }
            },
            dismissButton = { TextButton(onClick = { permissionPrompt = null }) { Text("취소") } },
        )
    }
}

@Composable
private fun PhotoSourceDialog(
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onPick: () -> Unit,
    onCapture: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("사진 추가") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (busy) Text("사진을 처리하는 중입니다.")
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onCapture, enabled = !busy) { Text("사진 촬영") }
                    TextButton(onClick = onPick, enabled = !busy) { Text("사진첩에서 추가") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("닫기") } },
    )
}

@Preview(showBackground = true)
@Composable
private fun PhotoSourcePreview() {
    MaterialTheme {
        PhotoSourceDialog(false, null, {}, {}, {})
    }
}
