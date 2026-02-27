package com.radio.ccbes.ui.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.radio.ccbes.ui.screens.radio.RadioScreen
import com.radio.ccbes.ui.screens.search.ui.SearchScreen
import com.radio.ccbes.ui.screens.main.MainScreen
import com.radio.ccbes.ui.screens.auth.WelcomeScreen
import com.radio.ccbes.ui.screens.comments.CommentScreen
import com.radio.ccbes.ui.screens.home.HomeScreen
import com.radio.ccbes.ui.screens.notifications.NotificationsScreen
import com.radio.ccbes.ui.screens.news.NewsScreen
import com.radio.ccbes.ui.screens.post.CreatePostScreen
import com.radio.ccbes.ui.screens.profile.AboutUsScreen
import com.radio.ccbes.ui.screens.profile.AccountSettingsScreen
import com.radio.ccbes.ui.screens.profile.EditProfileScreen
import com.radio.ccbes.ui.screens.profile.ProfileScreen
import com.radio.ccbes.ui.screens.profile.ScannerScreen
import com.radio.ccbes.ui.screens.profile.ShareProfileScreen
import com.radio.ccbes.ui.screens.profile.TermsAndConditionsScreen
import com.radio.ccbes.ui.screens.chat.ChatListScreen
import com.radio.ccbes.ui.screens.chat.ChatScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    windowSizeClass: WindowSizeClass
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(navController = navController, windowSizeClass = windowSizeClass)
        }
        composable(Screen.Notifications.route) {
            NotificationsScreen(navController = navController, windowSizeClass = windowSizeClass)
        }
        composable(Screen.Search.route) {
            SearchScreen(navController = navController, windowSizeClass = windowSizeClass)
        }
        composable(Screen.News.route) {
            NewsScreen(navController = navController, windowSizeClass = windowSizeClass)
        }
        composable(Screen.Radio.route) {
            RadioScreen(windowSizeClass = windowSizeClass)
        }
        composable(
            route = Screen.Profile.route,
            arguments = listOf(navArgument("userId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            ProfileScreen(navController = navController, userId = userId)
        }
        composable(Screen.EditProfile.route) { EditProfileScreen(navController = navController) }
        composable(Screen.ShareProfile.route) { ShareProfileScreen(navController = navController) }
        composable(Screen.Scanner.route) { ScannerScreen(navController = navController) }
        composable(Screen.CreatePost.route) {
            CreatePostScreen(navController = navController)
        }
        composable(
            route = Screen.EditPost.route,
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            CreatePostScreen(navController = navController, postId = postId)
        }
        composable(
            route = Screen.Comments.route,
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            CommentScreen(navController = navController, postId = postId)
        }
        composable(Screen.AccountSettings.route) { AccountSettingsScreen(navController = navController) }
        composable(Screen.TermsAndConditions.route) { TermsAndConditionsScreen(navController = navController) }
        composable(Screen.AboutUs.route) { AboutUsScreen(navController = navController) }
        composable(Screen.ChatList.route) { ChatListScreen(navController = navController) }
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatScreen(navController = navController, chatId = chatId)
        }
        composable(
            route = Screen.PostDetail.route,
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            com.radio.ccbes.ui.screens.post.PostDetailScreen(navController = navController, postId = postId)
        }
    }
}
