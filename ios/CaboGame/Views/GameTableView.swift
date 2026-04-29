import SwiftUI

struct GameTableView: View {
    @EnvironmentObject private var viewModel: GameViewModel
    @State private var selectedOwnIndex: Int = 0
    @State private var selectedOpponentIndex: Int = 0
    @State private var selectedOpponentID: UUID?

    // Colors
    private let bgDark = Color(red: 0.05, green: 0.10, blue: 0.08)
    private let bgTable = Color(red: 0.08, green: 0.14, blue: 0.11)
    private let tableBorder = Color(red: 0.18, green: 0.65, blue: 0.45)
    private let accentGreen = Color(red: 0.18, green: 0.75, blue: 0.55)
    private let accentGold = Color(red: 0.95, green: 0.75, blue: 0.30)
    private let textPrimary = Color.white
    private let textSecondary = Color.white.opacity(0.6)
    private let cardBackColor = Color(red: 0.12, green: 0.18, blue: 0.24)

    var body: some View {
        GeometryReader { geo in
            let isCompact = geo.size.height < 750
            let cardW: CGFloat = isCompact ? 36 : 42
            let cardH: CGFloat = cardW * 1.4
            let myCardW: CGFloat = isCompact ? 48 : 56
            let myCardH: CGFloat = myCardW * 1.4

            ZStack {
                bgDark.ignoresSafeArea()

                VStack(spacing: 0) {
                    // Header
                    header(isCompact: isCompact)
                        .padding(.horizontal, 12)
                        .padding(.top, 6)
                        .padding(.bottom, 4)

                    // Rectangular Table Area
                    ZStack {
                        // Table background
                        RoundedRectangle(cornerRadius: 16)
                            .fill(bgTable)
                            .overlay(
                                RoundedRectangle(cornerRadius: 16)
                                    .stroke(tableBorder.opacity(0.5), lineWidth: 2)
                            )

                        VStack(spacing: 0) {
                            // Top row: up to 3 opponents
                            topOpponentsRow(cardW: cardW, cardH: cardH)
                                .padding(.top, isCompact ? 8 : 12)

                            Spacer(minLength: 4)

                            // Middle: side opponents + center piles
                            HStack(spacing: 0) {
                                // Left opponent
                                sideOpponent(index: 3, cardW: cardW, cardH: cardH, isCompact: isCompact)
                                    .frame(width: geo.size.width * 0.22)

                                Spacer(minLength: 0)

                                // Center piles
                                centerPiles(isCompact: isCompact)

                                Spacer(minLength: 0)

                                // Right opponent
                                sideOpponent(index: 4, cardW: cardW, cardH: cardH, isCompact: isCompact)
                                    .frame(width: geo.size.width * 0.22)
                            }

                            Spacer(minLength: 4)

                            // Bottom: Your hand
                            myHand(cardW: myCardW, cardH: myCardH, isCompact: isCompact)
                                .padding(.bottom, isCompact ? 8 : 12)
                        }
                        .padding(.horizontal, 8)
                    }
                    .padding(.horizontal, 8)
                    .frame(maxHeight: geo.size.height * 0.58)

                    // Status + Actions
                    VStack(spacing: isCompact ? 6 : 8) {
                        statusBanner(isCompact: isCompact)
                        actionButtons(isCompact: isCompact)
                    }
                    .padding(.horizontal, 12)
                    .padding(.top, 8)
                    .padding(.bottom, 12)
                }
            }
        }
        .navigationBarHidden(true)
    }

    // MARK: - Header
    private func header(isCompact: Bool) -> some View {
        HStack(spacing: 8) {
            // Turn indicator
            HStack(spacing: 5) {
                Circle()
                    .fill(isMyTurn ? accentGreen : accentGold)
                    .frame(width: 8, height: 8)
                Text(isMyTurn ? "Your Turn" : currentTurnName)
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(textPrimary)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background((isMyTurn ? accentGreen : accentGold).opacity(0.2))
            .cornerRadius(8)

            Spacer()

            Text("Cabo")
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(textPrimary)

            Spacer()

            // Skip indicator or spacer
            if let me = localPlayer, me.roundsToSkip > 0 {
                Text("Skip \(me.roundsToSkip)")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 5)
                    .background(Color.red.opacity(0.4))
                    .cornerRadius(6)
            } else {
                Color.clear.frame(width: 60)
            }
        }
    }

