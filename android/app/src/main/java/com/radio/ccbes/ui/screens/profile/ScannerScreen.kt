package com.radio.ccbes.ui.screens.profile

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.radio.ccbes.data.repository.UserRepository
import com.radio.ccbes.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val auth = remember { FirebaseAuth.getInstance() }
    val userRepository = remember { UserRepository() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isProcessing by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("Escanear perfil", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (hasCameraPermission) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build()
                            preview.setSurfaceProvider(previewView.surfaceProvider)

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            val scanner = BarcodeScanning.getClient()

                            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                if (isProcessing) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }

                                @androidx.camera.core.ExperimentalGetImage
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    scanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            if (isProcessing) return@addOnSuccessListener
                                            
                                            for (barcode in barcodes) {
                                                val rawValue = barcode.rawValue
                                                if (rawValue != null && rawValue.startsWith("https://www.ccbes.com.ar/profile/")) {
                                                    val scannedValue = rawValue.removePrefix("https://www.ccbes.com.ar/profile/")
                                                    
                                                    isProcessing = true
                                                    scope.launch {
                                                        try {
                                                            val currentUser = auth.currentUser
                                                            if (currentUser != null) {
                                                                // 1. Intentar resolver como ID de usuario directamente (más fiable)
                                                                var targetUser = userRepository.getUser(scannedValue)
                                                                
                                                                // 2. Si no se encuentra, intentar como handle (para compatibilidad)
                                                                if (targetUser == null) {
                                                                    targetUser = userRepository.getUserByHandle(scannedValue)
                                                                }

                                                                if (targetUser != null) {
                                                                    if (targetUser.id != currentUser.uid) {
                                                                        navController.navigate(Screen.Profile.createRoute(targetUser.id)) {
                                                                            popUpTo(Screen.Scanner.route) { inclusive = true }
                                                                        }
                                                                    } else {
                                                                        Toast.makeText(context, "Este es tu propio perfil", Toast.LENGTH_SHORT).show()
                                                                        navController.popBackStack()
                                                                    }
                                                                } else {
                                                                    Log.d("Scanner", "Usuario no encontrado: $scannedValue")
                                                                    Toast.makeText(context, "No se encontró el perfil", Toast.LENGTH_SHORT).show()
                                                                    isProcessing = false
                                                                }
                                                            } else {
                                                                isProcessing = false
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.e("Scanner", "Error processing scan", e)
                                                            isProcessing = false
                                                        }
                                                    }
                                                    break
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

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                Log.e("Scanner", "Use case binding failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    }
                )
                
                // Scanner Overlay
                Box(
                    modifier = Modifier.fillMaxSize().padding(bottom = 100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.size(250.dp),
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(24.dp)
                    ) {}
                    
                    Text(
                        "Encuadra el código QR",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
                        fontSize = 14.sp
                    )
                }

                if (isProcessing) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
        } else {
           Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Permiso de cámara necesario", color = Color.White)
            }
        }
    }
}
