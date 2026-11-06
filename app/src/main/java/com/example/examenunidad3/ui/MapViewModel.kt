package com.example.examenunidad3.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.examenunidad3.data.MapLocation
import com.example.examenunidad3.data.MarkerRepository
import com.example.examenunidad3.domain.FormErrors
import com.example.examenunidad3.domain.MarkerFormValidator

data class MapUiState(
    val title: String = "",
    val description: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val errors: FormErrors = FormErrors(),
    val markers: List<MapLocation> = emptyList(),
    val selectedMarkerId: String? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isFirebaseConfigured: Boolean = false,
    val cameraTarget: MapLocation? = null,
    val message: String? = null
) {
    val selectedMarker: MapLocation?
        get() = markers.firstOrNull { it.id == selectedMarkerId }
}

class MapViewModel(
    private val repository: MarkerRepository
) : ViewModel() {
    var uiState by mutableStateOf(
        MapUiState(isFirebaseConfigured = repository.isConfigured)
    )
        private set

    init {
        repository.observe(
            onChanged = { locations ->
                uiState = uiState.copy(markers = locations, isLoading = false)
            },
            onError = { error ->
                uiState = uiState.copy(
                    isLoading = false,
                    message = "No se pudieron leer los marcadores: $error"
                )
            }
        )
    }

    fun updateTitle(value: String) {
        uiState = uiState.copy(title = value, errors = uiState.errors.copy(title = null))
    }

    fun updateDescription(value: String) {
        uiState = uiState.copy(
            description = value,
            errors = uiState.errors.copy(description = null)
        )
    }

    fun updateLatitude(value: String) {
        uiState = uiState.copy(latitude = value, errors = uiState.errors.copy(latitude = null))
    }

    fun updateLongitude(value: String) {
        uiState = uiState.copy(longitude = value, errors = uiState.errors.copy(longitude = null))
    }

    fun useMapCoordinates(latitude: Double, longitude: Double) {
        uiState = uiState.copy(
            latitude = latitude.toCoordinateText(),
            longitude = longitude.toCoordinateText(),
            errors = uiState.errors.copy(latitude = null, longitude = null)
        )
    }

    fun fillForm(
        title: String?,
        description: String?,
        latitude: Double?,
        longitude: Double?
    ) {
        uiState = uiState.copy(
            title = title ?: uiState.title,
            description = description ?: uiState.description,
            latitude = latitude?.toCoordinateText() ?: uiState.latitude,
            longitude = longitude?.toCoordinateText() ?: uiState.longitude,
            errors = FormErrors()
        )
    }

    fun showMessage(message: String) {
        uiState = uiState.copy(message = message)
    }

    fun addMarker() {
        if (!repository.isConfigured) {
            uiState = uiState.copy(
                message = "Falta conectar Firebase para poder guardar marcadores."
            )
            return
        }

        val validation = MarkerFormValidator.validate(
            title = uiState.title,
            description = uiState.description,
            latitudeText = uiState.latitude,
            longitudeText = uiState.longitude
        )
        if (validation.errors.hasErrors) {
            uiState = uiState.copy(errors = validation.errors)
            return
        }

        val location = MapLocation(
            title = uiState.title.trim(),
            description = uiState.description.trim(),
            latitude = requireNotNull(validation.latitude),
            longitude = requireNotNull(validation.longitude)
        )
        uiState = uiState.copy(isSaving = true)

        repository.add(location) { result ->
            result.onSuccess { savedLocation ->
                uiState = uiState.copy(
                    title = "",
                    description = "",
                    latitude = "",
                    longitude = "",
                    errors = FormErrors(),
                    isSaving = false,
                    selectedMarkerId = savedLocation.id,
                    cameraTarget = savedLocation,
                    message = "Ubicación guardada correctamente"
                )
            }.onFailure { error ->
                uiState = uiState.copy(
                    isSaving = false,
                    message = "No se pudo guardar: ${error.message.orEmpty()}"
                )
            }
        }
    }

    fun selectMarker(location: MapLocation) {
        uiState = uiState.copy(
            selectedMarkerId = location.id,
            cameraTarget = location
        )
    }

    fun clearSelection() {
        uiState = uiState.copy(selectedMarkerId = null)
    }

    fun deleteSelectedMarker() {
        val marker = uiState.selectedMarker ?: return
        deleteMarker(marker)
    }

    fun deleteMarker(marker: MapLocation) {
        repository.delete(marker.id) { result ->
            result.onSuccess {
                uiState = uiState.copy(
                    selectedMarkerId = if (uiState.selectedMarkerId == marker.id) {
                        null
                    } else {
                        uiState.selectedMarkerId
                    },
                    message = "Ubicación eliminada"
                )
            }.onFailure { error ->
                uiState = uiState.copy(
                    message = "No se pudo eliminar: ${error.message.orEmpty()}"
                )
            }
        }
    }

    fun consumeMessage() {
        uiState = uiState.copy(message = null)
    }

    override fun onCleared() {
        repository.stop()
    }

    class Factory(
        private val repository: MarkerRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MapViewModel::class.java))
            return MapViewModel(repository) as T
        }
    }
}

private fun Double.toCoordinateText(): String = String.format(java.util.Locale.US, "%.6f", this)