    // MARK: - Top Opponents (indices 0, 1, 2)
    private func topOpponentsRow(cardW: CGFloat, cardH: CGFloat) -> some View {
        let topOps = Array(opponents.prefix(3))

        return HStack(spacing: 12) {
            ForEach(Array(topOps.enumerated()), id: \.element.id) { idx, player in
                opponentSlot(player: player, opponentIndex: idx, cardW: cardW, cardH: cardH)
            }
            // Empty slots if fewer than 3
            ForEach(0..<(3 - topOps.count), id: \.self) { _ in
                emptyPlayerSlot(cardW: cardW, cardH: cardH)
            }
        }
    }

    // MARK: - Side Opponent (index 3 or 4)
    private func sideOpponent(index: Int, cardW: CGFloat, cardH: CGFloat, isCompact: Bool) -> some View {
        let sideOps = Array(opponents.dropFirst(3))

        return Group {
            if index - 3 < sideOps.count {
                let player = sideOps[index - 3]
                opponentSlot(player: player, opponentIndex: index, cardW: cardW, cardH: cardH, vertical: true)
            } else {
                emptyPlayerSlot(cardW: cardW, cardH: cardH, vertical: true)
            }
        }
    }

    // MARK: - Opponent Slot
    private func opponentSlot(player: Player, opponentIndex: Int, cardW: CGFloat, cardH: CGFloat, vertical: Bool = false) -> some View {
        let isTurn = viewModel.gameState.currentPlayerID == player.id
        let isSelected = selectedOpponentID == player.id

        return VStack(spacing: 3) {
            Text(player.name)
                .font(.system(size: 10, weight: .semibold))
                .foregroundColor(isTurn ? accentGreen : textPrimary)
                .lineLimit(1)

            if vertical {
                // 2x2 grid for side opponents
                VStack(spacing: 2) {
                    HStack(spacing: 2) {
                        ForEach(0..<min(2, player.hand.count), id: \.self) { idx in
                            opponentCard(player: player, index: idx, cardW: cardW, cardH: cardH)
                        }
                    }
                    if player.hand.count > 2 {
                        HStack(spacing: 2) {
                            ForEach(2..<player.hand.count, id: \.self) { idx in
                                opponentCard(player: player, index: idx, cardW: cardW, cardH: cardH)
                            }
                        }
                    }
                }
            } else {
                // Horizontal row for top opponents
                HStack(spacing: 2) {
                    ForEach(0..<player.hand.count, id: \.self) { idx in
                        opponentCard(player: player, index: idx, cardW: cardW, cardH: cardH)
                    }
                }
            }
        }
        .padding(5)
        .background(isTurn ? accentGreen.opacity(0.15) : (isSelected ? accentGold.opacity(0.1) : Color.clear))
        .cornerRadius(8)
    }

    private func opponentCard(player: Player, index: Int, cardW: CGFloat, cardH: CGFloat) -> some View {
        let card = player.hand[index]
        let isRevealed = shouldRevealOpponentCard(playerID: player.id, at: index) || viewModel.gameState.winnerID != nil
        let isCardSelected = selectedOpponentID == player.id && selectedOpponentIndex == index

        return miniCard(
            text: card != nil ? (isRevealed ? card!.shortName : "") : "",
            isFaceUp: isRevealed && card != nil,
            isEmpty: card == nil,
            isSelected: isCardSelected,
            width: cardW,
            height: cardH
        )
        .onTapGesture {
            selectedOpponentID = player.id
            selectedOpponentIndex = index
        }
    }

    // MARK: - Empty Player Slot
    private func emptyPlayerSlot(cardW: CGFloat, cardH: CGFloat, vertical: Bool = false) -> some View {
        VStack(spacing: 3) {
            Text("Empty")
                .font(.system(size: 9, weight: .medium))
                .foregroundColor(textSecondary.opacity(0.4))

            if vertical {
                VStack(spacing: 2) {
                    HStack(spacing: 2) {
                        emptyCard(width: cardW, height: cardH)
                        emptyCard(width: cardW, height: cardH)
                    }
                    HStack(spacing: 2) {
                        emptyCard(width: cardW, height: cardH)
                        emptyCard(width: cardW, height: cardH)
                    }
                }
            } else {
                HStack(spacing: 2) {
                    ForEach(0..<4, id: \.self) { _ in
                        emptyCard(width: cardW, height: cardH)
                    }
                }
            }
        }
        .padding(5)
        .opacity(0.35)
    }

