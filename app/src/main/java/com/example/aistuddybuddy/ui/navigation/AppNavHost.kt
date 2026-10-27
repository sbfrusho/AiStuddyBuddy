package com.example.aistuddybuddy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aistuddybuddy.ui.screens.camera.CameraScreen
import com.example.aistuddybuddy.ui.screens.crop.CropScreen
import com.example.aistuddybuddy.ui.screens.questions.QuestionsScreen
import com.example.aistuddybuddy.ui.screens.questions.ScanViewModel
import com.example.aistuddybuddy.ui.screens.settings.SettingsScreen
import org.koin.androidx.compose.koinViewModel

object Routes {
    const val CAMERA = "camera"
    const val CROP = "crop"
    const val QUESTIONS = "questions"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    // Scoped to the activity so the scanned page and questions survive navigation.
    val scanViewModel: ScanViewModel = koinViewModel()

    val openSettings = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } }

    NavHost(navController = navController, startDestination = Routes.CAMERA) {
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
                onQuestionsExtracted = {
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
                onOpenSettings = openSettings
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
