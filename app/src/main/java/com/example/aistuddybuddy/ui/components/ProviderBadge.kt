package com.example.aistuddybuddy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aistuddybuddy.domain.model.AiProvider

val AiProvider.brandColor: Color
    get() = when (this) {
        AiProvider.CHATGPT -> Color(0xFF10A37F)
        AiProvider.GEMINI -> Color(0xFF4285F4)
        AiProvider.DEEPSEEK -> Color(0xFF4D6BFE)
        AiProvider.OPENROUTER -> Color(0xFF6467F2)
    }

/** A simple round icon for each AI provider, tinted with its brand color. */
@Composable
fun ProviderBadge(provider: AiProvider, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Box(
        modifier = modifier
            .size(size)
            .background(provider.brandColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = provider.displayName.first().toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.5f).sp
        )
    }
}
