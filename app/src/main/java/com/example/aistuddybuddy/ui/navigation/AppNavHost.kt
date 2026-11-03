package com.example.aistuddybuddy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aistuddybuddy.domain.repository.SettingsRepository
import com.example.aistuddybuddy.ui.screens.bookmarks.BookmarksScreen
import com.example.aistuddybuddy.ui.screens.camera.CameraScreen
import com.example.aistuddybuddy.ui.screens.crop.CropScreen
import com.example.aistuddybuddy.ui.screens.questions.QuestionsScreen
import com.example.aistuddybuddy.ui.screens.questions.ScanViewModel
import com.example.aistuddybuddy.ui.screens.settings.SettingsScreen
import com.example.aistuddybuddy.ui.screens.welcome.WelcomeScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

object Routes {
    const val CAMERA = "camera"
    const val CROP = "crop"
    const val QUESTIONS = "questions"
    const val SETTINGS = "settings"
    const val BOOKMARKS = "bookmarks"
    const val WELCOME = "welcome"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    // Scoped to the activity so the scanned page and questions survive navigation.
    val scanViewModel: ScanViewModel = koinViewModel()
    val settingsRepository: SettingsRepository = koinInject()
    // Read once: changing it later must not swap the start destination under the user.
    val startDestination = remember {
        if (settingsRepository.preferences.value.onboardingDone) Routes.CAMERA else Routes.WELCOME
    }

    val openSettings = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.WELCOME) {
            WelcomeScreen(onDone = {
                navController.navigate(Routes.CAMERA) {
                    popUpTo(Routes.WELCOME) { inclusive = true }
                }
            })
        }
        composable(Routes.CAMERA) {
            CameraScreen(
                viewModel = scanViewModel,
                onImageReady = { navController.navigate(Routes.CROP) { launchSingleTop = true } },
                onOpenQuestions = { navController.navigate(Routes.QUESTIONS) { launchSingleTop = true } },
                onOpenSettings = openSettings
            )
        }
        composable(Routes.CROP) {
            CropScreen(
                viewModel = scanViewModel,
                onBack = { navController.popBackStack() },
                onScanStarted = {
                    navController.navigate(Routes.QUESTIONS) {
                        popUpTo(Routes.CAMERA)
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Routes.QUESTIONS) {
            QuestionsScreen(
                viewModel = scanViewModel,
                onScanMore = { navController.popBackStack(Routes.CAMERA, inclusive = false) },
                onOpenSettings = openSettings,
                onOpenBookmarks = { navController.navigate(Routes.BOOKMARKS) { launchSingleTop = true } }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.BOOKMARKS) {
            BookmarksScreen(onBack = { navController.popBackStack() })
        }
    }
}
