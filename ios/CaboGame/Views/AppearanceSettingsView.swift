import SwiftUI

private enum AppearancePreviewMetrics {
    static let cardW: CGFloat = 44
    static let cardH: CGFloat = 62
    static let corner: CGFloat = 6
}

struct AppearanceSettingsView: View {
    @AppStorage("cabogame.boardTheme") private var boardThemeRaw = BoardTheme.forest.rawValue
    @AppStorage("cabogame.cardDeckStyle") private var cardDeckRaw = CardDeckStyle.classic.rawValue

    var body: some View {
        let previewBoard = BoardTheme.fromStored(boardThemeRaw)
        let previewDeck = CardDeckStyle.fromStored(cardDeckRaw)
        return ZStack {
            previewBoard.bgDark
                .ignoresSafeArea()
            Form {
                Section {
                    Picker("Table color", selection: $boardThemeRaw) {
                        ForEach(BoardTheme.allCases) { theme in
                            Text(theme.displayName).tag(theme.rawValue)
                        }
                    }
                    .pickerStyle(.navigationLink)
                } header: {
                    Text("Board")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.white.opacity(0.95))
                } footer: {
                    Text("Felt, frame, and accent colors on the game table. Each player picks their own look on their device.")
                        .font(.footnote)
                        .foregroundStyle(.white.opacity(0.72))
                }

                Section {
                    Picker("Card deck", selection: $cardDeckRaw) {
                        ForEach(CardDeckStyle.allCases) { deck in
                            Text(deck.displayName).tag(deck.rawValue)
                        }
                    }
                    .pickerStyle(.navigationLink)

                    HStack(alignment: .center, spacing: 12) {
                        Group {
                            deckFacePreview(deck: previewDeck)
                            deckBackPreview(deck: previewDeck)
                        }
                        .fixedSize(horizontal: true, vertical: true)
                        Spacer(minLength: 0)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 8, trailing: 16))
                } header: {
                    Text("Cards")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.white.opacity(0.95))
                } footer: {
                    Text("Face colors and card-back patterns are decorative only.")
                        .font(.footnote)
                        .foregroundStyle(.white.opacity(0.72))
                }
            }
            .scrollContentBackground(.hidden)
        }
        .preferredColorScheme(.dark)
        .tint(previewBoard.accentPrimary)
        .navigationTitle("Table look")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(.visible, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
    }

    private func deckFacePreview(deck: CardDeckStyle) -> some View {
        let r = AppearancePreviewMetrics.corner
        return Text("A♥")
            .font(.system(size: 18, weight: .bold))
            .foregroundStyle(deck.suitColor(for: "A♥"))
            .frame(width: AppearancePreviewMetrics.cardW, height: AppearancePreviewMetrics.cardH)
            .background(deck.faceBackground, in: RoundedRectangle(cornerRadius: r))
            .clipShape(RoundedRectangle(cornerRadius: r))
            .overlay(
                RoundedRectangle(cornerRadius: r)
                    .stroke(Color.black.opacity(0.12), lineWidth: 1)
            )
    }

    private func deckBackPreview(deck: CardDeckStyle) -> some View {
        let r = AppearancePreviewMetrics.corner
        return CardBackArt(deck: deck, cornerRadius: r)
            .frame(width: AppearancePreviewMetrics.cardW, height: AppearancePreviewMetrics.cardH)
            .clipShape(RoundedRectangle(cornerRadius: r))
            .overlay(
                RoundedRectangle(cornerRadius: r)
                    .stroke(Color.white.opacity(0.2), lineWidth: 1)
            )
    }
}
