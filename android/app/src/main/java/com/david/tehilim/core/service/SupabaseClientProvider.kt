package com.david.tehilim.core.service

import com.david.tehilim.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

/**
 * Fournit le **client Supabase** unique de l'app (créé une seule fois, paresseux).
 *
 * URL + clé anon proviennent de [BuildConfig] (injectées depuis
 * `app/supabase.properties` au build). Si elles sont vides — fichier absent sur
 * un clone sans config, CI, contributeur externe — `client == null` et la
 * feature « Chaîne de Tehilim » reste **désactivée** : l'app demeure 100 %
 * locale. C'est la garantie qu'offrait la config Firebase auparavant.
 *
 * La clé anon est publique par conception (protégée par la RLS Postgres) ; on la
 * garde toutefois hors du dépôt par convention (cf. .gitignore).
 */
object SupabaseClientProvider {

    val client: SupabaseClient? by lazy {
        val url = BuildConfig.SUPABASE_URL
        val key = BuildConfig.SUPABASE_ANON_KEY
        if (url.isBlank() || key.isBlank()) {
            null
        } else {
            createSupabaseClient(supabaseUrl = url, supabaseKey = key) {
                install(Auth)
                install(Postgrest)
                install(Realtime)
            }
        }
    }
}
