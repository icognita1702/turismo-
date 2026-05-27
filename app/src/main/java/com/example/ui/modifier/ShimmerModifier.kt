package com.example.ui.modifier

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize

/**
 * Filtro de pintura e gradiente Shimmer para Skeleton Screens.
 * Cria um gradiente linear animado simétrico simulando reflexão de luz premium.
 */
fun Modifier.shimmer(
    visible: Boolean = true,
    colors: List<Color> = listOf(
        Color(0xFFE2E2E9).copy(alpha = 0.5f),
        Color(0xFFEEEEF5).copy(alpha = 0.9f),
        Color(0xFFE2E2E9).copy(alpha = 0.5f)
    )
): Modifier = composed {
    if (!visible) return@composed this

    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    
    // Anima o deslocamento horizontal do gradiente de -2x até +2x da largura da tela para um movimento suave
    val startOffsetX by transition.animateFloat(
        initialValue = -2f * (if (size.width > 0) size.width.toFloat() else 400f),
        targetValue = 2f * (if (size.width > 0) size.width.toFloat() else 400f),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset_animation"
    )

    val brush = if (size.width > 0 && size.height > 0) {
        Brush.linearGradient(
            colors = colors,
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    } else {
        Brush.linearGradient(
            colors = colors,
            start = Offset(0f, 0f),
            end = Offset(400f, 400f)
        )
    }

    background(brush = brush)
        .onGloballyPositioned {
            size = it.size
        }
}