    // MARK: - Center Piles
    private func centerPiles(isCompact: Bool) -> some View {
        let pileW: CGFloat = isCompact ? 44 : 52
        let pileH: CGFloat = pileW * 1.4

        return HStack(spacing: isCompact ? 16 : 24) {
            // Discard
            VStack(spacing: 3) {
                Text("Discard")
                    .font(.system(size: 9, weight: .medium))
                    .foregroundColor(textSecondary)
                if let top = viewModel.gameState.discardPile.last {
                    pileCard(text: top.shortName, isFaceUp: true, width: pileW, height: pileH)
                } else {
                    pileCard(text: "", isFaceUp: false, width: pileW, height: pileH, isEmpty: true)
                }
            }

            // Drawn / Deck
            VStack(spacing: 3) {
                Text(viewModel.gameState.pendingDraw != nil ? "Drawn" : "Deck")
                    .font(.system(size: 9, weight: .medium))
                    .foregroundColor(viewModel.gameState.pendingDraw != nil ? accentGold : textSecondary)
                if isMyTurn, let pending = viewModel.gameState.pendingDraw?.card {
                    pileCard(text: pending.shortName, isFaceUp: true, width: pileW, height: pileH, highlight: true)
                } else {
                    pileCard(text: "", isFaceUp: false, width: pileW, height: pileH)
                }
            }
        }
    }

    // MARK: - My Hand
    private func myHand(cardW: CGFloat, cardH: CGFloat, isCompact: Bool) -> some View {
        VStack(spacing: 4) {
            Text("You")
                .font(.system(size: 11, weight: .bold))
                .foregroundColor(isMyTurn ? accentGold : textPrimary)

            if let me = localPlayer {
                HStack(spacing: isCompact ? 6 : 8) {
                    ForEach(me.hand.indices, id: \.self) { idx in
                        let card = me.hand[idx]
                        let isRevealed = shouldRevealOwnCard(at: idx) || viewModel.gameState.winnerID != nil
                        let isCardSelected = idx == selectedOwnIndex

                        miniCard(
                            text: card != nil ? (isRevealed ? card!.shortName : "") : "",
                            isFaceUp: isRevealed && card != nil,
                            isEmpty: card == nil,
                            isSelected: isCardSelected,
                            width: cardW,
                            height: cardH
                        )
                        .onTapGesture {
                            selectedOwnIndex = idx
                        }
                    }
                }
            }
        }
        .padding(.vertical, 8)
        .padding(.horizontal, 12)
        .background(isMyTurn ? accentGold.opacity(0.12) : Color.white.opacity(0.05))
        .cornerRadius(10)
    }

    // MARK: - Status Banner
    private func statusBanner(isCompact: Bool) -> some View {
        Group {
            if let winnerID = viewModel.gameState.winnerID,
               let winner = viewModel.gameState.players.first(where: { $0.id == winnerID }) {
                statusPill("Winner: \(winner.name) (\(winner.score()) pts)", color: .mint)
            } else if viewModel.gameState.phase == .initialPeek {
                statusPill("Peek 2 cards (\(remainingInitialPeeks) left)", color: accentGold)
            } else if let caboCallerID = viewModel.gameState.caboCallerID,
                      let caller = viewModel.gameState.players.first(where: { $0.id == caboCallerID }) {
                statusPill("Cabo! \(caller.name) - Final turns", color: .orange)
            } else if let matchStatus = viewModel.matchDiscardStatusText {
                statusPill(matchStatus, color: .orange)
            } else if viewModel.gameState.phase == .waitingForSpecialResolution {
                statusPill("Resolve \(activeSpecialRank?.display ?? "") power", color: accentGreen)
            } else if let secs = viewModel.initialPeekSecondsRemaining,
                      viewModel.gameState.playersFinishedInitialPeek >= viewModel.gameState.players.count {
                statusPill("Starting in \(secs)s", color: .yellow)
            } else {
                EmptyView()
            }
        }
    }

    private func statusPill(_ text: String, color: Color) -> some View {
        Text(text)
            .font(.system(size: 12, weight: .semibold))
            .foregroundColor(.white)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(color.opacity(0.3))
            .cornerRadius(8)
    }

