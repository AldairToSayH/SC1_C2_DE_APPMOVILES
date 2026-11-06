package com.example.examenunidad3.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.examenunidad3.BuildConfig
import com.example.examenunidad3.data.MapLocation
import com.example.examenunidad3.voice.AndroidVoiceRecognizer
import com.example.examenunidad3.voice.AndroidVoiceSpeaker
import com.example.examenunidad3.voice.QwenVoiceInterpreter
import com.example.examenunidad3.voice.VoiceAction
import com.example.examenunidad3.voice.VoiceCommand
import com.example.examenunidad3.voice.VoiceEngine
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import java.text.Normalizer
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onCloseApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }
    var showForm by remember { mutableStateOf(false) }
    var showMarkerList by remember { mutableStateOf(false) }
    var showVoiceHelp by remember { mutableStateOf(false) }
    var markerPendingDelete by remember { mutableStateOf<MapLocation?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var isProcessingVoice by remember { mutableStateOf(false) }
    var pendingTranscript by remember { mutableStateOf<String?>(null) }
    var lastTranscript by remember { mutableStateOf("") }
    var lastVoiceEngine by remember { mutableStateOf<VoiceEngine?>(null) }

    val voiceInterpreter = remember {
        QwenVoiceInterpreter(
            apiKey = BuildConfig.QWEN_API_KEY,
            endpoint = BuildConfig.QWEN_BASE_URL,
            model = BuildConfig.QWEN_MODEL
        )
    }
    val voiceSpeaker = remember { AndroidVoiceSpeaker(context) }
    val voiceRecognizer = remember {
        AndroidVoiceRecognizer(
            context = context,
            onListeningChanged = { listening -> isListening = listening },
            onTranscript = { transcript ->
                lastTranscript = transcript
                pendingTranscript = transcript
            },
            onErrorMessage = viewModel::showMessage
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceRecognizer.start()
        } else {
            viewModel.showMessage("Se necesita permiso de micrófono para usar comandos de voz.")
        }
    }

    fun showOnlyMap() {
        showForm = false
        showMarkerList = false
        viewModel.clearSelection()
    }

    fun executeVoiceCommand(command: VoiceCommand) {
        lastVoiceEngine = command.engine
        when (command.action) {
            VoiceAction.OPEN_FORM -> {
                showMarkerList = false
                viewModel.clearSelection()
                showForm = true
            }

            VoiceAction.OPEN_MARKERS -> {
                showForm = false
                viewModel.clearSelection()
                showMarkerList = true
            }

            VoiceAction.SHOW_MAP -> showOnlyMap()

            VoiceAction.FOCUS_MARKER -> {
                val marker = state.markers.findByVoiceName(command.markerName)
                if (marker == null) {
                    viewModel.showMessage("No encontré ese marcador.")
                } else {
                    showForm = false
                    showMarkerList = false
                    viewModel.selectMarker(marker)
                }
            }

            VoiceAction.DELETE_MARKER -> {
                val marker = state.markers.findByVoiceName(command.markerName)
                if (marker == null) {
                    viewModel.showMessage("No encontré el marcador que deseas eliminar.")
                } else {
                    markerPendingDelete = marker
                }
            }

            VoiceAction.FILL_FORM -> {
                viewModel.fillForm(
                    title = command.title,
                    description = command.description,
                    latitude = command.latitude,
                    longitude = command.longitude
                )
                showMarkerList = false
                viewModel.clearSelection()
                showForm = true
            }

            VoiceAction.ADD_MARKER -> {
                viewModel.fillForm(
                    title = command.title,
                    description = command.description,
                    latitude = command.latitude,
                    longitude = command.longitude
                )
                showMarkerList = false
                viewModel.clearSelection()
                showForm = true
                viewModel.addMarker()
            }

            VoiceAction.SUBMIT_FORM -> {
                showForm = true
                viewModel.addMarker()
            }

            VoiceAction.CLOSE_APP -> {
                voiceSpeaker.speak(command.response.ifBlank { "Cerrando la aplicación" })
                Handler(Looper.getMainLooper()).postDelayed(onCloseApp, 800)
                return
            }

            VoiceAction.HELP -> showVoiceHelp = true

            VoiceAction.UNKNOWN -> viewModel.showMessage(
                command.response.ifBlank { "No entendí el comando. Di ayuda para ver ejemplos." }
            )
        }

        if (command.action != VoiceAction.UNKNOWN) {
            voiceSpeaker.speak(command.response)
            if (command.response.isNotBlank()) {
                viewModel.showMessage(command.response)
            }
        }
    }

    LaunchedEffect(pendingTranscript) {
        val transcript = pendingTranscript ?: return@LaunchedEffect
        pendingTranscript = null
        isProcessingVoice = true
        voiceInterpreter.interpret(
            transcript = transcript,
            markerTitles = state.markers.map(MapLocation::title)
        ) { result ->
            isProcessingVoice = false
            result.onSuccess(::executeVoiceCommand)
                .onFailure { error ->
                    viewModel.showMessage("No se pudo procesar el comando: ${error.message.orEmpty()}")
                }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceRecognizer.destroy()
            voiceInterpreter.close()
            voiceSpeaker.shutdown()
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(state.cameraTarget?.id) {
        if (showForm && !state.cameraTarget?.id.isNullOrBlank()) {
            showForm = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(contentPadding)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Header(
                markerCount = state.markers.size,
                onShowMarkers = { showMarkerList = true }
            )

            if (!state.isFirebaseConfigured) {
                ConfigurationWarning()
            }

            MapCard(
                state = state,
                modifier = Modifier.weight(1f),
                onMapTap = { latitude, longitude ->
                    viewModel.useMapCoordinates(latitude, longitude)
                    showForm = true
                },
                onMarkerTap = viewModel::selectMarker
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showForm = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Agregar ubicación")
                }
                OutlinedButton(
                    onClick = { showMarkerList = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Ver marcadores (${state.markers.size})")
                }
            }

            VoiceControl(
                isListening = isListening,
                isProcessing = isProcessingVoice,
                isQwenConfigured = voiceInterpreter.isQwenConfigured,
                lastTranscript = lastTranscript,
                lastEngine = lastVoiceEngine,
                onClick = {
                    if (isListening) {
                        voiceRecognizer.stop()
                    } else if (
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        voiceRecognizer.start()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            )
        }
    }

    when {
        showForm -> {
            ModalBottomSheet(onDismissRequest = { showForm = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp)
                ) {
                    MarkerForm(
                        state = state,
                        onTitleChange = viewModel::updateTitle,
                        onDescriptionChange = viewModel::updateDescription,
                        onLatitudeChange = viewModel::updateLatitude,
                        onLongitudeChange = viewModel::updateLongitude,
                        onAdd = viewModel::addMarker
                    )
                }
            }
        }

        showMarkerList -> {
            ModalBottomSheet(onDismissRequest = { showMarkerList = false }) {
                MarkerList(
                    markers = state.markers,
                    onView = { marker ->
                        showMarkerList = false
                        viewModel.selectMarker(marker)
                    },
                    onDelete = { marker -> markerPendingDelete = marker }
                )
            }
        }

        state.selectedMarker != null -> {
            ModalBottomSheet(onDismissRequest = viewModel::clearSelection) {
                SelectedMarkerContent(
                    marker = requireNotNull(state.selectedMarker),
                    onClose = viewModel::clearSelection,
                    onDelete = { markerPendingDelete = state.selectedMarker }
                )
            }
        }
    }

    markerPendingDelete?.let { marker ->
        AlertDialog(
            onDismissRequest = { markerPendingDelete = null },
            title = { Text("Eliminar ubicación") },
            text = { Text("¿Deseas eliminar \"${marker.title}\" de forma permanente?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        markerPendingDelete = null
                        viewModel.deleteMarker(marker)
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { markerPendingDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showVoiceHelp) {
        AlertDialog(
            onDismissRequest = { showVoiceHelp = false },
            title = { Text("Comandos de voz") },
            text = {
                Text(
                    "Prueba diciendo:\n\n" +
                        "• Abre nueva ubicación.\n" +
                        "• Muéstrame todos los marcadores.\n" +
                        "• Busca Plaza de Armas.\n" +
                        "• Elimina Sacsayhuamán.\n" +
                        "• Guarda el formulario.\n" +
                        "• Vuelve al mapa.\n" +
                        "• Cierra la aplicación."
                )
            },
            confirmButton = {
                TextButton(onClick = { showVoiceHelp = false }) { Text("Entendido") }
            }
        )
    }
}

@Composable
private fun VoiceControl(
    isListening: Boolean,
    isProcessing: Boolean,
    isQwenConfigured: Boolean,
    lastTranscript: String,
    lastEngine: VoiceEngine?,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !isProcessing
            ) {
                Text(
                    when {
                        isProcessing -> "Qwen está interpretando…"
                        isListening -> "Escuchando… Toca para detener"
                        else -> "🎙 Hablar con Qwen"
                    }
                )
            }
            Text(
                text = if (isQwenConfigured) {
                    "Qwen conectado"
                } else {
                    "Qwen sin API key · comandos básicos en modo local"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            if (lastTranscript.isNotBlank()) {
                val engine = when (lastEngine) {
                    VoiceEngine.QWEN -> "Qwen"
                    VoiceEngine.LOCAL -> "Local"
                    null -> "Procesando"
                }
                Text(
                    text = "Último comando ($engine): “$lastTranscript”",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun Header(
    markerCount: Int,
    onShowMarkers: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Mis ubicaciones",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "$markerCount ${if (markerCount == 1) "marcador registrado" else "marcadores registrados"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onShowMarkers) {
            Text("Ver todos")
        }
    }
}

@Composable
private fun ConfigurationWarning() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Text(
            text = "Falta agregar google-services.json para guardar en Firebase.",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun MarkerForm(
    state: MapUiState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Nueva ubicación",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Escribe las coordenadas o toca un punto en el mapa.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Título") },
                singleLine = true,
                isError = state.errors.title != null,
                supportingText = state.errors.title?.let { error -> { Text(error) } }
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Descripción") },
                minLines = 2,
                maxLines = 3,
                isError = state.errors.description != null,
                supportingText = state.errors.description?.let { error -> { Text(error) } }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CoordinateField(
                    value = state.latitude,
                    onValueChange = onLatitudeChange,
                    label = "Latitud",
                    error = state.errors.latitude,
                    modifier = Modifier.weight(1f)
                )
                CoordinateField(
                    value = state.longitude,
                    onValueChange = onLongitudeChange,
                    label = "Longitud",
                    error = state.errors.longitude,
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = onAdd,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !state.isSaving
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(if (state.isSaving) "Guardando…" else "Agregar marcador")
            }
        }
    }
}

@Composable
private fun CoordinateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = error != null,
        supportingText = error?.let { message -> { Text(message) } }
    )
}

@Composable
private fun MapCard(
    state: MapUiState,
    modifier: Modifier = Modifier,
    onMapTap: (Double, Double) -> Unit,
    onMarkerTap: (MapLocation) -> Unit
) {
    val cusco = LatLng(-13.5319, -71.9675)
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(cusco, 12f)
    }

    LaunchedEffect(state.cameraTarget) {
        state.cameraTarget?.let { location ->
            cameraState.animate(
                update = CameraUpdateFactory.newLatLngZoom(
                    LatLng(location.latitude, location.longitude),
                    15f
                ),
                durationMs = 500
            )
        }
    }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mapa",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Toca el mapa para agregar coordenadas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraState,
                    uiSettings = MapUiSettings(
                        compassEnabled = true,
                        zoomControlsEnabled = false,
                        scrollGesturesEnabled = true,
                        zoomGesturesEnabled = true
                    ),
                    onMapClick = { point -> onMapTap(point.latitude, point.longitude) }
                ) {
                    state.markers.forEach { location ->
                        key(location.id) {
                            Marker(
                                state = rememberUpdatedMarkerState(
                                    position = LatLng(location.latitude, location.longitude)
                                ),
                                title = location.title,
                                snippet = location.description,
                                onClick = {
                                    onMarkerTap(location)
                                    true
                                }
                            )
                        }
                    }
                }

                if (state.isLoading && state.isFirebaseConfigured) {
                    Surface(
                        modifier = Modifier.align(Alignment.Center),
                        shape = RoundedCornerShape(50),
                        tonalElevation = 6.dp
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkerList(
    markers: List<MapLocation>,
    onView: (MapLocation) -> Unit,
    onDelete: (MapLocation) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 650.dp)
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Todos mis marcadores",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${markers.size} ${if (markers.size == 1) "ubicación guardada" else "ubicaciones guardadas"}",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (markers.isEmpty()) {
            Text(
                text = "Todavía no tienes marcadores.",
                modifier = Modifier.padding(vertical = 28.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(markers, key = { it.id }) { marker ->
                    MarkerListItem(
                        marker = marker,
                        onView = { onView(marker) },
                        onDelete = { onDelete(marker) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkerListItem(
    marker: MapLocation,
    onView: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = marker.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(marker.description, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = marker.coordinatesText(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onView) { Text("Ver en mapa") }
                TextButton(onClick = onDelete) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun SelectedMarkerContent(
    marker: MapLocation,
    onClose: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = marker.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(marker.description, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = marker.coordinatesText(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onClose) { Text("Cerrar") }
            TextButton(onClick = onDelete) {
                Text("Eliminar ubicación", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun MapLocation.coordinatesText(): String = String.format(
    Locale.US,
    "%.6f, %.6f",
    latitude,
    longitude
)

private fun List<MapLocation>.findByVoiceName(name: String?): MapLocation? {
    val target = name?.voiceNormalized().orEmpty()
    if (target.isBlank()) return null

    return firstOrNull { marker ->
        val title = marker.title.voiceNormalized()
        title == target || title.contains(target) || target.contains(title)
    } ?: maxByOrNull { marker ->
        val titleWords = marker.title.voiceNormalized().split(' ').filter { it.length > 3 }
        titleWords.count(target::contains)
    }?.takeIf { marker ->
        marker.title.voiceNormalized().split(' ').filter { it.length > 3 }
            .count(target::contains) >= 1
    }
}

private fun String.voiceNormalized(): String = Normalizer
    .normalize(this, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
    .lowercase(Locale.ROOT)
    .trim()
