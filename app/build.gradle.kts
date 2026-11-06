import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Firebase se activa automáticamente cuando se descarga el archivo desde la consola.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

val localProperties = Properties().apply {
    val propertiesFile = rootProject.file("local.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use(::load)
    }
}

fun String.asBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.example.examenunidad3"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.examenunidad3"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        manifestPlaceholders["MAPS_API_KEY"] =
            localProperties.getProperty("MAPS_API_KEY", "PENDIENTE_CONFIGURAR")

        buildConfigField(
            "String",
            "QWEN_API_KEY",
            localProperties.getProperty("QWEN_API_KEY", "").asBuildConfigString()
        )
        buildConfigField(
            "String",
            "QWEN_BASE_URL",
            localProperties.getProperty(
                "QWEN_BASE_URL",
                "https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions"
            ).asBuildConfigString()
        )
        buildConfigField(
            "String",
            "QWEN_MODEL",
            localProperties.getProperty("QWEN_MODEL", "qwen-plus").asBuildConfigString()
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.maps.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
