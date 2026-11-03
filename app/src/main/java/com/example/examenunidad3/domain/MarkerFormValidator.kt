package com.example.examenunidad3.domain

data class FormErrors(
    val title: String? = null,
    val description: String? = null,
    val latitude: String? = null,
    val longitude: String? = null
) {
    val hasErrors: Boolean
        get() = title != null || description != null || latitude != null || longitude != null
}

data class ValidationResult(
    val latitude: Double?,
    val longitude: Double?,
    val errors: FormErrors
)

object MarkerFormValidator {
    fun validate(
        title: String,
        description: String,
        latitudeText: String,
        longitudeText: String
    ): ValidationResult {
        val latitude = latitudeText.trim().replace(',', '.').toDoubleOrNull()
        val longitude = longitudeText.trim().replace(',', '.').toDoubleOrNull()

        return ValidationResult(
            latitude = latitude,
            longitude = longitude,
            errors = FormErrors(
                title = if (title.isBlank()) "Ingresa un título" else null,
                description = if (description.isBlank()) "Ingresa una descripción" else null,
                latitude = when {
                    latitudeText.isBlank() -> "Ingresa la latitud"
                    latitude == null -> "Usa un número válido"
                    latitude !in -90.0..90.0 -> "Debe estar entre -90 y 90"
                    else -> null
                },
                longitude = when {
                    longitudeText.isBlank() -> "Ingresa la longitud"
                    longitude == null -> "Usa un número válido"
                    longitude !in -180.0..180.0 -> "Debe estar entre -180 y 180"
                    else -> null
                }
            )
        )
    }
}
