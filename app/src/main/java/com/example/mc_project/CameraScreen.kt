package com.example.mc_project

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.Surface
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.coroutines.resume

// online sources was used
// AI was used to help align the text and buttons
// Developer pages were used as a base to build this code block
@Composable
fun CameraScreen(vm: ProfileViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current

    // Permission
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val askPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted = it }

    LaunchedEffect(Unit) {
        if (!granted) askPermission.launch(Manifest.permission.CAMERA)
    }

    if (!granted) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))
            Text("Camera permission needed.")
            Button(onClick = { askPermission.launch(Manifest.permission.CAMERA) }) {
                Text("Grant")
            }
        }
        return
    }

    // Create PreviewView via AndroidView
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    val rotation = view.display?.rotation ?: Surface.ROTATION_0
    val preview = remember { Preview.Builder().build() }
    val imageCapture = remember(rotation) {
        ImageCapture.Builder()
            .setTargetRotation(rotation)
            .build()
    }

    // UI
    Column(Modifier.fillMaxSize()) {
        // Preview + shutter overlay
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).also { pv ->
                        previewView = pv
                    }
                }
            )

            var cameraSelector by remember {
                mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA)
            }

            // Bind camera AFTER PreviewView exists
            LaunchedEffect(previewView, imageCapture, cameraSelector) {
                val pv = previewView ?: return@LaunchedEffect

                preview.setSurfaceProvider(pv.surfaceProvider)

                val cameraProvider = context.getCameraProvider()
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            }

            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()

            val scale by animateFloatAsState(
                targetValue = if (pressed) 0.88f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "shutterBounce"
            )

            // Back button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Image shutter button (overlay)
            IconButton(
                onClick = {
                    val file = createPhotoFile(context)
                    val opts = ImageCapture.OutputFileOptions.Builder(file).build()

                    imageCapture.takePicture(
                        opts,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onError(error: ImageCaptureException) {
                                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                            }

                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                vm.addCameraImage(file.absolutePath)
                                Toast.makeText(context, "Saved: ${file.name}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                },
                interactionSource = interactionSource,
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .scale(scale)
                    .alpha(if (pressed) 0.6f else 1f)
                    .background(Color.White, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = "Take picture",
                    tint = Color.Black,
                    modifier = Modifier.size(48.dp)
                )
            }

            IconButton(
                onClick = {
                    cameraSelector =
                        if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        else
                            CameraSelector.DEFAULT_BACK_CAMERA
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Cameraswitch,
                    contentDescription = "Switch camera",
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

private fun createPhotoFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val baseDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
    val dir = File(baseDir, "camera").apply { mkdirs() }
    return File(dir, "IMG_$timeStamp.jpg")
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener(
            {cont.resume(future.get())},
            ContextCompat.getMainExecutor(this)
        )
    }