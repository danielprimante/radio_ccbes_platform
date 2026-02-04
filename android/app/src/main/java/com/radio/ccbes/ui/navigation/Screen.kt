package com.radio.ccbes.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector?, val navRoute: String = route) {
    object Home : Screen("home", "Inicio", Icons.Default.Home)
    object Notifications : Screen("notifications", "Notificaciones", null)
    object Search : Screen("search", "Buscar", Icons.Default.Search)
    object Radio : Screen("radio", "Radio", Icons.Filled.Radio)
    object Profile : Screen("profile?userId={userId}", "Perfil", Icons.Default.Person, "profile") {
        fun createRoute(userId: String? = null) = if (userId != null) "profile?userId=$userId" else "profile"
    }
    object Comments : Screen("comments/{postId}", "Comentarios", null) {
        fun createRoute(postId: String) = "comments/$postId"
    }
    object CreatePost : Screen("create_post", "Publicar", Icons.Default.Add)
    object ChatList : Screen("chat_list", "Mensajes", null)
    object Chat : Screen("chat/{chatId}", "Chat", null) {
        fun createRoute(chatId: String) = "chat/$chatId"
    }
    object EditPost : Screen("edit_post/{postId}", "Editar", null) {
        fun createRoute(postId: String) = "edit_post/$postId"
    }
    object Welcome : Screen("welcome", "Bienvenido", null)
    object EditProfile : Screen("edit_profile", "Editar Perfil", null)
    object ShareProfile : Screen("share_profile", "Compartir Perfil", null)
    object Scanner : Screen("scanner", "Escanear QR", null)
    object AccountSettings : Screen("account_settings", "Cuenta", null)
    object TermsAndConditions : Screen("terms_and_conditions", "Términos y Condiciones", null)
    object AboutUs : Screen("about_us", "Sobre Nosotros", null)
}
