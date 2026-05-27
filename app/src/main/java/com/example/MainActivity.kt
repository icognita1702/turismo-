package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.location.DefaultLocationTracker
import com.example.ui.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.android.gms.location.LocationServices

/**
 * Ponto de entrada do aplicativo "Turismo Global".
 * Inicializa os provedores de rastreamento de satélites e acopla ao ciclo do Compose.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ativa o visual Edge-to-Edge sem bordas pretas nos botões de navegação
        enableEdgeToEdge()

        // Inicializa o cliente oficial de geoposicionamento de baixa latência do Google Play Services
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationTracker = DefaultLocationTracker(applicationContext, fusedLocationClient)

        // Inicializa o ViewModel utilizando o ciclo de vida do ComponentActivity e factory customizada
        val homeViewModel = ViewModelProvider(
            this,
            HomeViewModelFactory(application, locationTracker)
        )[HomeViewModel::class.java]

        setContent {
            MyApplicationTheme {
                HomeScreen(
                    viewModel = homeViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
