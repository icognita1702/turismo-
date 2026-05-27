package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.HomeViewModel
import com.example.LocationErrorType
import com.example.LocationState
import com.example.WikipediaState
import com.example.ui.theme.VibrantBackground
import com.example.ui.theme.VibrantOutline
import com.example.ui.theme.VibrantPrimary
import com.example.ui.theme.VibrantPrimaryContainer
import com.example.ui.theme.VibrantSurface
import com.example.ui.theme.VibrantSurfaceVariant

/**
 * HomeScreen com o tema "Vibrant Palette" aplicado com fidelidade extrema.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val wikipediaState by viewModel.wikipediaState.collectAsState()

    val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val hasFine = permissionsMap[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val hasCoarse = permissionsMap[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        viewModel.fetchCurrentLocation(hasFine || hasCoarse)
    }

    fun hasLocationPermissions(ctx: Context): Boolean {
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
               ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermissions(context)) {
            viewModel.fetchCurrentLocation(true)
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = VibrantBackground,
        bottomBar = {
            VibrantBottomNavigation()
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Cabeçalho alinhado à esquerda com Badge, Quadrado Místico e Clima
            VibrantHeaderSection(
                viewModel = viewModel,
                wikipediaState = wikipediaState
            )

            // 2. Área de Conteúdo Central Reativa
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState is LocationState.Loading,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut()
                ) {
                    VibrantCenterSignalSphere(
                        statusLabel = "Aguardando",
                        signalStrength = "Conectando...",
                        isLoading = true
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState is LocationState.Success,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut()
                ) {
                    val successState = uiState as? LocationState.Success
                    if (successState != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Círculo concêntrico dinâmico de sinal GPS
                            VibrantCenterSignalSphere(
                                statusLabel = "Sinal GPS",
                                signalStrength = "Excelente",
                                isLoading = false
                            )

                            // Cartão com design premium e borda do Vibrant Palette
                            VibrantCoordinatesCard(
                                latitude = successState.latitude,
                                longitude = successState.longitude
                            )

                            // Cartão com o mapa vetorial MapLibre e Guia Cultural Wikipédia flutuante integrado
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(455.dp)
                                    .border(1.dp, VibrantOutline, RoundedCornerShape(28.dp)),
                                shape = RoundedCornerShape(28.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = VibrantSurface
                                )
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    ComposableMap(
                                        latitude = successState.latitude,
                                        longitude = successState.longitude,
                                        viewModel = viewModel,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Badge indicador premium do tipo de renderização
                                    Box(
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .align(Alignment.TopEnd)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF21005D))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "VETOR GLOBAL",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 0.5.sp
                                            )
                                        )
                                    }

                                    // Guia Cultural Wikipédia flutuando de forma elegante na base do mapa
                                    WikipediaBottomCard(
                                        viewModel = viewModel,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(12.dp),
                                        onLocationRequested = {
                                            if (hasLocationPermissions(context)) {
                                                viewModel.fetchCurrentLocation(true)
                                            } else {
                                                permissionLauncher.launch(permissions)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState is LocationState.Error,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut()
                ) {
                    val errorState = uiState as? LocationState.Error
                    if (errorState != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Círculo indicando falha ou sinal cortado
                            VibrantCenterSignalSphere(
                                statusLabel = "Sinal GPS",
                                signalStrength = if (errorState.type == LocationErrorType.GPS_DISABLED) "Desligado" else "Sem Sinal",
                                isLoading = false,
                                isError = true
                            )

                            VibrantErrorCard(
                                message = errorState.message,
                                type = errorState.type,
                                onAction = {
                                    when (errorState.type) {
                                        LocationErrorType.PERMISSION_DENIED -> {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", context.packageName, null)
                                            }
                                            context.startActivity(intent)
                                        }
                                        LocationErrorType.GPS_DISABLED -> {
                                            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                            context.startActivity(intent)
                                        }
                                        LocationErrorType.GENERIC -> {
                                            if (hasLocationPermissions(context)) {
                                                viewModel.fetchCurrentLocation(true)
                                            } else {
                                                permissionLauncher.launch(permissions)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState is LocationState.Idle,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut()
                ) {
                    VibrantCenterSignalSphere(
                        statusLabel = "Satélite",
                        signalStrength = "Sintonizando",
                        isLoading = true
                    )
                }
            }

            // 3. Área inferior de botões, indicadores de etapa e rodapé
            VibrantActionControls(
                onConfirm = {
                    // Ação premium de confirmação do estágio operacional
                },
                onReload = {
                    if (hasLocationPermissions(context)) {
                        viewModel.fetchCurrentLocation(true)
                    } else {
                        permissionLauncher.launch(permissions)
                    }
                }
            )
        }
    }
}

/**
 * Cabeçalho altamente estilizado seguindo o padrão Vibrant do HTML com clima integrado.
 */
@Composable
fun VibrantHeaderSection(
    viewModel: HomeViewModel,
    wikipediaState: WikipediaState
) {
    val stepLabel = when (wikipediaState) {
        is WikipediaState.Success -> "ETAPA 05 / 05"
        is WikipediaState.Loading -> "ETAPA 05 / 05"
        else -> "ETAPA 05 / 05"
    }
    
    val titleLabel = when (wikipediaState) {
        is WikipediaState.Success -> "Guia Ativo"
        is WikipediaState.Loading -> "Processando..."
        else -> "Geo-Clima"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stepLabel,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.6.sp
                ),
                color = VibrantPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = titleLabel,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = VibrantBackground.let { Color(0xFF1D1B20) }
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Cartão de clima em tempo real
            WeatherHeaderCard(viewModel = viewModel)

            // Avatar circular direito do HTML (bg-[#EADDFF] com quadrado [#21005D])
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(VibrantPrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF21005D))
                )
            }
        }
    }
}

