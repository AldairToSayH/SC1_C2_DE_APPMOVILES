# Conexión pendiente con Firebase

La aplicación, Google Maps, la validación y la capa de Realtime Database ya están implementadas.

## Datos que se usarán al registrar la aplicación

- Paquete Android: `com.example.examenunidad3`
- Archivo descargado: `google-services.json`
- Destino del archivo: `app/google-services.json`
- Nodo de Realtime Database: `markers`

Al colocar `google-services.json` en `app/`, el complemento de Google Services se activa automáticamente.

## Realtime Database

Crear la base de datos y pegar el contenido de `database.rules.json` en la pestaña **Reglas**. Estas reglas son públicas para fines del laboratorio; en un proyecto real debe agregarse autenticación.
