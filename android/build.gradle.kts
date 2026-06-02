// Top-level build file — plugins declared, applied par sous-module.
// Versions alignées sur Android Studio Panda 4 + Gradle 8.13 + Kotlin 2.1.
plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.1.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10" apply false
    // Base de données : Supabase (aucun plugin Gradle). FCM (messagerie push,
    // notifications de chaîne) réintroduit le plugin google-services, appliqué
    // conditionnellement dans :app si google-services.json est présent.
    id("com.google.gms.google-services") version "4.4.2" apply false
}
