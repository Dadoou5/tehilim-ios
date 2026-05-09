import Foundation

struct LifeCase: Codable, Identifiable, Hashable {
    let id: String
    let title: String
    let symbol: String
    let note: String
    let psalms: [Int]
    /// Section logique pour l'affichage (optionnelle, fallback "Autres").
    let section: String?
}
