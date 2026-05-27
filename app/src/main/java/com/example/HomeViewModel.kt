package com.example

import android.app.Application
import android.content.Context
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.WikipediaMonument
import com.example.data.PointOfInterest
import com.example.data.WeatherState
import com.example.location.LocationTracker
import com.example.network.WikipediaService
import com.example.network.OverpassService
import com.example.network.WeatherService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estados da UI para exibição limpa e gerenciamento de erros da geolocalização.
 */
sealed interface LocationState {
    object Idle : LocationState
    object Loading : LocationState
    data class Success(val latitude: Double, val longitude: Double) : LocationState
    data class Error(val message: String, val type: LocationErrorType) : LocationState
}

/**
 * Estados reativos para guiar a busca de dados culturais da API Wikipédia de geosearch.
 */
sealed interface WikipediaState {
    object Idle : WikipediaState
    object Loading : WikipediaState
    data class Success(val monuments: List<WikipediaMonument>) : WikipediaState
    data class Error(val message: String) : WikipediaState
}

/**
 * Estados reativos para os dados do OpenStreetMap obtidos da Overpass API.
 */
sealed interface OverpassState {
    object Idle : OverpassState
    object Loading : OverpassState
    data class Success(val pois: List<PointOfInterest>) : OverpassState
    data class Error(val message: String) : OverpassState
}

/**
 * Tipos detalhados de erros para exibição de mensagens amigáveis e ações personalizadas.
 */
enum class LocationErrorType {
    PERMISSION_DENIED,
    GPS_DISABLED,
    GENERIC
}

/**
 * ViewModel que gerencia de forma reativa a aquisição de localização, guias culturais e POIs do OSM.
 */
