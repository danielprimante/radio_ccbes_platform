package com.radio.ccbes.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.radio.ccbes.ui.theme.RedAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutUsScreen(
    navController: NavController,
    viewModel: AboutUsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val config by viewModel.aboutConfig.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sobre Nosotros", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(180.dp),
                shape = CircleShape,
                color = Color.Transparent
            ) {
                if (config.logoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = config.logoUrl,
                        contentDescription = "Logo CCBES",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Image(
                        painter = painterResource(id = com.radio.ccbes.R.drawable.ic_logo_redondo),
                        contentDescription = "Logo CCBES",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                config.churchName.ifEmpty { "Iglesia CCBES" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Text(
                config.subText.ifEmpty { "Centro Cristiano Bienvenido Espiritu Santo" },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            ContactInfoItem(
                Icons.Default.LocationOn, 
                "Ubicación", 
                config.location.ifEmpty { "Calle 431, Mar del Plata, Argentina" }
            )
            ContactInfoItem(
                Icons.Default.Email, 
                "Correo Electrónico", 
                config.email.ifEmpty { "contacto@ccbes.org" }
            )
            ContactInfoItem(
                Icons.Default.Phone, 
                "Teléfono", 
                config.phone.ifEmpty { "+1 234 567 890" }
            )

            if (config.facebookUrl.isNotEmpty() || config.instagramUrl.isNotEmpty() || config.youtubeUrl.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "Síguenos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                ) {
                    if (config.facebookUrl.isNotEmpty()) {
                        SocialMediaButton("Facebook", config.facebookUrl)
                    }
                    if (config.instagramUrl.isNotEmpty()) {
                        SocialMediaButton("Instagram", config.instagramUrl)
                    }
                    if (config.youtubeUrl.isNotEmpty()) {
                        SocialMediaButton("YouTube", config.youtubeUrl)
                    }
                }
            }
        }
    }
}

@Composable
fun ContactInfoItem(icon: ImageVector, label: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = RedAccent, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = Color.Black)
        }
    }
}

@Composable
fun SocialMediaButton(platform: String, url: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Button(
        onClick = { 
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = RedAccent.copy(alpha = 0.1f), contentColor = RedAccent),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(platform, fontSize = 12.sp)
    }
}
