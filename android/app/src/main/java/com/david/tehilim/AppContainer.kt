package com.david.tehilim

import android.content.Context
import com.david.tehilim.core.persistence.FavoritesStore
import com.david.tehilim.core.persistence.Preferences
import com.david.tehilim.core.persistence.SavedPrayerStore
import com.david.tehilim.core.service.ContentLoader
import com.david.tehilim.core.service.DailyEngine
import com.david.tehilim.core.service.LifeCaseRepository
import com.david.tehilim.core.service.Psalm119Repository
import com.david.tehilim.core.service.PsalmRepository
import com.david.tehilim.core.service.SearchInterpreter

/**
 * Conteneur DI simple — équivalent direct de l'AppContainer iOS.
 *
 * Charge le contenu (psaumes, cas de la vie, sections 119, règles de lecture)
 * depuis les JSONs embarqués dans `assets/data/` au démarrage de l'app.
 *
 * En cas d'échec de chargement, on instancie des repositories vides pour
 * éviter de planter l'app — un assertion failure est loggué pour debug.
 */
class AppContainer(context: Context) {

    val contentLoader: ContentLoader = ContentLoader(context)
    val preferences: Preferences = Preferences(context)
    val favorites: FavoritesStore = FavoritesStore(context)
    val savedPrayers: SavedPrayerStore = SavedPrayerStore(context)

    val psalmRepository: PsalmRepository
    val lifeCaseRepository: LifeCaseRepository
    val psalm119Repository: Psalm119Repository
    val dailyEngine: DailyEngine
    val searchInterpreter: SearchInterpreter

    init {
        val psalms = runCatching { contentLoader.loadPsalms() }.getOrElse {
            android.util.Log.e("AppContainer", "Failed to load psalms", it); emptyList()
        }
        val cases = runCatching { contentLoader.loadLifeCases() }.getOrElse {
            android.util.Log.e("AppContainer", "Failed to load life cases", it); emptyList()
        }
        val sections = runCatching { contentLoader.loadPsalm119Sections() }.getOrElse {
            android.util.Log.e("AppContainer", "Failed to load 119 sections", it); emptyList()
        }
        val rules = runCatching { contentLoader.loadDailyRules() }.getOrElse {
            android.util.Log.e("AppContainer", "Failed to load daily rules", it)
            com.david.tehilim.core.model.DailyRules.empty
        }

        psalmRepository = PsalmRepository(psalms)
        lifeCaseRepository = LifeCaseRepository(cases)
        psalm119Repository = Psalm119Repository(sections)
        dailyEngine = DailyEngine(rules)
        searchInterpreter = SearchInterpreter(psalmRepository)
    }
}
