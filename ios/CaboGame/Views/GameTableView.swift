import SwiftUI

struct GameTableView: View {
    @EnvironmentObject private var viewModel: GameViewModel
    @State private var selectedOwnIndex: Int = 0
    @State private var selectedOpponentIndex: Int = 0
    @State private var selectedOpponentID: UUID?

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(red: 0.06, green: 0.34, blue: 0.22), Color(red: 0.04, green: 0.24, blue: 0.16)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 18) {
                    headerHUD
                    centerBoard
                    opponentsSection
                    localHandSection

                    if viewModel.gameState.winnerID == nil {
                        actionControls
                    } else {
                        rematchControls
                    }

                    if let matchStatus = viewModel.matchDiscardStatusText {
                        statusChip(matchStatus, color: .orange)
                    }

                    if let secs = viewModel.initialPeekSecondsRemaining,
                       viewModel.gameState.phase == .initialPeek,
                       viewModel.gameState.playersFinishedInitialPeek >= viewModel.gameState.players.count {
                        statusChip("Game starts in \(secs)s", color: .yellow)
                    }

                    if let winnerID = viewModel.gameState.winnerID,
                       let winner = viewModel.gameState.players.first(where: { $0.id == winnerID }) {
                        statusChip("Winner: \(winner.name) (\(winner.score()) points)", color: .mint)
                    } else if let caboCallerID = viewModel.gameState.caboCallerID,
                              let caller = viewModel.gameState.players.first(where: { $0.id == caboCallerID }) {
                        statusChip("Cabo called by \(caller.name). Final turns in progress...", color: .orange)
                    }
                }
                .padding(.horizontal, 14)
                .padding(.bottom, 24)
            }
        }
        .navigationTitle("Cabo")
    }

    private var headerHUD: some View {
        HStack(spacing: 10) {
            statusChip("Turn: \(currentTurnName)", color: isMyTurn ? .green : .blue)
            if let me = localPlayer, me.roundsToSkip > 0 {
                statusChip("Skip \(me.roundsToSkip)", color: .red)
            }
            Spacer(minLength: 0)
        }
    }

    private var centerBoard: some View {
        HStack(alignment: .top, spacing: 14) {
            VStack(spacing: 6) {
                Text("Discard")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.white.opacity(0.8))
                if let topDiscard = viewModel.gameState.discardPile.last {
                    tableCard(text: topDiscard.shortName, isFaceUp: true, isSelected: false, isEmpty: false)
                } else {
                    tableCard(text: "", isFaceUp: false, isSelected: false, isEmpty: false)
                }
            }

            VStack(spacing: 6) {
                Text("Drawn")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.white.opacity(0.8))
                if isMyTurn, let pending = viewModel.gameState.pendingDraw?.card {
                    tableCard(text: pending.shortName, isFaceUp: true, isSelected: true, isEmpty: false)
                } else if let replacedWith = viewModel.lastReplacedIncomingCard {
                    tableCard(text: replacedWith.shortName, isFaceUp: true, isSelected: false, isEmpty: false)
                } else {
                    tableCard(text: "", isFaceUp: false, isSelected: false, isEmpty: false)
                }
            }

            Spacer()

            if let attempted = viewModel.matchAttemptOwnCard,
               let topDiscard = viewModel.matchAttemptTopDiscardCard {
                VStack(spacing: 6) {
                    Text("Match Attempt")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.white.opacity(0.8))
                    HStack(spacing: 8) {
                        tableCard(text: topDiscard.shortName, isFaceUp: true, isSelected: false, isEmpty: false, size: CGSize(width: 54, height: 74))
                        Text("vs").foregroundStyle(.white.opacity(0.8)).font(.caption)
                        tableCard(text: attempted.shortName, isFaceUp: true, isSelected: false, isEmpty: false, size: CGSize(width: 54, height: 74))
                    }
                }
            }
        }
    }

    private var opponentsSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Opponents")
                .font(.headline)
                .foregroundStyle(.white)

            ForEach(opponents) { p in
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text(p.name)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(.white)
                        Spacer()
                        Text("\(p.hand.compactMap { $0 }.count) cards")
                            .font(.caption)
                            .foregroundStyle(.white.opacity(0.75))
                    }
                    HStack(spacing: 8) {
                        ForEach(p.hand.indices, id: \.self) { idx in
                            let card = p.hand[idx]
                            tableCard(
                                text: displayTextForOpponentSlot(card: card, playerID: p.id, index: idx),
                                isFaceUp: shouldRevealOpponentCard(playerID: p.id, at: idx),
                                isSelected: selectedOpponentID == p.id && selectedOpponentIndex == idx,
                                isEmpty: card == nil,
                                size: CGSize(width: 54, height: 74)
                            )
                            .onTapGesture {
                                selectedOpponentID = p.id
                                selectedOpponentIndex = idx
                            }
                        }
                    }
                }
                .padding(10)
                .background(Color.white.opacity(0.08))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
        }
    }

    private var localHandSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Your Hand")
                .font(.headline)
                .foregroundStyle(.white)

            if let me = localPlayer {
                HStack(spacing: 10) {
                    ForEach(me.hand.indices, id: \.self) { idx in
                        let card = me.hand[idx]
                        tableCard(
                            text: displayTextForOwnSlot(card: card, index: idx),
                            isFaceUp: shouldRevealOwnCard(at: idx),
                            isSelected: idx == selectedOwnIndex,
                            isEmpty: card == nil,
                            size: CGSize(width: 68, height: 96)
                        )
                        .onTapGesture {
                            selectedOwnIndex = idx
                        }
                    }
                }
            }
        }
        .padding(10)
        .background(Color.white.opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private var actionControls: some View {
        VStack(spacing: 12) {
            if viewModel.gameState.phase == .initialPeek {
                VStack(spacing: 8) {
                    Text("Initial setup: peek 2 of your cards")
                        .foregroundStyle(.white)
                    Text("Remaining peeks: \(remainingInitialPeeks)")
                        .font(.caption.weight(.medium))
                        .foregroundStyle(.white.opacity(0.8))
                    Button("Peek Selected Card") {
                        viewModel.peekInitialCard(at: selectedOwnIndex)
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(
                        !isMyTurn ||
                        remainingInitialPeeks <= 0 ||
                        viewModel.gameState.playersFinishedInitialPeek >= viewModel.gameState.players.count
                    )
                }
            }

            HStack {
                Button("Match Discard (Anytime)") {
                    viewModel.attemptMatchDiscard(withOwnCardAt: selectedOwnIndex)
                }
                .buttonStyle(.bordered)
                .disabled(
                    viewModel.gameState.phase == .initialPeek ||
                    viewModel.gameState.winnerID != nil ||
                    localPlayer == nil
                )
            }

            HStack {
                Button("Draw Deck") {
                    viewModel.draw(from: .deck)
                }
                .buttonStyle(.borderedProminent)
                Button("Draw Discard") {
                    viewModel.draw(from: .discardTop)
                }
                .buttonStyle(.bordered)
            }
            .disabled(!isMyTurn || viewModel.gameState.phase != .waitingForDraw)

            HStack {
                Button("Replace Selected Card") {
                    viewModel.replaceWithDrawnCard(at: selectedOwnIndex)
                }
                .buttonStyle(.borderedProminent)
                Button("Discard for Effect") {
                    viewModel.discardForEffect()
                }
                .buttonStyle(.bordered)
            }
            .disabled(!isMyTurn || viewModel.gameState.phase != .waitingForPlacementOrDiscard)

            if viewModel.gameState.phase == .waitingForSpecialResolution && isMyTurn {
                specialActionPanel
            }

            Button("Call Cabo") {
                viewModel.callCabo()
            }
            .buttonStyle(.borderedProminent)
            .disabled(
                !isMyTurn ||
                viewModel.gameState.phase == .initialPeek ||
                viewModel.gameState.caboCallerID != nil ||
                viewModel.gameState.winnerID != nil
            )
        }
        .padding(12)
        .background(Color.white.opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private var rematchControls: some View {
        VStack(spacing: 10) {
            if viewModel.isHostUser {
                Button("Start New Game") {
                    viewModel.startNewGameRound()
                }
                .buttonStyle(.borderedProminent)
                if viewModel.gameState.rematchRequestedByHost {
                    Text("Waiting for all players to be ready...")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            } else {
                Button(viewModel.isLocalPlayerReadyForNewGame ? "Ready ✓" : "Ready") {
                    viewModel.toggleReadyForNewGame()
                }
                .buttonStyle(.borderedProminent)
                .disabled(!viewModel.gameState.rematchRequestedByHost)

                if !viewModel.gameState.rematchRequestedByHost {
                    Text("Waiting for host to start a new game.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(12)
        .background(Color.white.opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }

    private var specialActionPanel: some View {
        VStack(spacing: 8) {
            Text("Resolve \(activeSpecialRank?.display ?? "") power")
                .foregroundStyle(.white)
            if let activeSpecialRank {
                switch activeSpecialRank {
                case .jack:
                    Text("Pick one of your cards above, then use Jack.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Button("Use Jack: Peek selected own card") {
                        guard let me = localPlayer else { return }
                        viewModel.resolveSpecial(.lookOwnCard(playerID: me.id, cardIndex: selectedOwnIndex))
                    }
                    .buttonStyle(.borderedProminent)

                case .queen:
                    Text("Tap an opponent card above, then use Queen.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Button("Use Queen: Peek selected opponent card") {
                        guard let targetID = selectedOpponentID else { return }
                        viewModel.resolveSpecial(.lookOtherCard(targetPlayerID: targetID, cardIndex: selectedOpponentIndex))
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(selectedOpponentID == nil)

                case .king:
                    Text("Pick your card and an opponent card above, then use King.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Button("Use King: Swap selected cards") {
                        guard let me = localPlayer, let targetID = selectedOpponentID else { return }
                        viewModel.resolveSpecial(
                            .swapCards(
                                fromPlayerID: me.id,
                                fromCardIndex: selectedOwnIndex,
                                toPlayerID: targetID,
                                toCardIndex: selectedOpponentIndex
                            )
                        )
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(selectedOpponentID == nil)

                default:
                    EmptyView()
                }
            }
        }
    }

    private var localPlayer: Player? {
        guard let localID = viewModel.localPlayerID else { return nil }
        return viewModel.gameState.players.first(where: { $0.id == localID })
    }

    private var opponents: [Player] {
        guard let localID = viewModel.localPlayerID else { return [] }
        return viewModel.gameState.players.filter { $0.id != localID }
    }

    private var isMyTurn: Bool {
        viewModel.gameState.currentPlayerID == viewModel.localPlayerID
    }

    private var currentTurnName: String {
        guard let currentID = viewModel.gameState.currentPlayerID,
              let player = viewModel.gameState.players.first(where: { $0.id == currentID }) else {
            return "Unknown"
        }
        return player.name
    }

    private var activeSpecialRank: Rank? {
        guard viewModel.gameState.phase == .waitingForSpecialResolution,
              let rank = viewModel.gameState.discardPile.last?.rank else {
            return nil
        }
        switch rank {
        case .jack, .queen, .king:
            return rank
        default:
            return nil
        }
    }

    private var remainingInitialPeeks: Int {
        max(0, 2 - viewModel.gameState.currentPlayerPeekedIndices.count)
    }

    private func shouldRevealOwnCard(at index: Int) -> Bool {
        if viewModel.gameState.winnerID != nil {
            return true
        }
        if viewModel.gameState.phase == .initialPeek {
            if viewModel.initialPeekedOwnIndices.contains(index) {
                return true
            }
        }
        return viewModel.jackPeekedOwnIndex == index
    }

    private func shouldRevealOpponentCard(playerID: UUID, at index: Int) -> Bool {
        viewModel.queenPeekedOpponentPlayerID == playerID && viewModel.queenPeekedOpponentIndex == index
    }

    private func displayTextForOwnSlot(card: Card?, index: Int) -> String {
        guard let card else { return "" }
        return shouldRevealOwnCard(at: index) ? card.shortName : "🂠"
    }

    private func displayTextForOpponentSlot(card: Card?, playerID: UUID, index: Int) -> String {
        guard let card else { return "" }
        return shouldRevealOpponentCard(playerID: playerID, at: index) ? card.shortName : "🂠"
    }

    private func statusChip(_ text: String, color: Color) -> some View {
        Text(text)
            .font(.caption.weight(.semibold))
            .foregroundStyle(.white)
            .padding(.vertical, 7)
            .padding(.horizontal, 10)
            .background(color.opacity(0.35))
            .clipShape(Capsule())
    }

    private func tableCard(
        text: String,
        isFaceUp: Bool,
        isSelected: Bool,
        isEmpty: Bool,
        size: CGSize = CGSize(width: 60, height: 84)
    ) -> some View {
        let border = isSelected ? Color.yellow.opacity(0.9) : Color.white.opacity(0.2)
        return ZStack {
            RoundedRectangle(cornerRadius: 10)
                .fill(isEmpty ? Color.white.opacity(0.05) : (isFaceUp ? Color.white : Color(red: 0.09, green: 0.13, blue: 0.22)))
            RoundedRectangle(cornerRadius: 10)
                .stroke(border, lineWidth: isSelected ? 2.4 : 1.2)
            if isEmpty {
                RoundedRectangle(cornerRadius: 8)
                    .stroke(style: StrokeStyle(lineWidth: 1.2, dash: [4, 4]))
                    .foregroundStyle(Color.white.opacity(0.25))
                    .padding(5)
            } else {
                Text(isFaceUp ? text : "🂠")
                    .font(size.width > 64 ? .title3.weight(.semibold) : .headline.weight(.semibold))
                    .foregroundStyle(isFaceUp ? faceUpCardColor(for: text) : .white)
            }
        }
        .frame(width: size.width, height: size.height)
        .shadow(color: .black.opacity(0.24), radius: 5, x: 0, y: 3)
    }

    private func faceUpCardColor(for text: String) -> Color {
        if text.contains("♥") || text.contains("♦") {
            return .red
        }
        return .black
    }
}
