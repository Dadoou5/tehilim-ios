import WidgetKit
import SwiftUI

@main
struct TehilimWidgetBundle: WidgetBundle {
    init() {
        // V2.1.b — swizzle de Bundle.main pour que les Text("…") du widget
        // résolvent contre la `.lproj` correspondant à `pref.app.language`
        // partagée via AppGroup. Sans ça, le widget reste figé sur la locale
        // iOS système même quand l'utilisateur bascule la langue in-app.
        LocalizedBundleInstaller.installOnce()
    }

    var body: some Widget {
        DailyVerseWidget()
    }
}
