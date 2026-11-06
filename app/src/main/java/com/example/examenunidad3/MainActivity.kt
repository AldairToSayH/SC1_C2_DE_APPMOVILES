package com.example.examenunidad3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.examenunidad3.data.createMarkerRepository
import com.example.examenunidad3.ui.MapScreen
import com.example.examenunidad3.ui.MapViewModel
import com.example.examenunidad3.ui.theme.Examenunidad3Theme
import com.google.android.gms.maps.MapsInitializer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        MapsInitializer.initialize(
            applicationContext,
            MapsInitializer.Renderer.LATEST,
            null
        )

        val repository = createMarkerRepository(applicationContext)

        setContent {
            Examenunidad3Theme(dynamicColor = false) {
                val factory = remember(repository) { MapViewModel.Factory(repository) }
                val mapViewModel: MapViewModel = viewModel(factory = factory)
                MapScreen(
                    viewModel = mapViewModel,
                    onCloseApp = ::finishAffinity
                )
            }
        }
    }
}
