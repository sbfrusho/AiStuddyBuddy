package com.example.aistuddybuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.aistuddybuddy.ui.navigation.AppNavHost
import com.example.aistuddybuddy.ui.theme.AiStuddyBuddyTheme
import org.koin.androidx.compose.KoinAndroidContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KoinAndroidContext {
                AiStuddyBuddyTheme {
                    AppNavHost()
                }
            }
        }
    }
}
