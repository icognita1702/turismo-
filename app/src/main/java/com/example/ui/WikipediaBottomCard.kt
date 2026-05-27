package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.HomeViewModel
import com.example.WikipediaState
import com.example.data.PoiType
import com.example.data.PointOfInterest
import com.example.data.WikipediaMonument
import com.example.ui.modifier.shimmer

/**
 * WikipediaBottomCard: Componente premium de feedback cultural de locais próximos.
 * Fica flutuando de forma elegante na parte inferior da tela com cantos arredondados acentuados
 * e um fundo translúcido (efeito de vidro esfumaçado).
 * Adapta-se dinamicamente para exibir informações refinadas de POIs próximos clicados no mapa.
 */
@Composable
fun WikipediaBottomCard(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    onLocationRequested: () -> Unit = {}
) {
    val wikipediaState by viewModel.wikipediaState.collectAsState()
    val selectedPoi by viewModel.selectedPoi.collectAsState()

    // Controla o índice do monumento selecionado para navegação do carrossel da Wikipédia
    var selectedIndex by remember(wikipediaState) { mutableIntStateOf(0) }

    // Fundo premium translúcido de vidro esfumaçado (glassmorphism) com bordas arredondadas e gradientes de luz
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.40f),
                        Color.White.copy(alpha = 0.10f)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            ),
        color = Color(0xF2FFFFFF).copy(alpha = 0.84f), // Esfumaçado translúcido premium
        shadowElevation = 8.dp
    ) {
        AnimatedContent(
            targetState = selectedPoi,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "poi_or_wikipedia_transition"
        ) { activePoi ->
            if (activePoi != null) {
                // UI de detalhe do Ponto de Interesse selecionado pelo usuário no mapa
                PoiDetailView(
                    poi = activePoi,
                    onDismiss = { viewModel.selectPoi(null) }
                )
            } else {
                // UI reativa padrão do Guia Cultural da Wikipédia
                AnimatedContent(
                    targetState = wikipediaState,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "wikipedia_state_transition"
                ) { state ->
                    when (state) {
                        is WikipediaState.Idle -> {
                            WikipediaIdleView {
                                onLocationRequested()
                            }
                        }
                        is WikipediaState.Loading -> {
                            WikipediaLoadingView()
                        }
                        is WikipediaState.Success -> {
                            val monuments = state.monuments
                            if (monuments.isNotEmpty()) {
                                // Limita o índice de forma ultra segura para evitar IndexOutOfBoundsException
                                val index = selectedIndex.coerceIn(0, monuments.lastIndex)
                                val currentMonument = monuments[index]
                                
                                WikipediaSuccessView(
                                    monument = currentMonument,
                                    currentIndex = index,
                                    totalCount = monuments.size,
                                    onPrevious = {
                                        if (selectedIndex > 0) selectedIndex-- else selectedIndex = monuments.lastIndex
                                    },
                                    onNext = {
                                        if (selectedIndex < monuments.lastIndex) selectedIndex++ else selectedIndex = 0
                                    }
                                )
                            } else {
                                WikipediaEmptyView()
                            }
                        }
                        is WikipediaState.Error -> {
                            WikipediaErrorView(
                                message = state.message,
                                onRetry = {
                                    // Tenta obter novamente o guia cultural reavaliando o estado
                                    val uiState = viewModel.uiState.value
                                    if (uiState is com.example.LocationState.Success) {
                                        viewModel.fetchWikipediaInfo(uiState.latitude, uiState.longitude, forceRefresh = true)
                                    } else {
                                        onLocationRequested()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Detalhe elegante do Ponto de Interesse (restaurante, bar ou atração) selecionado.
 */
@Composable
private fun PoiDetailView(
    poi: PointOfInterest,
    onDismiss: () -> Unit
) {
    val categoryLabel = when (poi.type) {
        PoiType.RESTAURANT -> "RESTAURANTE RECOMENDADO"
        PoiType.BAR -> "BAR OU PUB LOCAL"
        PoiType.VIEWPOINT -> "PONTO TURÍSTICO / ATRAÇÃO"
    }

    val categoryColor = when (poi.type) {
        PoiType.RESTAURANT -> Color(0xFFFF6F00) // Laranja restaurante
        PoiType.BAR -> Color(0xFF8E24AA)        // Violeta bar
        PoiType.VIEWPOINT -> Color(0xFF00ACC1)  // Turquesa atração
    }

    val categoryIcon = when (poi.type) {
        PoiType.RESTAURANT -> Icons.Default.Restaurant
        PoiType.BAR -> Icons.Default.LocalBar
        PoiType.VIEWPOINT -> Icons.Default.Explore
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Círculo com ícone do tipo de POI com cor adaptiva combinada
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(categoryColor.copy(alpha = 0.12f))
                .border(1.5.dp, categoryColor.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = categoryIcon,
                contentDescription = categoryLabel,
                tint = categoryColor,
                modifier = Modifier.size(24.dp)
            )
        }

        // Informações textuais perfeitamente escaneáveis
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = categoryLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.1.sp,
                    fontSize = 11.sp
                ),
                color = categoryColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = poi.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1D1B20),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Está localizado a exatamente ${String.format("%.0f", poi.distance)}m do seu sinal de GPS atual.",
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 14.sp),
                color = Color(0xFF49454F)
            )
        }

        // Botão para fechar o detalhamento e restaurar o carrossel normal da Wikipédia
        IconButton(
            onClick = onDismiss,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color(0xFFEADDFF).copy(alpha = 0.55f),
                contentColor = Color(0xFF21005D)
            ),
            modifier = Modifier.size(48.dp) // Touch target accessibility padrão de 48.dp
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Fechar detalhes do ponto"
            )
        }
    }
}

@Composable
private fun WikipediaIdleView(
    onRequestLocation: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFEADDFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = "Menu de turismo",
                tint = Color(0xFF21005D)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Guia Cultural Ativo",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1D1B20)
            )
            Text(
                text = "Carregue sua localização para revelar marcos e histórias próximas.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF49454F)
            )
        }
        IconButton(
            onClick = onRequestLocation,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color(0xFF6750A4),
                contentColor = Color.White
            ),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Carregar Local"
            )
        }
    }
}

