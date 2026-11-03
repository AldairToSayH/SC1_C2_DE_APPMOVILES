package com.example.examenunidad3.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

interface MarkerRepository {
    val isConfigured: Boolean

    fun observe(
        onChanged: (List<MapLocation>) -> Unit,
        onError: (String) -> Unit
    )

    fun add(location: MapLocation, onResult: (Result<MapLocation>) -> Unit)

    fun delete(id: String, onResult: (Result<Unit>) -> Unit)

    fun stop()
}

class FirebaseMarkerRepository(
    database: FirebaseDatabase = FirebaseDatabase.getInstance()
) : MarkerRepository {
    override val isConfigured = true
    private val markersReference = database.getReference("markers")
    private var listener: ValueEventListener? = null

    override fun observe(
        onChanged: (List<MapLocation>) -> Unit,
        onError: (String) -> Unit
    ) {
        listener?.let(markersReference::removeEventListener)
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val locations = snapshot.children.mapNotNull { child ->
                    child.getValue(MapLocation::class.java)?.apply {
                        if (id.isBlank()) id = child.key.orEmpty()
                    }
                }
                onChanged(locations)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }.also(markersReference::addValueEventListener)
    }

    override fun add(location: MapLocation, onResult: (Result<MapLocation>) -> Unit) {
        val newReference = markersReference.push()
        val id = newReference.key
        if (id == null) {
            onResult(Result.failure(IllegalStateException("No se pudo generar el identificador")))
            return
        }

        val locationWithId = location.copy(id = id)
        newReference.setValue(locationWithId)
            .addOnSuccessListener { onResult(Result.success(locationWithId)) }
            .addOnFailureListener { error -> onResult(Result.failure(error)) }
    }

    override fun delete(id: String, onResult: (Result<Unit>) -> Unit) {
        markersReference.child(id).removeValue()
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { error -> onResult(Result.failure(error)) }
    }

    override fun stop() {
        listener?.let(markersReference::removeEventListener)
        listener = null
    }
}

private class UnconfiguredMarkerRepository : MarkerRepository {
    override val isConfigured = false

    override fun observe(
        onChanged: (List<MapLocation>) -> Unit,
        onError: (String) -> Unit
    ) = onChanged(emptyList())

    override fun add(location: MapLocation, onResult: (Result<MapLocation>) -> Unit) {
        onResult(Result.failure(IllegalStateException(CONFIGURATION_MESSAGE)))
    }

    override fun delete(id: String, onResult: (Result<Unit>) -> Unit) {
        onResult(Result.failure(IllegalStateException(CONFIGURATION_MESSAGE)))
    }

    override fun stop() = Unit
}

fun createMarkerRepository(context: Context): MarkerRepository {
    val firebaseApp = runCatching { FirebaseApp.initializeApp(context) }.getOrNull()
    return if (firebaseApp != null) {
        FirebaseMarkerRepository()
    } else {
        UnconfiguredMarkerRepository()
    }
}

private const val CONFIGURATION_MESSAGE =
    "Firebase aún no está configurado. Agrega app/google-services.json."
