# Conexión pendiente con Firebase

La aplicación, Google Maps, la validación y la capa de Realtime Database ya están implementadas.

## Datos que se usarán al registrar la aplicación

- Paquete Android: `com.example.examenunidad3`
- Archivo descargado: `google-services.json`
- Destino del archivo: `app/google-services.json`
- Nodo de Realtime Database: `markers`

Al colocar `google-services.json` en `app/`, el complemento de Google Services se activa automáticamente.
El archivo es local y está excluido de Git; cada desarrollador debe descargar su propia copia desde Firebase Console.

La clave de Google Maps debe agregarse únicamente a `local.properties`:

```properties
MAPS_API_KEY=TU_CLAVE_DE_GOOGLE_MAPS
```

`local.properties` también está excluido de Git.

## Asistente de voz con Qwen

Para interpretar lenguaje natural con Qwen, agrega también estas propiedades locales:

```properties
QWEN_API_KEY=TU_API_KEY_DE_MODEL_STUDIO
QWEN_MODEL=qwen-plus
QWEN_BASE_URL=https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions
```

Sin `QWEN_API_KEY`, la aplicación conserva un intérprete local de respaldo para los comandos básicos.

## Realtime Database

Crear la base de datos y pegar el contenido de `database.rules.json` en la pestaña **Reglas**. Estas reglas son públicas para fines del laboratorio; en un proyecto real debe agregarse autenticación.
