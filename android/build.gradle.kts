// Top-level build file — plugins declared, applied par sous-module.
// Versions alignées sur Android Studio Panda 4 + Gradle 8.13 + Kotlin 2.1.
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.1.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10" apply false
    // Feature « Chaîne de Tehilim » : Supabase (supabase-kt + Ktor, depuis
    // mavenCentral) → aucun plugin Gradle à appliquer (contrairement à
    // l'ancien google-services de Firebase).
}
