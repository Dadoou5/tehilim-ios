import SwiftUI

struct PsalmsTabView: View {
    @Binding var path: NavigationPath
    @EnvironmentObject private var router: TabRouter

    var body: some View {
        NavigationStack(path: $path) {
            VStack(spacing: 0) {
                Picker("", selection: $router.psalmsSegment) {
                    Text("Livres").tag(0)
                    Text("Tous").tag(1)
                    Text("Favoris").tag(2)
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 16)
                .padding(.top, 8)

                Group {
                    switch router.psalmsSegment {
                    case 0: BookListView()
                    case 1: PsalmListView(book: nil)
                    default: FavoritesListView()
                    }
                }
            }
            .navigationTitle("Tehilim")
        }
    }
}