class HomeViewModel(
    application: Application,
    private val locationTracker: LocationTracker,
    private val wikipediaService: WikipediaService = WikipediaService(),
    private val overpassService: OverpassService = OverpassService(),
    private val weatherService: WeatherService = WeatherService()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<LocationState>(LocationState.Idle)
    val uiState: StateFlow<LocationState> = _uiState.asStateFlow()

    private val _wikipediaState = MutableStateFlow<WikipediaState>(WikipediaState.Idle)
    val wikipediaState: StateFlow<WikipediaState> = _wikipediaState.asStateFlow()

    private val _overpassState = MutableStateFlow<OverpassState>(OverpassState.Idle)
    val overpassState: StateFlow<OverpassState> = _overpassState.asStateFlow()

    private val _weatherState = MutableStateFlow<WeatherState>(WeatherState.Idle)
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    private val _selectedPoi = MutableStateFlow<PointOfInterest?>(null)
    val selectedPoi: StateFlow<PointOfInterest?> = _selectedPoi.asStateFlow()

    // Memória para evitar chamadas de API desnecessárias ao se deslocar menos de 50 metros
    private var lastSearchedLatitude: Double? = null
    private var lastSearchedLongitude: Double? = null

    /**
     * Tenta obter a localização atual após verificar permissões e estado do GPS.
     */
    fun fetchCurrentLocation(permissionsGranted: Boolean) {
        viewModelScope.launch {
            _uiState.value = LocationState.Loading

            if (!permissionsGranted) {
                _uiState.value = LocationState.Error(
                    message = "As permissões de localização foram negadas. Precisamos delas para mostrar sua geolocalização de turismo.",
                    type = LocationErrorType.PERMISSION_DENIED
                )
                return@launch
            }

            // Verifica se o provedor de localização GPS ou Rede está habilitado
            val context = getApplication<Application>()
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = try {
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            } catch (e: Exception) {
                false
            }

            if (!isGpsEnabled) {
                _uiState.value = LocationState.Error(
                    message = "Seu sinal de GPS parece estar desligado ou indisponível no momento.",
                    type = LocationErrorType.GPS_DISABLED
                )
                return@launch
            }

            val location = locationTracker.getCurrentLocation()
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                
                _uiState.value = LocationState.Success(
                    latitude = latitude,
                    longitude = longitude
                )
                
                // Dispara busca automatizada de informações climáticas, culturais e POIs para as novas coordenadas capturadas
                fetchWeatherInfo(latitude, longitude)
                fetchWikipediaInfo(latitude, longitude)
                fetchNearbyPoi(latitude, longitude)
            } else {
                _uiState.value = LocationState.Error(
                    message = "Não foi possível obter uma leitura de GPS válida com alta precisão no momento. Carregando dados da última localização conhecida...",
                    type = LocationErrorType.GENERIC
                )
            }
        }
    }

    /**
     * Dispara busca de informações históricas e culturais na Wikipédia de forma assíncrona.
     * Filtra requisições redundantes a menos de 50 metros para máxima economia de dados móveis.
     */
    fun fetchWikipediaInfo(latitude: Double, longitude: Double, forceRefresh: Boolean = false) {
        val lastLat = lastSearchedLatitude
        val lastLon = lastSearchedLongitude

        if (!forceRefresh && lastLat != null && lastLon != null) {
            val results = FloatArray(1)
            try {
                android.location.Location.distanceBetween(lastLat, lastLon, latitude, longitude, results)
                if (results[0] < 50.0f) {
                    // Sem alteração significativa de localização — evita chamadas redundantes de API
                    return
                }
            } catch (e: Exception) {
                // Tolerância a falhas na biblioteca nativa de localização durante testes simulados
            }
        }

        viewModelScope.launch {
            _wikipediaState.value = WikipediaState.Loading
            try {
                val monuments = wikipediaService.fetchNearbyCulturalGuide(latitude, longitude, radius = 1000)
                if (monuments.isEmpty()) {
                    _wikipediaState.value = WikipediaState.Error(
                        "Nenhum monumento ou ponto histórico de relevância cultural foi identificado nas proximidades deste local."
                    )
                } else {
                    lastSearchedLatitude = latitude
                    lastSearchedLongitude = longitude
                    _wikipediaState.value = WikipediaState.Success(monuments)
                }
            } catch (e: Exception) {
                _wikipediaState.value = WikipediaState.Error(
                    e.localizedMessage ?: "Ocorreu uma falha ao conectar-se à Wikipédia de turismo para carregar o guia cultural."
                )
            }
        }
    }

    /**
     * Dispara busca de pontos de interesse (POIs) na Overpass API baseado na localização do usuário.
     * Filtra requisições redundantes a menos de 50 metros para máxima economia de dados.
     */
    fun fetchNearbyPoi(latitude: Double, longitude: Double, forceRefresh: Boolean = false) {
        val lastLat = lastSearchedLatitude
        val lastLon = lastSearchedLongitude

        if (!forceRefresh && lastLat != null && lastLon != null) {
            val results = FloatArray(1)
            try {
                android.location.Location.distanceBetween(lastLat, lastLon, latitude, longitude, results)
                if (results[0] < 50.0f) {
                    // Sem alteração significativa de localização
                    return
                }
            } catch (e: Exception) {
                // Tolerância a falhas na biblioteca nativa de localização
            }
        }

        viewModelScope.launch {
            _overpassState.value = OverpassState.Loading
            try {
                val pois = overpassService.fetchNearbyPoi(latitude, longitude, radius = 1000)
                _overpassState.value = OverpassState.Success(pois)
            } catch (e: Exception) {
                _overpassState.value = OverpassState.Error(
                    e.localizedMessage ?: "Ocorreu uma falha ao comunicar-se à Overpass API para carregar pontos de interesse locais."
                )
            }
        }
    }

    /**
     * Define ou reseta o ponto de interesse atualmente selecionado no mapa.
     */
    fun selectPoi(poi: PointOfInterest?) {
        _selectedPoi.value = poi
    }

    /**
     * Dispara busca de informações climáticas na Open-Meteo API.
     */
    fun fetchWeatherInfo(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _weatherState.value = WeatherState.Loading
            try {
                val response = weatherService.fetchWeather(latitude, longitude)
                val current = response.currentWeather
                if (current != null) {
                    _weatherState.value = WeatherState.Success(
                        temperature = current.temperature,
                        weatherCode = current.weatherCode
                    )
                } else {
                    _weatherState.value = WeatherState.Error("Dados de clima indisponíveis no momento.")
                }
            } catch (e: Exception) {
                _weatherState.value = WeatherState.Error(
                    e.localizedMessage ?: "Não foi possível obter dados meteorológicos atuais."
                )
            }
        }
    }
}

/**
 * Factory robusta para instanciar a classe HomeViewModel com suas dependências necessárias.
 */
class HomeViewModelFactory(
    private val application: Application,
    private val locationTracker: LocationTracker,
    private val wikipediaService: WikipediaService = WikipediaService(),
    private val overpassService: OverpassService = OverpassService(),
    private val weatherService: WeatherService = WeatherService()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(
                application = application,
                locationTracker = locationTracker,
                wikipediaService = wikipediaService,
                overpassService = overpassService,
                weatherService = weatherService
            ) as T
        }
        throw IllegalArgumentException("Classe ViewModel desconhecida")
    }
}
