package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.HomeViewModel
import com.example.data.WeatherState
import com.example.ui.modifier.shimmer

/**
 * WeatherHeaderCard: Componente premium discreto para exibir clima em tempo real.
 * Apresenta o clima do local atual e se adapta a estados de carregamento ou erro.
 */
@Composable
fun WeatherHeaderCard(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val weatherState by viewModel.weatherState.collectAsState()

    AnimatedContent(
        targetState = weatherState,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "weather_state_animation",
        modifier = modifier
    ) { state ->
        when (state) {
            is WeatherState.Idle -> {
                // Estado neutro e sutil antes do sinal GPS ser ativado
                Box(modifier = Modifier.height(2.dp))
            }
            is WeatherState.Loading -> {
                // Skeleton Screen premium com efeito de brilho Shimmer pulsante
                Row(
                    modifier = Modifier
                        .width(185.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .shimmer()
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(18.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {}
            }
            is WeatherState.Success -> {
                val (weatherIcon, weatherDesc) = getWeatherDetails(state.weatherCode)

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFF7F2FA))
                        .border(1.5.dp, Color(0xFFEADDFF), RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Ícone minimalista sutil com cor de acentuação premium
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEADDFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = weatherIcon,
                            contentDescription = weatherDesc,
                            tint = Color(0xFF6750A4),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "${state.temperature.toInt()}°C",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp
                            ),
                            color = Color(0xFF21005D),
                            lineHeight = 16.sp
                        )
                        Text(
                            text = weatherDesc,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color(0xFF49454F).copy(alpha = 0.82f),
                            lineHeight = 10.sp
                        )
                    }
                }
            }
            is WeatherState.Error -> {
                // Tratamento suave de falhas, mantendo o layout limpo sem quebrar a UI
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFF9DEDC))
                        .border(1.dp, Color(0xFFB3261E), RoundedCornerShape(18.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "Clima indisponível",
                        tint = Color(0xFFB3261E),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Instável",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = Color(0xFFB3261E)
                    )
                }
            }
        }
    }
}

/**
 * Converte o código WMO da Open-Meteo em ícone representativo e legenda limpa.
 */
private fun getWeatherDetails(code: Int): Pair<ImageVector, String> {
    return when (code) {
        0 -> Pairs(Icons.Default.WbSunny, "Céu Limpo")
        1, 2, 3 -> Pairs(Icons.Default.Cloud, "Parcialmente Nublado")
        45, 48 -> Pairs(Icons.Default.Cloud, "Neblina")
        51, 53, 55 -> Pairs(Icons.Default.Grain, "Chuva Fraca")
        61, 63, 65 -> Pairs(Icons.Default.Umbrella, "Chuva")
        71, 73, 75 -> Pairs(Icons.Default.AcUnit, "Neve")
        80, 81, 82 -> Pairs(Icons.Default.Umbrella, "Pancadas de Chuva")
        95, 96, 99 -> Pairs(Icons.Default.Thunderstorm, "Tempestades")
        else -> Pairs(Icons.Default.WbSunny, "Clima Estável")
    }
}

// Classe de utilidade genérica para empacotamento simplificado
private fun <A, B> Pairs(first: A, second: B): Pair<A, B> = Pair(first, second)