    // MARK: - Action Buttons
    private func actionButtons(isCompact: Bool) -> some View {
        let btnH: CGFloat = isCompact ? 36 : 40
        let fontSize: CGFloat = isCompact ? 11 : 12

        return Group {
            if viewModel.gameState.winnerID != nil {
                // Rematch controls
                rematchButtons(btnH: btnH, fontSize: fontSize)
            } else if viewModel.gameState.phase == .initialPeek {
                // Initial peek
                HStack(spacing: 8) {
                    actionBtn("Peek Card", primary: true, height: btnH, fontSize: fontSize) {
                        viewModel.peekInitialCard(at: selectedOwnIndex)
                    }
                    .disabled(remainingInitialPeeks <= 0 || !isMyTurn || viewModel.gameState.playersFinishedInitialPeek >= viewModel.gameState.players.count)
                }
            } else if viewModel.gameState.phase == .waitingForSpecialResolution && isMyTurn {
                // Special power resolution
                specialButtons(btnH: btnH, fontSize: fontSize)
            } else {
                // Normal game actions - 2 rows
                VStack(spacing: 6) {
                    HStack(spacing: 6) {
                        actionBtn("Draw Deck", primary: true, height: btnH, fontSize: fontSize) {
                            viewModel.draw(from: .deck)
                        }
                        .disabled(!isMyTurn || viewModel.gameState.phase != .waitingForDraw)

                        actionBtn("Draw Discard", primary: false, height: btnH, fontSize: fontSize) {
                            viewModel.draw(from: .discardTop)
                        }
                        .disabled(!isMyTurn || viewModel.gameState.phase != .waitingForDraw)

                        actionBtn("Match", primary: false, height: btnH, fontSize: fontSize) {
                            viewModel.attemptMatchDiscard(withOwnCardAt: selectedOwnIndex)
                        }
                        .disabled(viewModel.gameState.phase == .initialPeek || localPlayer == nil)
                    }

                    HStack(spacing: 6) {
                        actionBtn("Replace", primary: true, height: btnH, fontSize: fontSize) {
                            viewModel.replaceWithDrawnCard(at: selectedOwnIndex)
                        }
                        .disabled(!isMyTurn || viewModel.gameState.phase != .waitingForPlacementOrDiscard)

                        actionBtn("Discard", primary: false, height: btnH, fontSize: fontSize) {
                            viewModel.discardForEffect()
                        }
                        .disabled(!isMyTurn || viewModel.gameState.phase != .waitingForPlacementOrDiscard)

                        actionBtn("Cabo!", primary: false, height: btnH, fontSize: fontSize, accent: accentGold) {
                            viewModel.callCabo()
                        }
                        .disabled(!isMyTurn || viewModel.gameState.phase == .initialPeek || viewModel.gameState.caboCallerID != nil)
                    }
                }
            }
        }
    }

    private func rematchButtons(btnH: CGFloat, fontSize: CGFloat) -> some View {
        HStack(spacing: 8) {
            if viewModel.isHostUser {
                actionBtn("New Game", primary: true, height: btnH, fontSize: fontSize) {
                    viewModel.startNewGameRound()
                }
            } else {
                actionBtn(viewModel.isLocalPlayerReadyForNewGame ? "Ready!" : "Ready", primary: true, height: btnH, fontSize: fontSize) {
                    viewModel.toggleReadyForNewGame()
                }
                .disabled(!viewModel.gameState.rematchRequestedByHost)
            }
        }
    }

    private func specialButtons(btnH: CGFloat, fontSize: CGFloat) -> some View {
        HStack(spacing: 8) {
            if let rank = activeSpecialRank {
                switch rank {
                case .jack:
                    actionBtn("Peek Own Card", primary: true, height: btnH, fontSize: fontSize) {
                        guard let me = localPlayer else { return }
                        viewModel.resolveSpecial(.lookOwnCard(playerID: me.id, cardIndex: selectedOwnIndex))
                    }
                case .queen:
                    actionBtn("Peek Opponent Card", primary: true, height: btnH, fontSize: fontSize) {
                        guard let targetID = selectedOpponentID else { return }
                        viewModel.resolveSpecial(.lookOtherCard(targetPlayerID: targetID, cardIndex: selectedOpponentIndex))
                    }
                    .disabled(selectedOpponentID == nil)
                case .king:
                    actionBtn("Swap Cards", primary: true, height: btnH, fontSize: fontSize) {
                        guard let me = localPlayer, let targetID = selectedOpponentID else { return }
                        viewModel.resolveSpecial(.swapCards(fromPlayerID: me.id, fromCardIndex: selectedOwnIndex, toPlayerID: targetID, toCardIndex: selectedOpponentIndex))
                    }
                    .disabled(selectedOpponentID == nil)
                default:
                    EmptyView()
                }
            }
        }
    }

