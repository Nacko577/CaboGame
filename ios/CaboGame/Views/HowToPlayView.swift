import SwiftUI

private let howToPlayGoalScoring: [(card: Card, points: Int)] = [
    (Card(suit: .spades, rank: .ace), 1),
    (Card(suit: .hearts, rank: .two), 2),
    (Card(suit: .diamonds, rank: .three), 3),
    (Card(suit: .hearts, rank: .ten), 10),
    (Card(suit: .diamonds, rank: .jack), 11),
    (Card(suit: .clubs, rank: .queen), 12),
    (Card(suit: .spades, rank: .king), 13),
]

struct HowToPlayView: View {
    private let captionOpacity: Double = 0.72
    private let cardGold = Color(red: 0.95, green: 0.75, blue: 0.30)
    private let cardBackFill = Color(red: 0.09, green: 0.13, blue: 0.22)

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(red: 0.05, green: 0.31, blue: 0.20), Color(red: 0.03, green: 0.20, blue: 0.14)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    Text("Quick visual guide — same rules as around the table.")
                        .font(.subheadline)
                        .foregroundStyle(.white.opacity(captionOpacity))
                        .fixedSize(horizontal: false, vertical: true)

                    guideSection(
                        title: "Goal",
                        caption: "Lowest total wins the round. Ace = 1 pt. Cards 2–10 score their number (2 = 2 pts … 10 = 10 pts). Jack = 11, Queen = 12, King = 13."
                    ) {
                        HStack(spacing: 8) {
                            ForEach(Array(howToPlayGoalScoring.enumerated()), id: \.offset) { _, item in
                                scoringChip(card: item.card, points: item.points, cardWidth: 28, cardHeight: 40)
                                if item.card.rank == .three {
                                    goalRankEllipsis(cardHeight: 40)
                                }
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 4)
                    }

                    guideSection(title: "Setup", caption: "Everyone gets 4 face-down cards. Memorize any two of yours during the peek timer.") {
                        HStack(spacing: 6) {
                            ForEach(0..<4, id: \.self) { idx in
                                faceDownDemo(width: 34, height: 48, emphasized: idx == 1 || idx == 3)
                            }
                        }
                        .frame(maxWidth: .infinity)

                        Text("Gold outline = example peek picks")
                            .font(.caption)
                            .foregroundStyle(.white.opacity(captionOpacity))
                            .frame(maxWidth: .infinity)
                            .padding(.top, 8)
                    }

                    guideSection(
                        title: "Your turn",
                        caption: "Draw from the deck or the discard pile. Then either swap into your hand or discard to use the drawn card’s power."
                    ) {
                        turnFlowDemo()
                        Text(
                            "Replacing: put the drawn card in a slot and discard what was there.\nPower play: discard the drawn card — if it’s J/Q/K you resolve that effect next."
                        )
                        .font(.footnote)
                        .foregroundStyle(.white.opacity(captionOpacity))
                        .fixedSize(horizontal: false, vertical: true)
                        .padding(.top, 10)
                    }

                    guideSection(title: "Face-card powers", caption: "Only when you discard that rank from your draw step.") {
                        HStack(alignment: .top, spacing: 8) {
                            Spacer(minLength: 0)
                            powerCardDemo(card: Card(suit: .clubs, rank: .jack), blurb: "Peek one of your cards.")
                            Spacer()
                            powerCardDemo(card: Card(suit: .hearts, rank: .queen), blurb: "Peek one opponent card.")
                            Spacer()
                            powerCardDemo(card: Card(suit: .spades, rank: .king), blurb: "Swap one of yours with any opponent card.")
                            Spacer(minLength: 0)
                        }
                    }

                    guideSection(
                        title: "Match the discard",
                        caption: "Anytime (your turn or not): choose one of your cards. Same rank as the top discard removes it from your hand. Wrong guess skips your next turn — or ends your turn immediately if it was already yours."
                    ) {
                        VStack(spacing: 10) {
                            HStack(spacing: 8) {
                                Text("Top discard")
                                    .font(.caption)
                                    .foregroundStyle(.white.opacity(captionOpacity))
                                miniPlayingCard(Card(suit: .spades, rank: .five), width: 38, height: 54)
                            }
                            Image(systemName: "arrow.down")
                                .font(.caption)
                                .foregroundStyle(.white.opacity(0.45))
                            HStack(spacing: 18) {
                                VStack(spacing: 6) {
                                    miniPlayingCard(Card(suit: .diamonds, rank: .five), width: 38, height: 54)
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 6)
                                                .stroke(Color(red: 0.29, green: 0.79, blue: 0.40), lineWidth: 2)
                                        )
                                    Text("✓ Match — remove yours")
                                        .font(.caption2)
                                        .foregroundStyle(Color(red: 0.51, green: 0.78, blue: 0.50))
                                        .multilineTextAlignment(.center)
                                }
                                VStack(spacing: 6) {
                                    miniPlayingCard(Card(suit: .diamonds, rank: .three), width: 38, height: 54)
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 6)
                                                .stroke(Color(red: 0.90, green: 0.22, blue: 0.21), lineWidth: 2)
                                        )
                                    Text("✗ Wrong — skip next turn")
                                        .font(.caption2)
                                        .foregroundStyle(Color(red: 1.0, green: 0.54, blue: 0.50))
                                        .multilineTextAlignment(.center)
                                }
                            }
                        }
                        .frame(maxWidth: .infinity)
                    }

                    guideSection(
                        title: "Call Cabo",
                        caption: "When you think your total is lowest, call Cabo before drawing. Everyone gets one more lap; then scores are compared."
                    ) {
                        HStack(spacing: 10) {
                            Spacer()
                            Text("Final turns")
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(cardGold)
                            Image(systemName: "arrow.forward")
                                .font(.caption)
                                .foregroundStyle(.white.opacity(0.45))
                            Text("Reveal & score")
                                .font(.subheadline)
                                .foregroundStyle(.white.opacity(captionOpacity))
                            Spacer()
                        }
                    }
                }
                .padding()
            }
        }
        .navigationTitle("How To Play")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func guideSection<Content: View>(title: String, caption: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.title3.weight(.bold))
                .foregroundStyle(.white)
            Text(caption)
                .font(.subheadline)
                .foregroundStyle(.white.opacity(captionOpacity))
                .fixedSize(horizontal: false, vertical: true)
            content()
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white.opacity(0.10))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private func scoringChip(card: Card, points: Int, cardWidth: CGFloat = 38, cardHeight: CGFloat = 54) -> some View {
        VStack(spacing: 6) {
            miniPlayingCard(card, width: cardWidth, height: cardHeight)
            Text("\(points) pts")
                .font(.caption2)
                .foregroundStyle(.white.opacity(captionOpacity))
        }
    }

    /// Three-dot separator between rank 3 and 10 (same vertical rhythm as ``scoringChip``).
    private func goalRankEllipsis(cardHeight: CGFloat) -> some View {
        VStack(spacing: 6) {
            HStack(spacing: 4) {
                ForEach(0..<3, id: \.self) { _ in
                    Text("\u{00B7}")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(.white.opacity(captionOpacity))
                }
            }
            .frame(height: cardHeight)
            Text("0 pts")
                .font(.caption2)
                .foregroundStyle(.clear)
        }
        .accessibilityLabel("More ranks in between")
    }

    private func faceDownDemo(width: CGFloat, height: CGFloat, emphasized: Bool) -> some View {
        RoundedRectangle(cornerRadius: 6)
            .fill(cardBackFill)
            .frame(width: width, height: height)
            .overlay(
                RoundedRectangle(cornerRadius: 6)
                    .stroke(emphasized ? cardGold : Color.white.opacity(0.25), lineWidth: emphasized ? 2 : 1)
            )
    }

    private func miniPlayingCard(_ card: Card, width: CGFloat, height: CGFloat) -> some View {
        let red = card.suit == .hearts || card.suit == .diamonds
        let fontSize: CGFloat =
            width > 34 ? 14 : (width > 30 ? 12 : 10)
        return Text(card.shortName)
            .font(.system(size: fontSize, weight: .bold))
            .foregroundStyle(red ? Color(red: 0.83, green: 0.18, blue: 0.18) : Color(red: 0.13, green: 0.13, blue: 0.13))
            .frame(width: width, height: height)
            .background(Color.white, in: RoundedRectangle(cornerRadius: 6))
            .overlay(
                RoundedRectangle(cornerRadius: 6)
                    .stroke(Color.black.opacity(0.15), lineWidth: 1)
            )
    }

    private func powerCardDemo(card: Card, blurb: String) -> some View {
        VStack(spacing: 6) {
            miniPlayingCard(card, width: 36, height: 50)
            Text(blurb)
                .font(.caption2)
                .foregroundStyle(.white.opacity(captionOpacity))
                .multilineTextAlignment(.center)
                .frame(maxWidth: 112)
        }
    }

    private func turnFlowDemo() -> some View {
        VStack(spacing: 8) {
            HStack {
                VStack(spacing: 4) {
                    Text("Deck")
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.white.opacity(captionOpacity))
                    faceDownDemo(width: 36, height: 48, emphasized: false)
                }
                .frame(maxWidth: .infinity)
                Image(systemName: "arrow.forward")
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.45))
                VStack(spacing: 4) {
                    Text("Discard")
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.white.opacity(captionOpacity))
                    miniPlayingCard(Card(suit: .hearts, rank: .nine), width: 36, height: 48)
                }
                .frame(maxWidth: .infinity)
            }
            Image(systemName: "arrow.down")
                .font(.caption)
                .foregroundStyle(.white.opacity(0.45))
            VStack(spacing: 4) {
                Text("Your hand")
                    .font(.caption2)
                    .foregroundStyle(.white.opacity(captionOpacity))
                HStack(spacing: 4) {
                    ForEach(0..<4, id: \.self) { idx in
                        if idx == 2 {
                            miniPlayingCard(Card(suit: .clubs, rank: .two), width: 30, height: 42)
                        } else {
                            faceDownDemo(width: 30, height: 42, emphasized: false)
                        }
                    }
                }
                Text("example slot you’re remembering")
                    .font(.system(size: 10))
                    .foregroundStyle(.white.opacity(0.45))
            }
        }
    }
}

#if DEBUG
#Preview {
    NavigationStack {
        HowToPlayView()
    }
}
#endif
