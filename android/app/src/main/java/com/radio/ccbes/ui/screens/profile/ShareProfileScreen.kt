package com.radio.ccbes.ui.screens.profile

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.radio.ccbes.data.repository.UserRepository
import com.radio.ccbes.data.settings.SettingsManager
import com.radio.ccbes.ui.navigation.Screen
import kotlinx.coroutines.launch

@Composable
fun ShareProfileScreen(navController: NavController) {
    val auth = remember { FirebaseAuth.getInstance() }
    val userRepository = remember { UserRepository() }
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    
    var handle by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    val gradients = listOf(
        listOf(Color(0xFFF9D423), Color(0xFFFF4E50)), // Yellow to Orange-Red
        listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)), // Purple to Blue
        listOf(Color(0xFF00C6FF), Color(0xFF0072FF)), // Light Blue to dark blue
        listOf(Color(0xFFFF0080), Color(0xFFFF8C00)), // Pink to Orange
        listOf(Color.Black, Color.DarkGray)
    )
    
    val savedGradientIndex by settingsManager.profileBackgroundGradientIndex.collectAsState(initial = 0)
    var currentGradientIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(savedGradientIndex) {
        currentGradientIndex = savedGradientIndex
    }

    LaunchedEffect(Unit) {
        auth.currentUser?.let { user ->
            val profile = userRepository.getUser(user.uid)
            val rawHandle = if (!profile?.handle.isNullOrBlank()) profile?.handle else (user.displayName ?: "usuario")
            handle = rawHandle!!.removePrefix("@").lowercase().replace(" ", "_")
            
            // Usamos el UID para el QR para asegurar un escaneo y resolución 100% fiable
            val qrUrl = "https://www.ccbes.com.ar/profile/${user.uid}"
            qrBitmap = generateQRCode(qrUrl)
        }
    }

    fun downloadQR() {
        qrBitmap?.let { bitmap ->
            try {
                val filename = "RadioCCBES_QR_${handle}.png"
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/RadioCCBES")
                    }
                }

                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    context.contentResolver.openOutputStream(it).use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream!!)
                    }
                    Toast.makeText(context, "QR guardado en Galería", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradients[currentGradientIndex.coerceIn(gradients.indices)]))
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
            }
            
            Button(
                onClick = { 
                    val nextIndex = (currentGradientIndex + 1) % gradients.size
                    currentGradientIndex = nextIndex
                    scope.launch {
                        settingsManager.setProfileBackgroundGradientIndex(nextIndex)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text("COLOR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            IconButton(onClick = { navController.navigate(Screen.Scanner.route) }) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear", tint = Color.White)
            }
        }

        // Central Card
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.8f)
                .aspectRatio(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    qrBitmap?.let {
                        androidx.compose.foundation.Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "@${handle.removePrefix("@").lowercase()}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = gradients[currentGradientIndex.coerceIn(gradients.indices)][1],
                    textAlign = TextAlign.Center
                )
            }
        }

        // Bottom Actions
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .fillMaxWidth(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionItem(
                    icon = Icons.Default.Share, 
                    label = "Compartir\nperfil",
                    onClick = {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "¡Mira mi perfil en Radio CCBES! https://www.ccbes.com.ar/profile/$handle")
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }
                )
                ActionItem(
                    icon = Icons.Default.ContentCopy, 
                    label = "Copiar\nenlace",
                    onClick = {
                        clipboardManager.setText(AnnotatedString("https://www.ccbes.com.ar/profile/$handle"))
                        Toast.makeText(context, "Enlace copiado", Toast.LENGTH_SHORT).show()
                    }
                )
                ActionItem(
                    icon = Icons.Default.Download, 
                    label = "Descargar",
                    onClick = { downloadQR() }
                )
            }
        }
    }
}

@Composable
fun ActionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier.size(50.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(12.dp),
                tint = Color.Black
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
            color = Color.Black
        )
    }
}

private fun generateQRCode(content: String): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
