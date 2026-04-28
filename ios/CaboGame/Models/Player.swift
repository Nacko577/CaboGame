import Foundation

struct Player: Identifiable, Codable, Equatable {
    let id: UUID
    var name: String
    var hand: [Card?]
    var roundsToSkip: Int

    init(id: UUID = UUID(), name: String, hand: [Card?] = [], roundsToSkip: Int = 0) {
        self.id = id
        self.name = name
        self.hand = hand
        self.roundsToSkip = roundsToSkip
    }

    func score() -> Int {
        hand.compactMap { $0 }.reduce(0) { $0 + $1.rank.caboValue }
    }
}
