import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// V1.4 — credentials du keystore release lus depuis `keystore.properties`
// (non commité, voir .gitignore). Si absent (CI ou nouveau clone), la
// signature de release est désactivée — ne pas uploader sur le Play Store
// dans ce cas, le bundle resterait non signé.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

android {
    namespace = "com.david.tehilim"
    compileSdk = 36

    defaultConfig {
        // V1.4 — `applicationId` (Play Store identifier) diffère du
        // `namespace` Kotlin/Android (`com.david.tehilim`) : Google sépare
        // les deux exprès. Le code Kotlin garde son organisation
        // historique, `app.tehilim` est notre namespace public sur le
        // Play Store. Voir build.gradle.kts namespace ci-dessus.
        applicationId = "app.tehilim"
        minSdk = 26              // Android 8.0 — 95%+ devices, RTL natif solide
        targetSdk = 36           // Android 16 (Baklava) — recommandé Play Store 2026
        // V1.4 — premier upload Play Store. versionCode s'incrémente à
        // chaque build envoyé au store. versionName V1.0.0 marque le
        // lancement public ; le suivi dev interne (V1.3.12) reste dans
        // README et release notes.
        //
        // Build 1 : upload initial rejeté côté package (com.david.tehilim pris)
        // Build 2 : fix package → app.tehilim + mode sombre + cycle hébraïque
        // Build 3 : fix onboarding non-affiché (lecture synchrone DataStore)
        // Build 4 : fix onboarding edge-to-edge (boutons sous barres système)
        // Build 5 : widget responsive 3 tailles (mirror iOS WidgetKit)
        // Build 6 : labels visibles barre du bas + taille de police adaptative
        // Build 7 : fix navigation depuis widget/notif (singleTop + onNewIntent
        //           + findStartDestination pour la bottom bar)
        versionCode = 7
        versionName = "1.0.0"

        // Tests
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        // RTL pour Hébreu — supportRtl est dans le manifest mais on aligne ici.
        resourceConfigurations += listOf("fr", "en", "iw")
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            // Signature explicite avec la clé de upload Play Store. Google Play
            // re-signe ensuite avec la « app signing key » qu'il gère.
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // V1.4 — embarque les symboles de débogage natifs (libs
            // androidx.graphics.path + androidx.datastore packagées en .so)
            // pour que Play Console puisse symboliser les stack traces.
            // Sans ça, avertissement « code natif sans symboles » à l'upload.
            // Ajoute ~5 MB au AAB mais améliore le diagnostic des crashes.
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/LICENSE*"
            )
        }
    }
}

dependencies {
    // Compose BOM — version unique pour tout l'écosystème Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose UI + Material 3
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // Lifecycle / ViewModel
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    // DataStore (preferences)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Serialization JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // KotlinX DateTime (Hebrew calendar utilities)
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    // Splash screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // AppCompat — pour AppCompatDelegate.setApplicationLocales (per-app language)
    implementation("androidx.appcompat:appcompat:1.7.0")

    // WorkManager — notifications quotidiennes
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Glance — widget Compose-like
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")

    // Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // Compose tooling (debug only)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// ─────────────────────────────────────────────────────────────────────────────
// Task : copie les JSON partagés depuis /data vers assets/data au build
// ─────────────────────────────────────────────────────────────────────────────
val copySharedAssets = tasks.register<Copy>("copySharedAssets") {
    description = "Copie les JSONs partagés iOS↔Android depuis ../data vers assets/data"
    from("$rootDir/../data") {
        include("*.json", "*.schema.json")
    }
    into("$projectDir/src/main/assets/data")
}

val copySharedFonts = tasks.register<Copy>("copySharedFonts") {
    description = "Copie la police Ezra SIL SR depuis le projet iOS vers res/font"
    from("$rootDir/../ios/Tehilim/Resources/Fonts") {
        include("SILEOTSR.ttf")
        rename { "ezra_sil_sr.ttf" }
    }
    into("$projectDir/src/main/res/font")
}

tasks.named("preBuild").configure {
    dependsOn(copySharedAssets, copySharedFonts)
}
