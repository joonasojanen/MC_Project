package com.example.mc_project

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll


// AI was used to help align the text and buttons
// Developer pages were used as a base to build this code block
// AI was used to debug the url download I had 403 error
// EXAMPLE URL: https://upload.wikimedia.org/wikipedia/commons/b/b6/Image_created_with_a_mobile_phone.png
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    vm: ProfileViewModel,
    onGoToConversation: () -> Unit,
    onOpenCamera: () -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val dbName by vm.name.collectAsState()
    val imagePath by vm.imagePath.collectAsState()

    var textName by remember { mutableStateOf("") }
    var photoMenuExpanded by remember { mutableStateOf(false) }
    var showUrlDialog by remember { mutableStateOf(false) }
    var imageUrl by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    var notificationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> notificationGranted = isGranted }

    LaunchedEffect(dbName) { if (textName.isBlank()) textName = dbName }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val storedPath = copyPickedImageToAppStorage(context, uri)
            vm.saveImagePath(storedPath)
        }
    }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var opened by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Profile")

                Button(onClick = onGoToConversation) {
                    Text("Go to Gallery")
                }
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusManager.clearFocus()
                    if (textName != dbName) vm.saveName(textName)
                }
        ) {

            Spacer(Modifier.height(16.dp))
            ProfileHeader(name = dbName, imagePath = imagePath, imageSize = 80.dp)
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                enabled = !notificationGranted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (notificationGranted) "Notifications Enabled" else "Enable Notifications")
            }

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { photoMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pick Profile Photo", maxLines = 1)
                    }

                    DropdownMenu(
                        expanded = photoMenuExpanded,
                        onDismissRequest = { photoMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("From Gallery") },
                            onClick = {
                                photoMenuExpanded = false
                                pickImageLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("From Web URL") },
                            onClick = {
                                photoMenuExpanded = false
                                showUrlDialog = true
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Automatic Theme", modifier = Modifier.weight(1f))
                    Switch(
                        checked = themeMode == ThemeMode.SENSOR,
                        onCheckedChange = { isOn ->
                            onThemeModeChange(if (isOn) ThemeMode.SENSOR else ThemeMode.SYSTEM)
                        }
                    )
                }
            }

            if (showUrlDialog) {
                AlertDialog(
                    onDismissRequest = { showUrlDialog = false },
                    title = { Text("Use Image From Web") },
                    text = {
                        OutlinedTextField(
                            value = imageUrl,
                            onValueChange = { imageUrl = it },
                            label = { Text("Image URL (https://...)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val url = imageUrl.trim()
                                if (url.startsWith("http", ignoreCase = true)) vm.saveImagePath(url)
                                showUrlDialog = false
                                focusManager.clearFocus()
                            }
                        ) { Text("Use") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUrlDialog = false }) { Text("Cancel") }
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = textName,
                onValueChange = { textName = it },
                label = { Text("Your name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        vm.saveName(textName)
                    }
                )
            )

            Spacer(Modifier.height(16.dp))

            SpeechInput(
                modifier = Modifier.fillMaxWidth(),
                onFinalText = { spoken ->
                    val command = spoken.lowercase()
                    val cameraCommands = listOf("open camera", "open the camera", "open a camera", "camera open")

                    if (!opened && cameraCommands.any { it in command }) {
                        opened = true
                        scope.launch {
                            delay(1000)
                            onOpenCamera()
                        }
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (pressed) 0.88f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "openCameraBounce"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            delay(200)
                            onOpenCamera()
                        }
                    },
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .size(160.dp)
                        .scale(scale)
                        .alpha(if (pressed) 0.6f else 1f)
                ) {
                    Image(
                        painter = painterResource(R.drawable.camera),
                        contentDescription = "Open camera",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

// Helper function to set the name and profile picture
// Developer and coil github pages were used as a base to build this code block
@Composable
fun ProfileHeader(name: String, imagePath: String?, modifier: Modifier = Modifier, imageSize: Dp = 40.dp)
{
    val context = LocalContext.current
    val isWebUrl = !imagePath.isNullOrBlank() && imagePath.startsWith("http", ignoreCase = true)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(
                    when {
                        imagePath.isNullOrBlank() -> null
                        isWebUrl -> imagePath
                        else -> File(imagePath)
                    }
                )
                .apply { if (isWebUrl) addHeader("User-Agent", "Mozilla/5.0") }
                .crossfade(true)
                .build(),
            contentDescription = "Profile photo",
            modifier = Modifier
                .size(imageSize)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = if (name.isBlank()) "No name set" else name,
            style = MaterialTheme.typography.titleMedium
        )
    }
}