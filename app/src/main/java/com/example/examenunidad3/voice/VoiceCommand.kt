package com.example.examenunidad3.voice

enum class VoiceAction {
    OPEN_FORM,
    OPEN_MARKERS,
    SHOW_MAP,
    FOCUS_MARKER,
    DELETE_MARKER,
    FILL_FORM,
    ADD_MARKER,
    SUBMIT_FORM,
    CLOSE_APP,
    HELP,
    UNKNOWN
}

enum class VoiceEngine {
    QWEN,
    LOCAL
}

data class VoiceCommand(
    val action: VoiceAction,
    val markerName: String? = null,
    val title: String? = null,
    val description: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val response: String = "",
    val engine: VoiceEngine = VoiceEngine.LOCAL
)
