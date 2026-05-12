import SwiftUI

// MARK: - Board (felt) themes — stored under `cabogame.boardTheme`

enum BoardTheme: String, CaseIterable, Identifiable {
    case forest
    case ocean
    case sunset
    case amethyst
    case ruby
    case obsidian

    var id: String { rawValue }

    /// Maps stored raw values (including legacy `midnight` → amethyst).
    static func fromStored(_ raw: String) -> BoardTheme {
        switch raw {
        case "midnight": return .amethyst
        default: return BoardTheme(rawValue: raw) ?? .forest
        }
    }

    var displayName: String {
        switch self {
        case .forest: return "Forest"
        case .ocean: return "Ocean"
        case .sunset: return "Sunset"
        case .amethyst: return "Amethyst"
        case .ruby: return "Ruby"
        case .obsidian: return "Obsidian"
        }
    }

    var bgDark: Color {
        switch self {
        case .forest: return Color(red: 0.05, green: 0.10, blue: 0.08)
        case .ocean: return Color(red: 0.06, green: 0.10, blue: 0.14)
        case .sunset: return Color(red: 0.11, green: 0.09, blue: 0.04)
        case .amethyst: return Color(red: 0.06, green: 0.06, blue: 0.12)
        case .ruby: return Color(red: 0.10, green: 0.05, blue: 0.08)
        case .obsidian: return Color(red: 0.03, green: 0.03, blue: 0.04)
        }
    }

    var bgTable: Color {
        switch self {
        case .forest: return Color(red: 0.08, green: 0.14, blue: 0.11)
        case .ocean: return Color(red: 0.08, green: 0.14, blue: 0.22)
        case .sunset: return Color(red: 0.22, green: 0.17, blue: 0.08)
        case .amethyst: return Color(red: 0.10, green: 0.10, blue: 0.20)
        case .ruby: return Color(red: 0.18, green: 0.08, blue: 0.12)
        case .obsidian: return Color(red: 0.07, green: 0.08, blue: 0.10)
        }
    }

    var tableBorder: Color {
        switch self {
        case .forest: return Color(red: 0.18, green: 0.65, blue: 0.45)
        case .ocean: return Color(red: 0.25, green: 0.55, blue: 0.85)
        case .sunset: return Color(red: 0.83, green: 0.66, blue: 0.17)
        case .amethyst: return Color(red: 0.45, green: 0.40, blue: 0.85)
        case .ruby: return Color(red: 0.85, green: 0.35, blue: 0.50)
        case .obsidian: return Color(red: 0.25, green: 0.30, blue: 0.38)
        }
    }

    /// Primary UI accent (turn highlight, primary buttons).
    var accentPrimary: Color {
        switch self {
        case .forest: return Color(red: 0.18, green: 0.75, blue: 0.55)
        case .ocean: return Color(red: 0.35, green: 0.72, blue: 0.95)
        case .sunset: return Color(red: 0.94, green: 0.69, blue: 0.12)
        case .amethyst: return Color(red: 0.55, green: 0.50, blue: 0.95)
        case .ruby: return Color(red: 0.95, green: 0.40, blue: 0.55)
        case .obsidian: return Color(red: 0.43, green: 0.70, blue: 0.97)
        }
    }

    var accentGold: Color {
        switch self {
        case .forest: return Color(red: 0.95, green: 0.75, blue: 0.30)
        case .ocean: return Color(red: 0.95, green: 0.80, blue: 0.45)
        case .sunset: return Color(red: 1.0, green: 0.88, blue: 0.42)
        case .amethyst: return Color(red: 0.75, green: 0.68, blue: 0.95)
        case .ruby: return Color(red: 0.98, green: 0.75, blue: 0.40)
        case .obsidian: return Color(red: 0.62, green: 0.68, blue: 0.78)
        }
    }

    /// Text / icons on solid primary buttons.
    var actionLabelOnPrimary: Color { bgDark }
}

// MARK: - Card deck styles — stored under `cabogame.cardDeckStyle`

enum CardBackPattern: String, CaseIterable {
    case solid
    case diagonalStripes
    case dotGrid
    case diamondLattice
    case crosshatch
    case plaid
    case squiggles
}

enum CardDeckStyle: String, CaseIterable, Identifiable {
    case classic
    case tartan
    case marina
    case saffron
    case velvet
    case casino
    case noir

    var id: String { rawValue }

