package three.two.bit.phonemanager.ui.groups

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import three.two.bit.phonemanager.R
import timber.log.Timber
import java.util.concurrent.Executors

/**
 * Story E11.9 Task 10: QR Scanner Screen
 *
 * AC E11.9.5: QR Code Scanning
 *
 * Provides camera-based QR code scanning to extract invite codes.
 *
 * @param onNavigateBack Callback to navigate back
 * @param onCodeScanned Callback when a valid code is scanned (passes the extracted code)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(
    onNavigateBack: () -> Unit,
    onCodeScanned: (String) -> Unit,
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.qr_scan_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { paddingValues ->
        if (hasCameraPermission) {
            CameraPreviewContent(
                onCodeScanned = { code ->
                    Timber.i("QR code scanned: $code")
                    onCodeScanned(code)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
        } else {
            PermissionDeniedContent(
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onNavigateBack = onNavigateBack,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
        }
    }
}

/**
 * Camera preview with QR code scanning
 */
@Composable
private fun CameraPreviewContent(
    onCodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var hasScanned by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }.also { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    fun bindCamera(cameraProvider: ProcessCameraProvider) {
                        val preview = Preview.Builder()
                            .build()
                            .also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                        val barcodeScanner = BarcodeScanning.getClient()

                        @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null && !hasScanned) {
                                        val image = InputImage.fromMediaImage(
                                            mediaImage,
                                            imageProxy.imageInfo.rotationDegrees
                                        )

                                        barcodeScanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                for (barcode in barcodes) {
                                                    if (barcode.valueType == Barcode.TYPE_URL ||
                                                        barcode.valueType == Barcode.TYPE_TEXT
                                                    ) {
                                                        barcode.rawValue?.let { value ->
                                                            val code = extractInviteCode(value)
                                                            if (code != null && !hasScanned) {
                                                                hasScanned = true
                                                                onCodeScanned(value)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    } else {
                                        imageProxy.close()
                                    }
                                }
                            }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            Timber.e(e, "Camera binding failed")
                        }
                    }

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        // Avoid binding before the view has a size to reduce NaN frame rate logs.
                        val tryBind = { bindCamera(cameraProvider) }
                        if (previewView.width == 0 || previewView.height == 0) {
                            previewView.doOnLayout { tryBind() }
                        } else {
                            tryBind()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Scanning overlay
        ScannerOverlay()

        // Instructions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.qr_scan_instructions_title),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.qr_scan_instructions_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Scanner overlay with cutout for QR code area
 */
@Composable
private fun ScannerOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val scanAreaSize = size.minDimension * 0.7f
        val scanAreaLeft = (size.width - scanAreaSize) / 2
        val scanAreaTop = (size.height - scanAreaSize) / 2

        // Dark overlay
        drawRect(
            color = Color.Black.copy(alpha = 0.6f),
            size = size,
        )

        // Transparent cutout (scan area)
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(scanAreaLeft, scanAreaTop),
            size = androidx.compose.ui.geometry.Size(scanAreaSize, scanAreaSize),
            cornerRadius = CornerRadius(16f, 16f),
            blendMode = BlendMode.Clear,
        )

        // Border around scan area
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(scanAreaLeft, scanAreaTop),
            size = androidx.compose.ui.geometry.Size(scanAreaSize, scanAreaSize),
            cornerRadius = CornerRadius(16f, 16f),
            style = Stroke(width = 4f),
        )

        // Corner accents
        val cornerLength = 40f
        val cornerOffset = 2f
        val accentColor = Color(0xFF4CAF50) // Green accent

        // Top-left corner
        drawLine(
            color = accentColor,
            start = Offset(scanAreaLeft - cornerOffset, scanAreaTop + cornerLength),
            end = Offset(scanAreaLeft - cornerOffset, scanAreaTop - cornerOffset),
            strokeWidth = 8f,
        )
        drawLine(
            color = accentColor,
            start = Offset(scanAreaLeft - cornerOffset, scanAreaTop - cornerOffset),
            end = Offset(scanAreaLeft + cornerLength, scanAreaTop - cornerOffset),
            strokeWidth = 8f,
        )

        // Top-right corner
        drawLine(
            color = accentColor,
            start = Offset(scanAreaLeft + scanAreaSize + cornerOffset, scanAreaTop + cornerLength),
            end = Offset(scanAreaLeft + scanAreaSize + cornerOffset, scanAreaTop - cornerOffset),
            strokeWidth = 8f,
        )
        drawLine(
            color = accentColor,
            start = Offset(scanAreaLeft + scanAreaSize - cornerLength, scanAreaTop - cornerOffset),
            end = Offset(scanAreaLeft + scanAreaSize + cornerOffset, scanAreaTop - cornerOffset),
            strokeWidth = 8f,
        )

        // Bottom-left corner
        drawLine(
            color = accentColor,
            start = Offset(scanAreaLeft - cornerOffset, scanAreaTop + scanAreaSize - cornerLength),
            end = Offset(scanAreaLeft - cornerOffset, scanAreaTop + scanAreaSize + cornerOffset),
            strokeWidth = 8f,
        )
        drawLine(
            color = accentColor,
            start = Offset(scanAreaLeft - cornerOffset, scanAreaTop + scanAreaSize + cornerOffset),
            end = Offset(scanAreaLeft + cornerLength, scanAreaTop + scanAreaSize + cornerOffset),
            strokeWidth = 8f,
        )

        // Bottom-right corner
        drawLine(
            color = accentColor,
            start = Offset(scanAreaLeft + scanAreaSize + cornerOffset, scanAreaTop + scanAreaSize - cornerLength),
            end = Offset(scanAreaLeft + scanAreaSize + cornerOffset, scanAreaTop + scanAreaSize + cornerOffset),
            strokeWidth = 8f,
        )
        drawLine(
            color = accentColor,
            start = Offset(scanAreaLeft + scanAreaSize - cornerLength, scanAreaTop + scanAreaSize + cornerOffset),
            end = Offset(scanAreaLeft + scanAreaSize + cornerOffset, scanAreaTop + scanAreaSize + cornerOffset),
            strokeWidth = 8f,
        )
    }
}

/**
 * Permission denied content
 */
@Composable
private fun PermissionDeniedContent(
    onRequestPermission: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = stringResource(R.string.camera_permission_required_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = stringResource(R.string.qr_camera_permission_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onRequestPermission) {
                Text(stringResource(R.string.qr_grant_permission))
            }

            Button(onClick = onNavigateBack) {
                Text(stringResource(R.string.qr_enter_manually))
            }
        }
    }
}

/**
 * Extract invite code from scanned content
 */
private fun extractInviteCode(content: String): String? {
    val trimmed = content.trim()

    // Check for deep link format: phonemanager://join/{code}
    val deepLinkRegex = Regex("""phonemanager://join/([A-Za-z0-9]{8})""", RegexOption.IGNORE_CASE)
    deepLinkRegex.find(trimmed)?.let { match ->
        return match.groupValues[1].uppercase()
    }

    // Check for plain 8-character alphanumeric code
    if (trimmed.length == 8 && trimmed.all { it.isLetterOrDigit() }) {
        return trimmed.uppercase()
    }

    // Check for HTTPS URL format
    val urlRegex = Regex("""https?://[^/]+/join/([A-Za-z0-9]{8})""", RegexOption.IGNORE_CASE)
    urlRegex.find(trimmed)?.let { match ->
        return match.groupValues[1].uppercase()
    }

    return null
}
