package com.example.tripplan.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(onBack: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack) { Text("뒤로") }
        Text("설정", style = MaterialTheme.typography.headlineSmall)
        Text("설정 기능은 준비 중입니다.")
        Text("여행 계획은 이 기기에 저장되며, 파일을 통해 친구와 공유할 수 있습니다.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true) @Composable private fun SettingsPreview() { MaterialTheme { SettingsScreen() } }
