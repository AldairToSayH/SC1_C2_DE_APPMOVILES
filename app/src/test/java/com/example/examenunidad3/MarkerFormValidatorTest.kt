package com.example.examenunidad3

import com.example.examenunidad3.domain.MarkerFormValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkerFormValidatorTest {
    @Test
    fun validPeruCoordinatesPassValidation() {
        val result = MarkerFormValidator.validate(
            title = "Plaza Mayor",
            description = "Centro histórico de Lima",
            latitudeText = "-12.0453",
            longitudeText = "-77.0311"
        )

        assertFalse(result.errors.hasErrors)
        assertEquals(-12.0453, result.latitude!!, 0.0)
        assertEquals(-77.0311, result.longitude!!, 0.0)
    }

    @Test
    fun commaDecimalSeparatorIsAccepted() {
        val result = MarkerFormValidator.validate("Lugar", "Detalle", "-12,1", "-77,2")

        assertFalse(result.errors.hasErrors)
        assertEquals(-12.1, result.latitude!!, 0.0)
    }

    @Test
    fun emptyAndOutOfRangeValuesProduceErrors() {
        val result = MarkerFormValidator.validate("", "", "91", "-181")

        assertTrue(result.errors.hasErrors)
        assertNotNull(result.errors.title)
        assertNotNull(result.errors.description)
        assertNotNull(result.errors.latitude)
        assertNotNull(result.errors.longitude)
    }
}