    private func actionBtn(_ title: String, primary: Bool, height: CGFloat, fontSize: CGFloat, accent: Color? = nil, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: fontSize, weight: .semibold))
                .foregroundColor(primary ? bgDark : textPrimary)
                .frame(maxWidth: .infinity)
                .frame(height: height)
                .background(primary ? (accent ?? accentGreen) : Color.white.opacity(0.12))
                .cornerRadius(8)
        }
        .buttonStyle(.plain)
    }

    // MARK: - Card Views
    private func miniCard(text: String, isFaceUp: Bool, isEmpty: Bool, isSelected: Bool, width: CGFloat, height: CGFloat) -> some View {
        let bg = isEmpty ? Color.white.opacity(0.05) : (isFaceUp ? Color.white : cardBackColor)
        let border = isSelected ? accentGold : Color.white.opacity(0.2)

        return ZStack {
            RoundedRectangle(cornerRadius: 5)
                .fill(bg)
            RoundedRectangle(cornerRadius: 5)
                .stroke(border, lineWidth: isSelected ? 2 : 0.8)

            if isEmpty {
                RoundedRectangle(cornerRadius: 4)
                    .stroke(style: StrokeStyle(lineWidth: 0.8, dash: [3, 3]))
                    .foregroundColor(Color.white.opacity(0.2))
                    .padding(3)
            } else if isFaceUp {
                Text(text)
                    .font(.system(size: width > 44 ? 14 : 10, weight: .bold))
                    .foregroundColor(cardColor(text))
            } else {
                Image(systemName: "suit.spade.fill")
                    .font(.system(size: width > 44 ? 14 : 10))
                    .foregroundColor(Color.white.opacity(0.25))
            }
        }
        .frame(width: width, height: height)
    }

    private func pileCard(text: String, isFaceUp: Bool, width: CGFloat, height: CGFloat, isEmpty: Bool = false, highlight: Bool = false) -> some View {
        let bg = isEmpty ? Color.white.opacity(0.08) : (isFaceUp ? Color.white : cardBackColor)
        let border = highlight ? accentGold : Color.white.opacity(0.25)

        return ZStack {
            RoundedRectangle(cornerRadius: 7)
                .fill(bg)
            RoundedRectangle(cornerRadius: 7)
                .stroke(border, lineWidth: highlight ? 2.5 : 1)

            if isEmpty {
                RoundedRectangle(cornerRadius: 5)
                    .stroke(style: StrokeStyle(lineWidth: 1, dash: [4, 4]))
                    .foregroundColor(Color.white.opacity(0.2))
                    .padding(4)
            } else if isFaceUp {
                Text(text)
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(cardColor(text))
            } else {
                Image(systemName: "suit.club.fill")
                    .font(.system(size: 16))
                    .foregroundColor(Color.white.opacity(0.3))
            }
        }
        .frame(width: width, height: height)
        .shadow(color: .black.opacity(0.3), radius: 4, y: 2)
    }

    private func emptyCard(width: CGFloat, height: CGFloat) -> some View {
        RoundedRectangle(cornerRadius: 5)
            .stroke(style: StrokeStyle(lineWidth: 0.8, dash: [3, 3]))
            .foregroundColor(Color.white.opacity(0.15))
            .frame(width: width, height: height)
    }

    private func cardColor(_ text: String) -> Color {
        (text.contains("♥") || text.contains("♦")) ? .red : .black
    }

    // MARK: - Computed Properties
    private var localPlayer: Player? {
        guard let localID = viewModel.localPlayerID else { return nil }
        return viewModel.gameState.players.first { $0.id == localID }
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
            return "..."
        }
        return player.name
    }

    private var activeSpecialRank: Rank? {
        guard viewModel.gameState.phase == .waitingForSpecialResolution,
              let rank = viewModel.gameState.discardPile.last?.rank else { return nil }
        switch rank {
        case .jack, .queen, .king: return rank
        default: return nil
        }
    }

    private var remainingInitialPeeks: Int {
        max(0, 2 - viewModel.gameState.currentPlayerPeekedIndices.count)
    }

    private func shouldRevealOwnCard(at index: Int) -> Bool {
        if viewModel.gameState.phase == .initialPeek && viewModel.initialPeekedOwnIndices.contains(index) {
            return true
        }
        return viewModel.jackPeekedOwnIndex == index
    }

    private func shouldRevealOpponentCard(playerID: UUID, at index: Int) -> Bool {
        viewModel.queenPeekedOpponentPlayerID == playerID && viewModel.queenPeekedOpponentIndex == index
    }
}