/**
 * Globus de satélite concêntrico com animação de pulsação infinita de alta performance
 */
@Composable
fun VibrantCenterSignalSphere(
    statusLabel: String,
    signalStrength: String,
    isLoading: Boolean,
    isError: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseSizeMultiplier by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_multiplier"
    )

    Box(
        modifier = Modifier
            .size(240.dp)
            .testTag("gps_signal_sphere"),
        contentAlignment = Alignment.Center
    ) {
        // Círculo 1: Aura externa pulsante suave
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = (if (isError) Color(0xFFFFDAD9) else Color(0xFFD0BCFF))
                        .copy(alpha = 0.15f * pulseSizeMultiplier),
                    shape = CircleShape
                )
        )

        // Círculo 2: Linha tracejada concêntrica de satélite
        val dashedStrokeColor = if (isError) Color(0xFFBA1A1A).copy(alpha = 0.3f) else VibrantPrimary.copy(alpha = 0.3f)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            drawCircle(
                color = dashedStrokeColor,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                )
            )
        }

        // Círculo 3: Esfera sólida com sombra e informações do sinal GPS
        val sphereBgColor = if (isError) Color(0xFFBA1A1A) else VibrantPrimary
        Column(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(sphereBgColor)
                .clickable { /* Feedbacks háticos */ }
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Color.White,
                    strokeWidth = 3.dp
                )
            } else {
                // Ícone concêntrico do Satélite
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = statusLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                ),
                color = Color.White.copy(alpha = 0.8f)
            )

            Text(
                text = signalStrength,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.2).sp
                ),
                color = Color.White
            )
        }
    }
}

/**
 * Cartão de latitude/longitude detalhado e estilizado baseado no Vibrant Design do HTML
 */
@Composable
fun VibrantCoordinatesCard(
    latitude: Double,
    longitude: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("location_success_card")
            .border(1.dp, VibrantOutline, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = VibrantSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Linha superior com Tag "LIVE" reativa
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Coordenadas Atuais",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF49454F)
                )

                // Tag LIVE brilhante do HTML
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(VibrantSurfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color(0xFF1D192B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Coordenadas dispostas simetricamente em colunas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LATITUDE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        ),
                        color = Color(0xFF49454F)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("%.4f", latitude),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color(0xFF1D1B20)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LONGITUDE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        ),
                        color = Color(0xFF49454F)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format("%.4f", longitude),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color(0xFF1D1B20)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Divider e informativo do FusedLocationProvider
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(VibrantOutline)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(VibrantPrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "i",
                        color = Color(0xFF21005D),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                Text(
                    text = "Localização capturada via FusedLocationProvider para máxima precisão.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        lineHeight = 16.sp
                    ),
                    color = Color(0xFF49454F)
                )
            }
        }
    }
}

/**
 * Cartão de erro premium com paleta de contraste adaptado
 */
@Composable
fun VibrantErrorCard(
    message: String,
    type: LocationErrorType,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFF2B8B5), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF9DEDC)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Erro de GPS",
                tint = Color(0xFFB3261E),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Pendência Detectada",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF31111D)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF31111D).copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB3261E),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (type == LocationErrorType.PERMISSION_DENIED) "Conceder Permissão" else "Ajustar Configurações",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

/**
 * Controles de rodapé dinâmicos e indicadores de etapa operacionais
 */
@Composable
fun VibrantActionControls(
    onConfirm: () -> Unit,
    onReload: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Botão Principal: Confirmar Localização (Vibrant amethyst h:56.dp)
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("confirm_location_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = VibrantPrimary,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(28.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Confirmar Localização",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "→",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Botão Secundário: Recarregar GPS (Borda premium h:48.dp)
        OutlinedButton(
            onClick = onReload,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("reload_gps_button"),
            border = borderStroke(2.dp, Color(0xFF79747E)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = VibrantPrimary
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                text = "Recarregar GPS",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = VibrantPrimary
                )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Indicador de Progresso de Páginas do Vibrant Theme (Pill shape ativa + bullet inativas)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 24.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(VibrantPrimary)
            )
            Box(
                modifier = Modifier
                    .size(width = 8.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(VibrantPrimaryContainer)
            )
            Box(
                modifier = Modifier
                    .size(width = 8.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(VibrantPrimaryContainer)
            )
        }
    }
}

/**
 * Barra de Navegação Horizontal customizada e responsiva com preenchimento seguro de insets
 */
@Composable
fun VibrantBottomNavigation() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color(0xFFF7F2FA))
            .border(0.5.dp, VibrantOutline)
            .navigationBarsPadding() // Diretriz imperativa de proteção contra sobreposição do sistema
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Aba Ativa: Mapa (bg-[#EADDFF] pill de destaque)
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { /* Navegação */ },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(VibrantPrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Mapa Ativo",
                    tint = Color(0xFF21005D),
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = "Mapa",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = Color(0xFF1D1B20)
            )
        }

        // Aba Inativa: Explorar
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { /* Navegação */ }
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Explorar",
                tint = Color(0xFF49454F).copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "Explorar",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = Color(0xFF49454F).copy(alpha = 0.5f)
            )
        }

        // Aba Inativa: Perfil
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { /* Navegação */ }
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Perfil",
                tint = Color(0xFF49454F).copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "Perfil",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = Color(0xFF49454F).copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Função helper para construir bordas customizadas sem cruzar dependências
 */
private fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = 
    androidx.compose.foundation.BorderStroke(width, color)
