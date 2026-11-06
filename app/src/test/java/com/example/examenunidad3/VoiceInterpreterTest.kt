package com.example.examenunidad3

import com.example.examenunidad3.voice.LocalVoiceInterpreter
import com.example.examenunidad3.voice.VoiceAction
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceInterpreterTest {
    private val markers = listOf(
        "Universidad Continental - Campus Cusco",
        "Plaza de Armas del Cusco",
        "Sacsayhuamán"
    )

    @Test
    fun opensMarkerList() {
        val command = LocalVoiceInterpreter.interpret(
            "Muéstrame todos los marcadores",
            markers
        )

        assertEquals(VoiceAction.OPEN_MARKERS, command.action)
    }

    @Test
    fun focusesExistingMarker() {
        val command = LocalVoiceInterpreter.interpret("Busca Plaza de Armas", markers)

        assertEquals(VoiceAction.FOCUS_MARKER, command.action)
        assertEquals("Plaza de Armas del Cusco", command.markerName)
    }

    @Test
    fun deletionRequiresUiConfirmationAction() {
        val command = LocalVoiceInterpreter.interpret("Elimina Sacsayhuamán", markers)

        assertEquals(VoiceAction.DELETE_MARKER, command.action)
        assertEquals("Sacsayhuamán", command.markerName)
    }

    @Test
    fun closesApplication() {
        val command = LocalVoiceInterpreter.interpret("Cierra la aplicación", markers)

        assertEquals(VoiceAction.CLOSE_APP, command.action)
    }
}