    /// Maps stored raw values (legacy purple deck `amethyst` → velvet).
    static func fromStored(_ raw: String) -> CardDeckStyle {
        switch raw {
        case "amethyst": return .velvet
        default: return CardDeckStyle(rawValue: raw) ?? .classic
        }
    }

    var displayName: String {
        switch self {
        case .classic: return "Classic"
        case .tartan: return "Tartan"
        case .marina: return "Marina"
        case .saffron: return "Saffron"
        case .velvet: return "Velvet"
        case .casino: return "Casino"
        case .noir: return "Noir"
        }
    }

    var faceBackground: Color {
        switch self {
        case .classic: return .white
        case .tartan: return Color(red: 0.96, green: 0.94, blue: 0.88)
        case .marina: return Color(red: 0.98, green: 0.97, blue: 0.94)
        case .saffron: return Color(red: 1.0, green: 0.97, blue: 0.88)
        case .velvet: return Color(red: 0.99, green: 0.96, blue: 0.98)
        case .casino: return Color(red: 0.99, green: 0.97, blue: 0.92)
        case .noir: return Color(red: 0.18, green: 0.18, blue: 0.20)
        }
    }

    var redSuit: Color {
        switch self {
        case .classic: return Color(red: 0.85, green: 0.15, blue: 0.18)
        case .tartan: return Color(red: 0.68, green: 0.18, blue: 0.20)
        case .marina: return Color(red: 0.82, green: 0.22, blue: 0.32)
        case .saffron: return Color(red: 0.78, green: 0.28, blue: 0.14)
        case .velvet: return Color(red: 0.72, green: 0.16, blue: 0.36)
        case .casino: return Color(red: 0.72, green: 0.10, blue: 0.14)
        case .noir: return Color(red: 0.95, green: 0.45, blue: 0.45)
        }
    }

    var blackSuit: Color {
        switch self {
        case .classic: return Color(red: 0.08, green: 0.08, blue: 0.10)
        case .tartan: return Color(red: 0.18, green: 0.18, blue: 0.20)
        case .marina: return Color(red: 0.14, green: 0.22, blue: 0.34)
        case .saffron: return Color(red: 0.22, green: 0.16, blue: 0.08)
        case .velvet: return Color(red: 0.20, green: 0.14, blue: 0.30)
        case .casino: return Color(red: 0.12, green: 0.10, blue: 0.12)
        case .noir: return Color(red: 0.86, green: 0.88, blue: 0.92)
        }
    }

    var backBase: Color {
        switch self {
        case .classic: return Color(red: 0.12, green: 0.18, blue: 0.24)
        case .tartan: return Color(red: 0.14, green: 0.20, blue: 0.12)
        case .marina: return Color(red: 0.07, green: 0.32, blue: 0.38)
        case .saffron: return Color(red: 0.72, green: 0.48, blue: 0.08)
        case .velvet: return Color(red: 0.30, green: 0.10, blue: 0.26)
        case .casino: return Color(red: 0.55, green: 0.12, blue: 0.18)
        case .noir: return Color(red: 0.10, green: 0.10, blue: 0.11)
        }
    }

    var backAccent: Color {
        switch self {
        case .classic: return Color(red: 0.28, green: 0.38, blue: 0.48)
        case .tartan: return Color(red: 0.48, green: 0.68, blue: 0.38)
        case .marina: return Color(red: 0.55, green: 0.88, blue: 0.86)
        case .saffron: return Color(red: 1.0, green: 0.91, blue: 0.52)
        case .velvet: return Color(red: 0.94, green: 0.62, blue: 0.58)
        case .casino: return Color(red: 0.95, green: 0.86, blue: 0.55)
        case .noir: return Color(red: 0.42, green: 0.42, blue: 0.46)
        }
    }

    var backPattern: CardBackPattern {
        switch self {
        case .classic: return .solid
        case .tartan: return .plaid
        case .marina: return .diagonalStripes
        case .saffron: return .dotGrid
        case .velvet: return .squiggles
        case .casino: return .diamondLattice
        case .noir: return .crosshatch
        }
    }

    func suitColor(for text: String) -> Color {
        guard text.contains("♥") || text.contains("♦") else { return blackSuit }
        return redSuit
    }
}

// MARK: - Card back artwork

