// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Añade estas dos líneas para Hilt y KSP:
    // KSP compatible con Kotlin 2.0.21
    id("com.google.devtools.ksp") version "2.0.21-1.0.26" apply false
    // Hilt actualizado para soportar Kotlin 2.0
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}