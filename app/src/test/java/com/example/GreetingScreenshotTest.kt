package com.example

import android.app.Application
import android.location.Location
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.location.LocationTracker
import com.example.ui.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Teste Roborazzi / Robolectric de captura de tela de alta fidelidade para o Turismo Global.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

    @get:Rule 
    val composeTestRule = createComposeRule()

    @Test
    fun greeting_screenshot() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        
        // Define uma coordenada fixa e realista para testes determinísticos (ex: São Paulo, Brasil)
        val mockLocation = Location("gps").apply {
            latitude = -23.550520
            longitude = -46.633308
            accuracy = 1.0f
            time = System.currentTimeMillis()
        }

        // Provedor anônimo leve de localização para simular o GPS sem infraestruturas de rede
        val mockTracker = object : LocationTracker {
            override suspend fun getCurrentLocation(): Location {
                return mockLocation
            }
        }

        // Instancia o ViewModel com o tracker simulado
        val viewModel = HomeViewModel(application, mockTracker)
        
        // Dispara a busca simulando permissão concedida
        viewModel.fetchCurrentLocation(permissionsGranted = true)

        composeTestRule.setContent {
            MyApplicationTheme {
                HomeScreen(viewModel = viewModel)
            }
        }

        // Aguarda a estabilização completa do loop de Compose e das Coroutines
        composeTestRule.waitForIdle()

        // Captura a tela inteira em formato PNG de alta definição
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    }
}
