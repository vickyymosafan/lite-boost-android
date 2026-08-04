package com.optimizer.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.optimizer.android.presentation.CameraViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProCameraActivity : ComponentActivity() {

    private var imageCapture: ImageCapture? = null
    
    private val viewModel: CameraViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            setContent { CameraScreen() }
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            setContent { CameraScreen() }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }


    private fun takePhoto() {
        val capture = imageCapture ?: return

        val photoFile = viewModel.getPhotoFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("ProCameraActivity", "Photo capture failed: ${exc.message}", exc)
                    viewModel.onPhotoError(exc.message ?: "Unknown error")
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.d("ProCameraActivity", "Photo capture succeeded: ${photoFile.absolutePath}")
                    viewModel.onPhotoSaved()
                }
            })
    }

    @Composable
    fun CameraScreen() {
        val lifecycleOwner = LocalLifecycleOwner.current
        val context = LocalContext.current
        val previewView = remember { PreviewView(context) }
        
        val uiState by viewModel.uiState.collectAsState()

        LaunchedEffect(uiState.showToastMessage) {
            uiState.showToastMessage?.let { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                viewModel.clearToastMessage()
            }
        }

        LaunchedEffect(previewView) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                val extensionsManagerFuture = ExtensionsManager.getInstanceAsync(context, cameraProvider)
                extensionsManagerFuture.addListener({
                    val extensionsManager = extensionsManagerFuture.get()
                    
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    var activeCameraSelector = cameraSelector

                    if (extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.HDR)) {
                        activeCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(cameraSelector, ExtensionMode.HDR)
                        viewModel.setHdrEnabled(true)
                    } else if (extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.BOKEH)) {
                        activeCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(cameraSelector, ExtensionMode.BOKEH)
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, activeCameraSelector, preview, imageCapture
                        )
                    } catch (exc: Exception) {
                        Log.e("ProCameraActivity", "Use case binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(context))
                
            }, ContextCompat.getMainExecutor(context))
        }

        CameraUI(
            previewView = previewView,
            isHdrEnabled = uiState.isHdrEnabled,
            onCaptureClick = { takePhoto() }
        )
    }
}

@Composable
fun CameraUI(
    previewView: PreviewView,
    isHdrEnabled: Boolean,
    onCaptureClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f/4f)
                .align(Alignment.Center)
        )

        CameraTopBar(isHdrEnabled = isHdrEnabled, modifier = Modifier.align(Alignment.TopCenter))
        CameraBottomBar(onCaptureClick = onCaptureClick, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun CameraTopBar(isHdrEnabled: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        if (isHdrEnabled) {
            Text("HDR", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun CameraBottomBar(onCaptureClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color.White, CircleShape)
                .align(Alignment.Center)
                .clickable { onCaptureClick() }
        )
    }
}
