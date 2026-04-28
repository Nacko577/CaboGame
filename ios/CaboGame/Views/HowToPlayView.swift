import SwiftUI

struct HowToPlayView: View {
    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(red: 0.05, green: 0.31, blue: 0.20), Color(red: 0.03, green: 0.20, blue: 0.14)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    ruleCard("Goal", "End rounds with the lowest card total.")
                    ruleCard("Start", "Each player gets 4 cards and peeks at 2.")
                    ruleCard("Turn", "Draw from deck or discard, then replace a card or discard for power.")
                    ruleCard("Powers", "J: peek your card, Q: peek opponent card, K: swap with opponent.")
                    ruleCard("Match Discard", "Anytime, match top discard rank with one of your cards to remove it. Wrong guess means skip next turn.")
                    ruleCard("Cabo", "Calling Cabo gives everyone one final turn until play returns to caller.")
                }
                .padding()
            }
        }
        .navigationTitle("How To Play")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func ruleCard(_ title: String, _ text: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.headline)
                .foregroundStyle(.white)
            Text(text)
                .font(.subheadline)
                .foregroundStyle(.white.opacity(0.88))
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white.opacity(0.10))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
