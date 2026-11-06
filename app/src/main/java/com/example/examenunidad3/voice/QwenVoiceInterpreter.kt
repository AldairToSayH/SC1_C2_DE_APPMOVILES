package com.example.examenunidad3.voice

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.Executors

class QwenVoiceInterpreter(
    private val apiKey: String,
    private val endpoint: String,
    private val model: String
) {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    val isQwenConfigured: Boolean
        get() = apiKey.isNotBlank()

    fun interpret(
        transcript: String,
        markerTitles: List<String>,
        callback: (Result<VoiceCommand>) -> Unit
    ) {
        executor.execute {
            val result = if (isQwenConfigured) {
                runCatching { requestQwen(transcript, markerTitles) }
                    .recoverCatching {
                        LocalVoiceInterpreter.interpret(transcript, markerTitles).copy(
                            response = "Qwen no respondió; ejecuté el comando en modo local."
                        )
                    }
            } else {
                runCatching { LocalVoiceInterpreter.interpret(transcript, markerTitles) }
            }
            mainHandler.post { callback(result) }
        }
    }

    fun close() {
        executor.shutdownNow()
    }

    private fun requestQwen(
        transcript: String,
        markerTitles: List<String>
    ): VoiceCommand {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12_000
            readTimeout = 18_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }

        val requestBody = JSONObject()
            .put("model", model)
            .put("temperature", 0)
            .put(
                "messages",
                JSONArray()
                    .put(
                        JSONObject()
                            .put("role", "system")
                            .put("content", SYSTEM_PROMPT)
                    )
                    .put(
                        JSONObject()
                            .put("role", "user")
                            .put(
                                "content",
                                "Comando de voz: $transcript\n" +
                                    "Marcadores disponibles: ${markerTitles.joinToString()}"
                            )
                    )
            )

        connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(requestBody.toString())
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }
        val responseBody = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        connection.disconnect()

        if (responseCode !in 200..299) {
            error("Qwen HTTP $responseCode: ${responseBody.take(180)}")
        }

        val content = JSONObject(responseBody)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")

        return parseCommand(extractJson(content)).copy(engine = VoiceEngine.QWEN)
    }

    private fun extractJson(content: String): JSONObject {
        val start = content.indexOf('{')
        val end = content.lastIndexOf('}')
        require(start >= 0 && end > start) { "Qwen no devolvió un objeto JSON" }
        return JSONObject(content.substring(start, end + 1))
    }

    private fun parseCommand(json: JSONObject): VoiceCommand {
        val action = runCatching {
            VoiceAction.valueOf(json.optString("action").uppercase(Locale.ROOT))
        }.getOrDefault(VoiceAction.UNKNOWN)

        return VoiceCommand(
            action = action,
            markerName = json.optionalString("markerName"),
            title = json.optionalString("title"),
            description = json.optionalString("description"),
            latitude = json.optionalDouble("latitude"),
            longitude = json.optionalDouble("longitude"),
            response = json.optString("response", "Comando procesado"),
            engine = VoiceEngine.QWEN
        )
    }

    private fun JSONObject.optionalString(name: String): String? =
        optString(name).trim().takeIf { it.isNotBlank() && it != "null" }

    private fun JSONObject.optionalDouble(name: String): Double? =
        if (has(name) && !isNull(name)) optDouble(name).takeUnless { it.isNaN() } else null

    private companion object {
        const val SYSTEM_PROMPT = """
Eres el controlador de voz de una aplicación Android de marcadores geográficos.
Interpreta español natural y responde exclusivamente con un objeto JSON válido, sin Markdown.

Acciones permitidas:
- OPEN_FORM: abrir formulario para una ubicación.
- OPEN_MARKERS: mostrar todos los marcadores.
- SHOW_MAP: cerrar paneles y mostrar el mapa.
- FOCUS_MARKER: localizar un marcador existente; usa markerName.
- DELETE_MARKER: solicitar eliminar un marcador; usa markerName. La app pedirá confirmación.
- FILL_FORM: completar los datos sin guardar.
- ADD_MARKER: completar y guardar; requiere title, description, latitude y longitude.
- SUBMIT_FORM: guardar el formulario que ya está completado.
- CLOSE_APP: cerrar la aplicación.
- HELP: explicar comandos disponibles.
- UNKNOWN: no se entiende o queda fuera de las capacidades.

Esquema exacto:
{"action":"OPEN_FORM","markerName":null,"title":null,"description":null,"latitude":null,"longitude":null,"response":"respuesta breve en español"}

No inventes coordenadas ni nombres. Para eliminar, nunca confirmes tú: devuelve DELETE_MARKER.
"""
    }
}