struct CardBackArt: View {
    let deck: CardDeckStyle
    var cornerRadius: CGFloat = 5

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: cornerRadius)
                .fill(deck.backBase)
            if deck.backPattern != .solid {
                RoundedRectangle(cornerRadius: cornerRadius)
                    .fill(Color.clear)
                    .overlay {
                        GeometryReader { geo in
                            Canvas { context, size in
                                Self.drawPattern(
                                    context: &context,
                                    size: size,
                                    pattern: deck.backPattern,
                                    accent: deck.backAccent
                                )
                            }
                            .frame(width: geo.size.width, height: geo.size.height)
                        }
                    }
                    .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
            }
        }
    }

    private static func drawPattern(
        context: inout GraphicsContext,
        size: CGSize,
        pattern: CardBackPattern,
        accent: Color
    ) {
        let w = size.width
        let h = size.height
        switch pattern {
        case .solid:
            break
        case .diagonalStripes:
            var x: CGFloat = -h
            while x < w + h {
                var path = Path()
                path.move(to: CGPoint(x: x, y: h))
                path.addLine(to: CGPoint(x: x + h * 1.05, y: 0))
                context.stroke(path, with: .color(accent.opacity(0.22)), lineWidth: 1.6)
                x += 9
            }
        case .dotGrid:
            let step: CGFloat = 8
            var row = 0
            var y: CGFloat = step * 0.35
            while y < h {
                var x: CGFloat = (row % 2 == 0) ? step * 0.35 : step * 0.85
                while x < w {
                    let dot = CGRect(x: x, y: y, width: 2.4, height: 2.4)
                    context.fill(Path(ellipseIn: dot), with: .color(accent.opacity(0.32)))
                    x += step
                }
                row += 1
                y += step * 0.9
            }
        case .diamondLattice:
            let step: CGFloat = 11
            var y: CGFloat = -step
            while y < h + step {
                var x: CGFloat = -step + (Int(y / step) % 2 == 0 ? 0 : step * 0.5)
                while x < w + step {
                    var path = Path()
                    path.move(to: CGPoint(x: x, y: y + step * 0.5))
                    path.addLine(to: CGPoint(x: x + step * 0.5, y: y))
                    path.addLine(to: CGPoint(x: x + step, y: y + step * 0.5))
                    path.addLine(to: CGPoint(x: x + step * 0.5, y: y + step))
                    path.closeSubpath()
                    context.stroke(path, with: .color(accent.opacity(0.38)), lineWidth: 0.9)
                    x += step
                }
                y += step * 0.55
            }
        case .crosshatch:
            var x: CGFloat = -h
            while x < w + h {
                var path = Path()
                path.move(to: CGPoint(x: x, y: h))
                path.addLine(to: CGPoint(x: x + h, y: 0))
                context.stroke(path, with: .color(accent.opacity(0.18)), lineWidth: 1)
                x += 7
            }
            var x2: CGFloat = -h
            while x2 < w + h {
                var path = Path()
                path.move(to: CGPoint(x: x2, y: 0))
                path.addLine(to: CGPoint(x: x2 + h, y: h))
                context.stroke(path, with: .color(accent.opacity(0.15)), lineWidth: 1)
                x2 += 7
            }
        case .squiggles:
            let m = min(w, h)
            let strokeW = max(1, m * 0.016)
            let amp = m * 0.04
            let lines = 6
            for i in 0..<lines {
                let t = CGFloat(i) / CGFloat(max(lines - 1, 1))
                let baseY = h * (0.12 + t * 0.76)
                let phase = CGFloat(i) * 0.85
                func yAt(_ x: CGFloat) -> CGFloat {
                    baseY + amp * (0.7 * sin(x * 0.11 + phase) + 0.35 * sin(x * 0.23 + phase * 1.3))
                }
                var path = Path()
                path.move(to: CGPoint(x: 0, y: yAt(0)))
                var x: CGFloat = 2
                while x <= w {
                    path.addLine(to: CGPoint(x: x, y: yAt(x)))
                    x += 2
                }
                context.stroke(path, with: .color(accent.opacity(0.36)), lineWidth: strokeW)
            }
        case .plaid:
            var yy: CGFloat = 0
            while yy < h {
                var path = Path()
                path.move(to: CGPoint(x: 0, y: yy))
                path.addLine(to: CGPoint(x: w, y: yy))
                context.stroke(path, with: .color(accent.opacity(0.22)), lineWidth: 1.2)
                yy += 6
            }
            var xx: CGFloat = 0
            while xx < w {
                var path = Path()
                path.move(to: CGPoint(x: xx, y: 0))
                path.addLine(to: CGPoint(x: xx, y: h))
                context.stroke(path, with: .color(accent.opacity(0.16)), lineWidth: 1)
                xx += 7
            }
        }
    }
}
