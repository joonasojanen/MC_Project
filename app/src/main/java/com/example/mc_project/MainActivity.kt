package com.example.mc_project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.example.mc_project.ui.theme.MC_ProjectTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.interaction.MutableInteractionSource
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.delay
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    // Holds "theme requested by notification", if any
    private val requestedTheme = mutableStateOf<Boolean?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // If app launched from notification
        requestedTheme.value = readThemeFromIntent(intent)

        setContent {
            var useDarkMode by rememberSaveable { mutableStateOf(false) }

            // Apply when notification requests a theme
            LaunchedEffect(requestedTheme.value) {
                requestedTheme.value?.let { useDarkMode = it }
            }

            MC_ProjectTheme(darkTheme = useDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Scaffold { innerPadding ->
                        MC_ProjectNavHost(
                            modifier = Modifier.padding(innerPadding),
                            currentDarkMode = useDarkMode
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedTheme.value = readThemeFromIntent(intent)
    }

    private fun readThemeFromIntent(intent: Intent?): Boolean? {
        if (intent == null) return null
        return if (intent.hasExtra("apply_theme")) {
            intent.getBooleanExtra("apply_theme", false)
        } else null
    }
}

data class Message(val body: String)

// Code from online source "developer" has been used as a base here
@Composable
fun MessageCard(msg: Message, authorName: String, authorImagePath: String?, modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(8.dp)) {
        var isExpanded by remember { mutableStateOf(false) }

        val surfaceColor by animateColorAsState(
            if (isExpanded) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface
        )

        Column {
            ProfileHeader(
                name = authorName,
                imagePath = authorImagePath,
                imageSize = 40.dp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 3.dp,
                shadowElevation = 1.dp,
                color = surfaceColor,
                modifier = Modifier
                    .padding(start = 52.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { isExpanded = !isExpanded }
                    .animateContentSize()
                    .padding(1.dp)
            ) {
                Text(
                    text = msg.body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(4.dp),
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// Code from online source "developer" has been used as a base here
@Composable
fun Conversation(messages: List<Message>, authorName: String, authorImagePath: String?, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        items(messages) { message ->
            MessageCard(msg = message, authorName = authorName, authorImagePath = authorImagePath)
        }
    }
}

// Code from online source "developer" has been used as a base here
@Composable
fun MC_ProjectNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    currentDarkMode: Boolean
) {
    val profileVm: ProfileViewModel = viewModel()

    StartLightSensorFeed(vm = profileVm, currentDarkMode = currentDarkMode)

    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = Routes.MAIN
    ) {
        composable(Routes.MAIN) {
            ConversationScreen(
                vm = profileVm,
                onGoToProfile = {
                    navController.navigate(Routes.SECOND) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Routes.SECOND) {
            ProfileScreen(
                vm = profileVm,
                onGoToConversation = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.MAIN) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

// AI was used to help align the text
@Composable
fun ConversationScreen(vm: ProfileViewModel, onGoToProfile: () -> Unit) {
    val name by vm.name.collectAsState()
    val imagePath by vm.imagePath.collectAsState()
    val messages by vm.messages.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Conversation")

            Button(onClick = onGoToProfile) {
                Text("Back to Profile")
            }
        }

        Conversation(
            messages = messages,
            authorName = name,
            authorImagePath = imagePath,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// AI was used to help align the text and buttons
// Developer pages were used as a base to build this code block
// AI was used to depug the url download I had 403 error
// EXAMPLE URL: https://upload.wikimedia.org/wikipedia/commons/b/b6/Image_created_with_a_mobile_phone.png
@Composable
fun ProfileScreen(vm: ProfileViewModel, onGoToConversation: () -> Unit) {
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
    ) { isGranted ->
        notificationGranted = isGranted
    }

    LaunchedEffect(dbName) {
        if (textName.isBlank()) textName = dbName
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val storedPath = copyPickedImageToAppStorage(context, uri)
            vm.saveImagePath(storedPath)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                focusManager.clearFocus()
                if (textName != dbName) vm.saveName(textName)
            }
    ) {
        // Profile text and conversation button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Profile")
            Button(onClick = onGoToConversation) { Text("Go to Lux sensor data") }
        }

        // Coil image request
        Spacer(Modifier.height(16.dp))
        ProfileHeader(
            name = dbName,
            imagePath = imagePath,
            imageSize = 80.dp
        )
        Spacer(Modifier.height(16.dp))

        // Enable Notifications button
        Button(
            onClick = {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            enabled = !notificationGranted,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (notificationGranted) "Notifications Enabled" else "Enable Notifications")
        }

        Spacer(Modifier.height(16.dp))

        // One button to choose from gallery or url
        Box {
            Button(onClick = { photoMenuExpanded = true }) {
                Text("Pick Profile Photo")
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
                            if (url.startsWith("http", ignoreCase = true)) {
                                vm.saveImagePath(url)
                            }
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

        // Name selection box
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

// Code from online source "developer" has been used as a base here
// AI was used to debug how to get the change in dark and light mode to work
@Composable
fun StartLightSensorFeed(
    vm: ProfileViewModel,
    currentDarkMode: Boolean,
) {
    val context = LocalContext.current
    val latestDarkMode = rememberUpdatedState(currentDarkMode)
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val lightSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) }
    var latestLux by remember { mutableStateOf<Float?>(null) }
    val darkBelowLux = 5000f
    val lightAboveLux = 10000f

    DisposableEffect(lightSensor) {
        if (lightSensor == null) {
            vm.addMessage("Light sensor not available on this device/emulator.")
            return@DisposableEffect onDispose { }
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val lux = event.values.firstOrNull() ?: return
                if (lux < 0f || lux > 40_000f) return
                latestLux = lux
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    LaunchedEffect(Unit) {
        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        while (true) {
            val lux = latestLux
            if (lux != null) {
                val isDarkNow = latestDarkMode.value

                val newDarkMode = when {
                    isDarkNow && lux >= lightAboveLux -> false
                    !isDarkNow && lux <= darkBelowLux -> true
                    else -> isDarkNow
                }

                if (newDarkMode != isDarkNow) {
                    // show notification
                    NotificationHelper.showThemeChanged(context, newDarkMode)
                }

                val time = timeFmt.format(Date())
                vm.addMessage("Light: %.1f lux (at %s)".format(lux, time))
            }

            delay(10_000)
        }
    }
}
