package com.radio.ccbes.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndConditionsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Términos y Condiciones", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Términos y Condiciones de Uso",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                "Bienvenido a Radio CCBES. Al utilizar nuestra aplicación, usted acepta los siguientes términos y condiciones:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Section("1. Uso del Contenido", "Todo el contenido proporcionado en esta aplicación, incluyendo audio de radio, reflexiones y publicaciones, es para uso personal y espiritual. Queda prohibida la reproducción comercial sin autorización.")
            
            Section("2. Conducta del Usuario", "Los usuarios se comprometen a interactuar de manera respetuosa en las secciones de comentarios y publicaciones. El contenido ofensivo, discriminatorio o spam será eliminado y podrá resultar en la suspensión de la cuenta.")
            
            Section("3. Privacidad", "Respetamos su privacidad. Sus datos personales se utilizan exclusivamente para mejorar su experiencia en la aplicación y no serán compartidos con terceros sin su consentimiento.")
            
            Section("4. Notificaciones", "Al habilitar las notificaciones push, usted acepta recibir actualizaciones sobre nuestra programación, noticias y reflexiones diarias.")
            
            Section("5. Modificaciones", "Nos reservamos el derecho de modificar estos términos en cualquier momento. El uso continuado de la aplicación constituye la aceptación de los nuevos términos.")

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "Última actualización: Enero 2026",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun Section(title: String, content: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
        Text(content, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
    }
}
