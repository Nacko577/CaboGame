import Foundation

enum DrawSource: String, Codable, CaseIterable {
    case deck
    case discardTop
}

enum SpecialAction: Codable, Equatable {
    case none
    case lookOwnCard(playerID: UUID, cardIndex: Int)
    case lookOtherCard(targetPlayerID: UUID, cardIndex: Int)
    case swapCards(
        fromPlayerID: UUID,
        fromCardIndex: Int,
        toPlayerID: UUID,
        toCardIndex: Int
    )
}

enum TurnPhase: String, Codable {
    case initialPeek
    case waitingForDraw
    case waitingForPlacementOrDiscard
    case waitingForSpecialResolution
}

struct PendingDraw: Codable, Equatable {
    let card: Card
    let source: DrawSource
}