@Composable
private fun WikipediaLoadingView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp)
    ) {
        // Shimmer do Cabeçalho "HISTÓRIA PRÓXIMA"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .shimmer()
                )
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                )
            }
            
            // Badge simulado
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmer()
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Conteúdo Principal simulado: Imagem e Parágrafos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Imagem do Monumento simulada
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .shimmer()
            )

            // Coluna textual simulada com linhas de larguras variadas
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Controles de Seleção simulados (Anterior e Próximo)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .shimmer()
            )
            
            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .shimmer()
            )
        }
    }
}

@Composable
private fun WikipediaSuccessView(
    monument: WikipediaMonument,
    currentIndex: Int,
    totalCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp)
    ) {
        // Cabeçalho de Navegação Inteligente (Carrossel)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Guia Inteligente",
                    tint = Color(0xFF6750A4),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "HISTÓRIA PRÓXIMA",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = Color(0xFF6750A4)
                )
            }
            
            // Badge com contador elegante do carrossel (ex: 2 / 5)
            Text(
                text = "${currentIndex + 1} / $totalCount",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                ),
                color = Color(0xFF49454F),
                modifier = Modifier
                    .background(Color(0xFFEADDFF), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Conteúdo Principal: Imagem e Informações
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Imagem do Monumento com Coil AsyncImage e tratamento premium de placeholder
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(16.dp))
                    .background(Color(0xFFEADDFF)),
                contentAlignment = Alignment.Center
            ) {
                if (monument.imageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(monument.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Imagem do monumento ${monument.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Placeholder de Cultura",
                        tint = Color(0xFF21005D),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Descrição e Resumo ultra escaneável
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = monument.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp
                    ),
                    color = Color(0xFF1D1B20),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = monument.summary,
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 15.sp
                    ),
                    color = Color(0xFF49454F),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Controles de Seleção (Anterior e Próximo)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, Color(0xFF79747E), CircleShape),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color(0xFF6750A4)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Ver monumento anterior"
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, Color(0xFF79747E), CircleShape),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color(0xFF6750A4)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Ver monumento seguinte"
                )
            }
        }
    }
}

@Composable
private fun WikipediaEmptyView() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "Nenhum local",
            tint = Color(0xFFBA1A1A),
            modifier = Modifier.size(32.dp)
        )
        Column {
            Text(
                text = "Sem Monumentos Próximos",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1D1B20)
            )
            Text(
                text = "Nenhum ponto histórico relevante foi encontrado no perímetro atual.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF49454F)
            )
        }
    }
}

@Composable
private fun WikipediaErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "Erro de conexão",
            tint = Color(0xFFBA1A1A),
            modifier = Modifier.size(32.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Erro no Guia Cultural",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFFBA1A1A)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFBA1A1A).copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onRetry,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color(0xFFBA1A1A),
                contentColor = Color.White
            ),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Tentar Novamente"
            )
        }
    }
}
