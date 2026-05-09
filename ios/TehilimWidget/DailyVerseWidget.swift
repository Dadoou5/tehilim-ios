import WidgetKit
import SwiftUI

struct DailyVerseWidget: Widget {
    let kind: String = "com.david.tehilim.widget.daily"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: DailyVerseProvider()) { entry in
            DailyVerseWidgetView(entry: entry)
                .containerBackground(for: .widget) {
                    WidgetBackground()
                }
                .widgetURL(URL(string: "tehilim://daily"))
        }
        .configurationDisplayName("Tehilim du jour")
        .description("Affiche les Tehilim à lire aujourd'hui selon le mode choisi.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}
