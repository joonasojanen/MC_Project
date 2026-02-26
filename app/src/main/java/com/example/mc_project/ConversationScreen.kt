package com.example.mc_project

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import java.io.File
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

data class Message(val body: String)

// AI was used to help align the text and buttons
// Developer pages were used as a base to build this code block
@Composable
fun ConversationScreen(vm: ProfileViewModel, onGoToProfile: () -> Unit) {
    val name by vm.name.collectAsState()
    val imagePath by vm.imagePath.collectAsState()
    val cameraImages by vm.cameraImages.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Gallery")

            Button(onClick = onGoToProfile) {
                Text("Back to Profile")
            }
        }

        Conversation(
            images = cameraImages,
            authorName = name,
            authorImagePath = imagePath,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// Code from online source "developer" has been used as a base here
@Composable
fun MessageCard(
    img: CameraImageEntity,
    authorName: String,
    authorImagePath: String?,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.padding(8.dp)) {
        var showFullScreen by remember { mutableStateOf(false) }

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {

                AsyncImage(
                    model = when {
                        authorImagePath.isNullOrBlank() -> null
                        authorImagePath.startsWith("http", true) -> authorImagePath
                        else -> File(authorImagePath)
                    },
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )

                Spacer(Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = authorName,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.width(6.dp))

                    Text(
                        text = formatTimeFromFile(img.imagePath),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 3.dp,
                shadowElevation = 1.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .padding(start = 52.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { showFullScreen = true }
            ) {
                // Feed preview (square)
                AsyncImage(
                    model = File(img.imagePath),
                    contentDescription = "Captured image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
            }

            // Fullscreen overlay, show whole image
            // AI was use to debug this, because it didn't work first
            if (showFullScreen) {
                Dialog(
                    onDismissRequest = { showFullScreen = false },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false,
                        dismissOnClickOutside = true,
                        dismissOnBackPress = true
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { showFullScreen = false })
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = File(img.imagePath),
                            contentDescription = "Full image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        )
                    }
                }
            }
        }
    }
}


// Code from online source "developer" has been used as a base here
@Composable
fun Conversation(
    images: List<CameraImageEntity>, authorName: String, authorImagePath: String?, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        items(images) { img ->
            MessageCard(img = img, authorName = authorName, authorImagePath = authorImagePath)
        }
    }
}