import Foundation

enum Suit: String, CaseIterable, Codable {
    case hearts
    case diamonds
    case clubs
    case spades
}

enum Rank: Int, CaseIterable, Codable {
    case ace = 1
    case two = 2
    case three = 3
    case four = 4
    case five = 5
    case six = 6
    case seven = 7
    case eight = 8
    case nine = 9
    case ten = 10
    case jack = 11
    case queen = 12
    case king = 13

    var display: String {
        switch self {
        case .ace: return "A"
        case .jack: return "J"
        case .queen: return "Q"
        case .king: return "K"
        default: return "\(rawValue)"
        }
    }

    var caboValue: Int {
        rawValue
    }
}

struct Card: Codable, Equatable, Identifiable {
    let id: UUID
    let suit: Suit
    let rank: Rank

    init(id: UUID = UUID(), suit: Suit, rank: Rank) {
        self.id = id
        self.suit = suit
        self.rank = rank
    }

    var shortName: String {
        let suitSymbol: String
        switch suit {
        case .hearts: suitSymbol = "♥"
        case .diamonds: suitSymbol = "♦"
        case .clubs: suitSymbol = "♣"
        case .spades: suitSymbol = "♠"
        }
        return "\(rank.display)\(suitSymbol)"
    }
}

extension Array where Element == Card {
    static func caboDeck(shuffled: Bool = true) -> [Card] {
        var deck: [Card] = []
        for suit in Suit.allCases {
            for rank in Rank.allCases {
                deck.append(Card(suit: suit, rank: rank))
            }
        }
        return shuffled ? deck.shuffled() : deck
    }
}