internal object LocalVoiceInterpreter {
    fun interpret(transcript: String, markerTitles: List<String>): VoiceCommand {
        val normalized = transcript.normalized()
        val matchedMarker = markerTitles.firstOrNull { title ->
            val normalizedTitle = title.normalized()
            normalized.contains(normalizedTitle) ||
                normalizedTitle.split(' ').filter { it.length > 3 }.count(normalized::contains) >= 2
        }

        return when {
            ("cerr" in normalized || "cierr" in normalized || "salir" in normalized) &&
                ("aplicacion" in normalized || "app" in normalized) ->
                VoiceCommand(
                    action = VoiceAction.CLOSE_APP,
                    response = "Cerrando la aplicación"
                )

            ("elimin" in normalized || "borr" in normalized) ->
                VoiceCommand(
                    action = VoiceAction.DELETE_MARKER,
                    markerName = matchedMarker ?: transcript.substringAfterCommand(),
                    response = "Preparando la confirmación de eliminación"
                )

            ("ver" in normalized || "mostr" in normalized || "muestr" in normalized ||
                "lista" in normalized) &&
                ("marcadores" in normalized || "ubicaciones" in normalized || "todos" in normalized) ->
                VoiceCommand(
                    action = VoiceAction.OPEN_MARKERS,
                    response = "Mostrando todos tus marcadores"
                )

            matchedMarker != null &&
                ("busc" in normalized || "mostr" in normalized || "muestr" in normalized ||
                    "ir" in normalized || "ubic" in normalized || "ver" in normalized) ->
                VoiceCommand(
                    action = VoiceAction.FOCUS_MARKER,
                    markerName = matchedMarker,
                    response = "Mostrando $matchedMarker"
                )

            "guard" in normalized || "registr" in normalized ->
                VoiceCommand(
                    action = VoiceAction.SUBMIT_FORM,
                    response = "Guardando la ubicación"
                )

            ("agreg" in normalized || "nuev" in normalized || "crea" in normalized ||
                "abr" in normalized) &&
                ("ubicacion" in normalized || "marcador" in normalized || "lugar" in normalized) ->
                VoiceCommand(
                    action = VoiceAction.OPEN_FORM,
                    response = "Abriendo el formulario de ubicación"
                )

            "mapa" in normalized || "volver" in normalized ->
                VoiceCommand(
                    action = VoiceAction.SHOW_MAP,
                    response = "Mostrando el mapa"
                )

            "ayuda" in normalized || "comandos" in normalized || "que puedes hacer" in normalized ->
                VoiceCommand(
                    action = VoiceAction.HELP,
                    response = "Mostrando los comandos disponibles"
                )

            else -> VoiceCommand(
                action = VoiceAction.UNKNOWN,
                response = "No entendí el comando. Di ayuda para ver ejemplos."
            )
        }
    }

    private fun String.substringAfterCommand(): String =
        replace(Regex("(?i)^(elimina(r)?|borra(r)?)\\s+(el|la|marcador|ubicación|ubicacion)?\\s*"), "")
            .trim()
            .takeIf(String::isNotBlank)
            ?: this

    private fun String.normalized(): String = Normalizer
        .normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .trim()
}
