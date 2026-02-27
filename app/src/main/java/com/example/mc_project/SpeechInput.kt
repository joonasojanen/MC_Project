package com.example.mc_project

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

// Online sources and android speech documentation was used to build this code block
@Composable
fun SpeechInput(
    modifier: Modifier = Modifier,
    onFinalText: (String) -> Unit
) {
    val context = LocalContext.current
    var sttState by remember { mutableStateOf(SttUiState()) }

    val stt = remember {
        SpeechToTextController(context) { sttState = it }
    }

    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { micGranted = it }

    DisposableEffect(Unit) {
        onDispose { stt.destroy() }
    }

    LaunchedEffect(sttState.final) {
        val text = sttState.final.trim()
        if (text.isNotEmpty()) {
        onFinalText(text)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            when {
                sttState.error != null -> "No sound detected"
                sttState.isListening && sttState.partial.isBlank() -> "Listening..."
                sttState.isListening && sttState.partial.isNotBlank() -> sttState.partial
                sttState.final.isNotBlank() -> sttState.final
                else -> "Tap mic and speak…"
            }
        )

        Spacer(Modifier.height(8.dp))

        Button(onClick = {
            if (!micGranted) {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                if (sttState.isListening) stt.stop() else stt.start()
            }
        }) {
            Text(if (sttState.isListening) "Stop Mic" else "Start Mic")
        }
    }
}
