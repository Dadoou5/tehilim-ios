# Compose
-keep class androidx.compose.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Conservez nos modèles Codable
-keep,includedescriptorclasses class com.david.tehilim.core.model.** { *; }
-keepclassmembers class com.david.tehilim.core.model.** {
    *** Companion;
    *** INSTANCE;
}
