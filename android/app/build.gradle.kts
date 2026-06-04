import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Feature « Chaîne de Tehilim » : config Supabase lue depuis
// `app/supabase.properties` (gitignoré) et exposée à l'app via BuildConfig.
// Absente (CI, nouveau clone) → URL/clé vides → l'app reste 100 % locale.
val supabaseProperties = Properties().apply {
    val f = file("supabase.properties")
    if (f.exists()) load(FileInputStream(f))
}

// FCM (messagerie push — notifications de chaîne) : applique google-services
// UNIQUEMENT si google-services.json est présent → build vert sans config
// (l'init FCM est gardée au runtime). La base de données reste Supabase.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
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
        // Build 8 : date hébraïque alignée à gauche (mirror iOS) — RTL via Unicode
        // Build 9 : fix double-routing deep link au cold-start (back stack
        //           dupliquée [home, daily, home, daily] → bouton Accueil cassé)
        // Build 10 : Home tap brute-force (popBackStack + fallback navigate
        //            avec popUpTo graph.id inclusive) — robuste à toutes
        //            les configurations de back stack
        // Build 11 : chips widget — hauteur intrinsèque + bump font hébreu
        //            (caractères tronqués sous 10sp dans Glance)
        // Build 12 : skip Compose splash quand cold-start via deep link
        //            (widget/notif) — accès direct au contenu
        // Build 13 : grille Tehilim 119 en sens de lecture hébreu —
        //            Aleph en haut à droite (parité avec iOS, RTL natif)
        // Build 14 : formulaire Lelouy Nichmat — labels alignés à droite +
        //            IME chaîné (Suivant → champ mère, Terminé → ferme
        //            clavier), scroll vertical + imePadding pour que le
        //            clavier soft ne masque jamais les champs
        // Build 15 : feature Commémoration (azcara) — date du décès
        //            optionnelle, calcul de la prochaine azcara par règles
        //            traditionnelles (Adar/Adar II, Heshvan 30, Kislev 30,
        //            Adar I 30), rappels J-7 + jour J via WorkManager
        // Build 16 : bump Glance 1.1.0 → 1.1.1 (warning Play Console
        //            « note cruciale » sur glance-appwidget-proto et
        //            external-protobuf) + activity-compose 1.9.2 → 1.9.3
        //            (gestion edge-to-edge SDK 35+ améliorée)
        // Build 17 : fix bug rappel quotidien non sauvegardé. Le toggle
        //            et l'heure étaient en state local du Composable
        //            (remember mutableStateOf) au lieu d'être persistés
        //            dans Datastore. Ajout des 3 prefs notif.enabled /
        //            hour / minute + refacto Composable pour lire/écrire
        //            via Flow + setters.
        // Build 18 : feature Partage/import d'une prière (lien tehilim://prayer
        //            → SMS/WhatsApp, aperçu + confirmation à l'import, dédup)
        //            + Modification d'une prière existante (formulaire pré-
        //            rempli réutilisé en mode édition, mise à jour en place,
        //            re-planification des rappels).
        // Build 19 : feature Mode Chabbat. Pendant Chabbat l'app et le widget
        //            affichent « Chabbat Chalom » (animé) à la place du
        //            contenu. Début (bougies, coucher −18 min) / fin (Havdala,
        //            coucher +42 min) calculés selon la position (GPS coarse,
        //            repli ville) via ShabbatCalculator (NOAA). Échappatoire
        //            « continuer quand même ». Réglage on/off + choix ville.
        // Build 20 : raffinements i18n + Chabbat. Fin de Chabbat = sortie des
        //            étoiles (Tzeit 8,5°, défaut Hebcal) selon la position
        //            (repli coucher+72 min aux hautes latitudes). Écran Chabbat
        //            affiche début ET fin. Repli anglais si langue système ≠
        //            fr/en. Champ hébreu Lelouy Nichmat aligné à droite.
        // Build 21 : lien de partage de prière cliquable partout (Mail/WhatsApp)
        //            via une URL https (page de redirection GitHub Pages) +
        //            App Link vérifié (assetlinks.json, autoVerify) →
        //            https://dadoou5.github.io/p/… ouvre l'app directement.
        // Build 22 : message de partage enrichi (date de décès + prochaine
        //            azcara, dans la langue de l'expéditeur) + aperçu de lien
        //            avec l'icône de l'app (Open Graph côté page de redirection).
        // Build 23 : écran « Chabbat Chalom » affiché dès 1 h avant l'entrée
        //            (pré-Chabbat) pour informer des horaires d'entrée/sortie ;
        //            re-blocage à l'entrée réelle même si « continuer » avant.
        // Build 24 : détail de prière — section « Rappels » toujours affichée
        //            (état « Aucun rappel » explicite) ; robustesse import liens.
        // Build 25 : retrait d'un favori directement depuis la liste (bouton
        //            cœur), sans ouvrir le Tehilim.
        // Build 26 : « Chaîne de Tehilim » — lecture collective temps réel
        //            (Firebase Firestore) : créer/partager une chaîne, rejoindre,
        //            réserver des Tehilim (verrous), compte rendu WhatsApp.
        // Build 27 : optimisation quota Firestore — état des attributions en doc
        //            unique (board) + écoute coupée en arrière-plan.
        // Build 28 : re-build (versionCode requis par le Play Store).
        // Build 29 : redesign écran de création de chaîne + temps de lecture
        //            estimé (détail Tehilim + cases de chaîne).
        // Build 30 : migration backend Firebase → Supabase + notifications push
        //            (participants : 70/80/90 %, distribution, suppression),
        //            suppression de chaîne par le créateur, sélection fluide
        //            (UI optimiste), i18n EN, auto-bascule APNs.
        // Build 31 : chaîne v2 — invitation + QR code, filtres de grille (par
        //            les 5 livres), répartition par personne, quitter / éditer /
        //            retirer un participant, notifs 100 % + rappel 80 %.
        versionCode = 40
        versionName = "1.0.0"

        // Feature « Chaîne de Tehilim » — config Supabase injectée dans BuildConfig.
        buildConfigField("String", "SUPABASE_URL",
            "\"${supabaseProperties.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY",
            "\"${supabaseProperties.getProperty("SUPABASE_ANON_KEY", "")}\"")

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
    // (Aucun workaround Guava : il n'existait que pour le conflit
    // Firestore↔WorkManager, disparu avec la migration vers Supabase.)

    // Compose BOM — version unique pour tout l'écosystème Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose UI + Material 3
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")

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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // KotlinX DateTime (Hebrew calendar utilities)
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    // Splash screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // AppCompat — pour AppCompatDelegate.setApplicationLocales (per-app language)
    implementation("androidx.appcompat:appcompat:1.7.0")

    // WorkManager — notifications quotidiennes
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Supabase (feature « Chaîne de Tehilim ») — Postgres + Realtime + auth
    // anonyme via supabase-kt + moteur Ktor (OkHttp). Depuis mavenCentral, aucun
    // plugin Gradle. Compile sans config ; l'app reste locale si
    // supabase.properties est absent (cf. SupabaseClientProvider).
    // supabase-kt 3.0.3 : compilé avec Kotlin 2.1.0 (consommable par le 2.1.10
    // du projet), et aligné sur coroutines 1.9.0 + kotlinx-datetime 0.6.1 — les
    // versions DÉJÀ utilisées ici (aucune régression sur le calendrier hébraïque).
    // Ktor 3.0.2 = celui qu'embarque supabase-kt 3.0.3 (moteur OkHttp assorti).
    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.3"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.ktor:ktor-client-okhttp:3.0.2")

    // FCM — notifications push de chaîne (messagerie uniquement ; la base reste
    // Supabase). Compile sans google-services.json ; init gardée au runtime.
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")
    // Guava complet : évince le stub listenablefuture (conflit play-services ↔ WorkManager).
    implementation("com.google.guava:guava:33.3.1-android")

    // QR code d'invitation de chaîne (partage en présentiel).
    implementation("com.google.zxing:core:3.5.3")

    // Glance — widget Compose-like
    // V1.4 build 16 — bump Glance 1.1.0 → 1.1.1 :
    // Play Console flag « note cruciale » sur androidx.glance:glance-appwidget-proto
    // et glance-appwidget-external-protobuf à la version 1.1.0. La 1.1.1 corrige
    // aussi l'usage interne des APIs edge-to-edge dépréciées en SDK 35+.
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    // Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

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
