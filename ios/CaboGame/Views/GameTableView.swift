import SwiftUI

struct GameTableView: View {
    @EnvironmentObject private var viewModel: GameViewModel
    let onLeaveGame: () -> Void
    @State private var selectedOwnIndex: Int = 0
    @State private var selectedOpponentIndex: Int = 0
    @State private var selectedOpponentID: UUID?

    private let bgDark = Color(red: 0.05, green: 0.10, blue: 0.08)
    private let bgTable = Color(red: 0.08, green: 0.14, blue: 0.11)
    private let tableBorder = Color(red: 0.18, green: 0.65, blue: 0.45)
    private let accentGreen = Color(red: 0.18, green: 0.75, blue: 0.55)
    private let accentGold = Color(red: 0.95, green: 0.75, blue: 0.30)
    private let cardBack = Color(red: 0.12, green: 0.18, blue: 0.24)

    var body: some View {
        GeometryReader { geo in
            let compact = geo.size.height < 750
            let narrow = geo.size.width < 390
            let opW: CGFloat = narrow ? 30 : (compact ? 34 : 40)
            let opH = opW * 1.4
            let myW: CGFloat = narrow ? 42 : (compact ? 46 : 54)
            let myH = myW * 1.4

            ZStack {
                bgDark.ignoresSafeArea()
                VStack(spacing: 6) {
                    header
                    topPillsRow
                    matchResultBanner
                    tableArea(opW: opW, opH: opH, myW: myW, myH: myH, compact: compact, width: geo.size.width, height: geo.size.height)
                    statusBanner
                    controls(compact: compact)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 4)
            }
        }
        .navigationBarHidden(true)
    }

    private var header: some View {
        HStack {
            Button(action: onLeaveGame) {
                Text("Leave")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(Color.white.opacity(0.12))
                    .cornerRadius(8)
            }
            Spacer()
            if let me = localPlayer, me.roundsToSkip > 0 {
                Text("Skip \(me.roundsToSkip)")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(.white)
                    .padding(.horizontal, 8).padding(.vertical, 5)
                    .background(Color.red.opacity(0.4)).cornerRadius(6)
            } else {
                Color.clear.frame(width: 60)
            }
        }
    }

    private func topPill(_ text: String, color: Color) -> some View {
        Text(text)
            .font(.system(size: 12, weight: .semibold))
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(color.opacity(0.3))
            .cornerRadius(8)
    }

    private var turnPill: some View {
        let isPeek = viewModel.gameState.phase == .initialPeek
        let text: String
        if isPeek {
            text = "Peeking"
        } else if isMyTurn {
            text = "Your Turn"
        } else {
            text = "\(currentTurnName)'s turn"
        }

        return topPill(text, color: isPeek ? accentGold : (isMyTurn ? accentGreen : accentGold))
    }

    private var peekProgressPill: some View {
        Group {
            if viewModel.gameState.phase == .initialPeek {
                topPill("Peek 2 cards (\(remainingInitialPeeks) left)", color: accentGold)
            }
        }
    }

    private var peekTimerPill: some View {
        Group {
            if viewModel.gameState.phase == .initialPeek {
                topPill("Time left: \(viewModel.initialPeekSecondsRemaining ?? 0)s", color: accentGold)
            }
        }
    }

    private var turnTimerPill: some View {
        Group {
            if let secs = viewModel.currentTurnSecondsRemaining,
               viewModel.gameState.winnerID == nil,
               viewModel.gameState.phase != .initialPeek {
                // Tint red when time is running out so the warning is obvious.
                let color: Color = secs <= 5 ? Color(red: 0.95, green: 0.30, blue: 0.30) : accentGold
                topPill("\(secs)s", color: color)
            }
        }
    }

    private var topPillsRow: some View {
        HStack(spacing: 8) {
            if viewModel.gameState.winnerID != nil {
                // Game is over; the round-result banner takes over.
                EmptyView()
            } else if viewModel.gameState.phase == .initialPeek {
                turnPill
                peekProgressPill
                peekTimerPill
            } else {
                turnPill
                turnTimerPill
            }
        }
        .padding(.horizontal, 2)
    }

    private func tableArea(
        opW: CGFloat,
        opH: CGFloat,
        myW: CGFloat,
        myH: CGFloat,
        compact: Bool,
        width: CGFloat,
        height: CGFloat
    ) -> some View {
        let boardW = width * 0.94
        // Make the board taller so the N/S/E/W seats fit better.
        let boardH = min(height * (compact ? 0.66 : 0.70), compact ? 520 : 560)

        // Seats: N/E/W/Opponents, S/local
        let opp = Array(opponents.prefix(3))
        let north = opp.count > 0 ? opp[0] : nil
        let east = opp.count > 1 ? opp[1] : nil
        let west = opp.count > 2 ? opp[2] : nil
        let south = localPlayer

        // Player seat cards (shrunken to fit one-row layout for 4 cards).
        let seatCardW: CGFloat = max(18, CGFloat(compact ? 26 : 30))
        let seatCardH: CGFloat = seatCardW * 1.35

        let padX = boardW * 0.08
        let padY = boardH * 0.10
        // Extra inset so E/W seats (especially the card column) aren’t flush with the table rim.
        let sideSeatInset = max(20, seatCardH * 0.18)

        return ZStack {
            RoundedRectangle(cornerRadius: 24)
                .fill(bgTable)
                .overlay(RoundedRectangle(cornerRadius: 24).stroke(tableBorder.opacity(0.55), lineWidth: 3))
                .shadow(color: .black.opacity(0.25), radius: 8, x: 0, y: 4)

            // Center piles.
            centerPiles(compact: compact, pileW: max(28, seatCardW * 1.6))

            // N — cards toward top edge, name below toward center
            seatView(player: north, isLocal: false, seatCardW: seatCardW, seatCardH: seatCardH, position: .north)
                .position(x: boardW / 2, y: padY)
            // S (local) — name toward center, cards toward bottom edge
            seatView(player: south, isLocal: true, seatCardW: seatCardW, seatCardH: seatCardH, position: .south)
                .position(x: boardW / 2, y: boardH - padY)
            // E — inset from right rim; landscape stack + perpendicular symbols; name +180° from prior 90°
            seatView(player: east, isLocal: false, seatCardW: seatCardW, seatCardH: seatCardH, position: .east)
                .position(x: boardW - padX - sideSeatInset, y: boardH / 2)
            // W — inset from left rim
            seatView(player: west, isLocal: false, seatCardW: seatCardW, seatCardH: seatCardH, position: .west)
                .position(x: padX + sideSeatInset, y: boardH / 2)
        }
        .frame(width: boardW, height: boardH)
        .padding(.top, compact ? 2 : 6)
    }

    private enum TableSeatPosition {
        /// Top: horizontal row of portrait cards; name below (toward table center).
        case north
        /// Bottom: name above row (toward center); portrait cards below toward edge.
        case south
        /// Right edge: name left of a vertical column of portrait cards (name toward center).
        case east
        /// Left edge: vertical column of portrait cards; name right of column (toward center).
        case west
    }

    private enum SideSeatEdge {
        /// N/S: portrait cards in a row; rank/suit axis matches the card shape.
        case none
        /// Right edge: landscape stack; rotate face symbols so they’re perpendicular to the felt.
        case east
        case west
    }

    private func seatView(
        player: Player?,
        isLocal: Bool,
        seatCardW: CGFloat,
        seatCardH: CGFloat,
        position: TableSeatPosition
    ) -> some View {
        return Group {
            if let player {
                let indices = Array(player.hand.indices.prefix(4))
                let nameLabel = seatNameLabel(isLocal: isLocal, player: player)

                switch position {
                case .north:
                    VStack(spacing: 4) {
                        HStack(spacing: 2) {
                            ForEach(indices, id: \.self) { idx in
                                sideSeatCard(idx: idx, player: player, isLocal: isLocal, seatCardW: seatCardW, seatCardH: seatCardH, edge: .none)
                            }
                        }
                        nameLabel
                    }
                case .south:
                    VStack(spacing: 4) {
                        nameLabel
                        HStack(spacing: 2) {
                            ForEach(indices, id: \.self) { idx in
                                sideSeatCard(idx: idx, player: player, isLocal: isLocal, seatCardW: seatCardW, seatCardH: seatCardH, edge: .none)
                            }
                        }
                    }
                case .east:
                    // Name at 270° (prior 90° + 180°). Symbols on landscape cards run perpendicular to the felt.
                    HStack(alignment: .center, spacing: 4) {
                        nameLabel.rotationEffect(.degrees(270))
                        VStack(spacing: 2) {
                            ForEach(indices, id: \.self) { idx in
                                sideSeatCard(idx: idx, player: player, isLocal: isLocal, seatCardW: seatCardW, seatCardH: seatCardH, edge: .east)
                            }
                        }
                    }
                case .west:
                    HStack(alignment: .center, spacing: 4) {
                        VStack(spacing: 2) {
                            ForEach(indices, id: \.self) { idx in
                                sideSeatCard(idx: idx, player: player, isLocal: isLocal, seatCardW: seatCardW, seatCardH: seatCardH, edge: .west)
                            }
                        }
                        nameLabel.rotationEffect(.degrees(90))
                    }
                }
            } else {
                Text("Empty")
                    .font(.system(size: 9, weight: .semibold))
                    .foregroundColor(.white.opacity(0.5))
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(Color.white.opacity(0.06))
                    .clipShape(Capsule())
            }
        }
        .frame(width: frameWidth(for: position, seatCardW: seatCardW, seatCardH: seatCardH),
               height: frameHeight(for: position, seatCardW: seatCardW, seatCardH: seatCardH))
    }

    private func frameWidth(for position: TableSeatPosition, seatCardW: CGFloat, seatCardH: CGFloat) -> CGFloat {
        switch position {
        case .north, .south:
            return seatCardW * 4 + 14
        case .east, .west:
            // Rotated name + horizontal card column (landscape cards; long edge toward center)
            return seatCardH + 36
        }
    }

    private func frameHeight(for position: TableSeatPosition, seatCardW: CGFloat, seatCardH: CGFloat) -> CGFloat {
        switch position {
        case .north, .south:
            return seatCardH + 4 + 14 // card row + spacing + name
        case .east, .west:
            // Stack short edge (seatCardW) vertically
            return seatCardW * 4 + 6
        }
    }

    @ViewBuilder
    private func seatNameLabel(isLocal: Bool, player: Player) -> some View {
        Text(isLocal ? "You" : resolvedDisplayName(for: player))
            .font(.system(size: 10, weight: .semibold))
            .foregroundColor(
                isLocal
                    ? accentGold
                    : (viewModel.gameState.currentPlayerID == player.id ? accentGreen : .white)
            )
            .lineLimit(1)
            .minimumScaleFactor(0.7)
    }

    private func sideSeatCard(idx: Int, player: Player, isLocal: Bool, seatCardW: CGFloat, seatCardH: CGFloat, edge: SideSeatEdge) -> some View {
        let card = player.hand[idx]
        let revealedBase = isLocal ? shouldRevealOwnCard(at: idx) : shouldRevealOpponentCard(playerID: player.id, at: idx)
        let revealed = revealedBase || viewModel.gameState.winnerID != nil
        let edgeLandscape = edge != .none
        let w = edgeLandscape ? seatCardH : seatCardW
        let h = edgeLandscape ? seatCardW : seatCardH
        let faceTextRotation: Angle
        switch edge {
        case .none: faceTextRotation = .zero
        case .east: faceTextRotation = .degrees(-90)
        case .west: faceTextRotation = .degrees(90)
        }

        return cardView(
            text: card?.shortName ?? "",
            isFaceUp: card != nil && revealed,
            isEmpty: card == nil,
            isSelected: isLocal
                ? (idx == selectedOwnIndex)
                : (selectedOpponentID == player.id && selectedOpponentIndex == idx),
            width: w,
            height: h,
            faceTextRotation: faceTextRotation
        )
        .onTapGesture {
            if isLocal {
                selectedOwnIndex = idx
            } else {
                selectedOpponentID = player.id
                selectedOpponentIndex = idx
            }
        }
        .onTapGesture(count: 2) {
            if isLocal {
                tryPeekOwnCard(at: idx)
            }
        }
    }

    private func topOpponents(opW: CGFloat, opH: CGFloat) -> some View {
        let row1 = Array(opponents.prefix(3))
        return HStack(spacing: 8) {
            HStack(spacing: 8) {
                ForEach(Array(row1.enumerated()), id: \.element.id) { _, p in
                    opponentSlot(player: p, vertical: false, opW: opW, opH: opH)
                }
                ForEach(0..<(3 - row1.count), id: \.self) { _ in
                    emptySlot(opW: opW, opH: opH, vertical: false)
                }
            }
        }
    }

    private func sideOpponent(position: Int, opW: CGFloat, opH: CGFloat) -> some View {
        let sidePlayers = Array(opponents.dropFirst(3).prefix(2))
        if position < sidePlayers.count {
            return AnyView(opponentSlot(player: sidePlayers[position], vertical: true, opW: opW, opH: opH))
        }
        return AnyView(emptySlot(opW: opW, opH: opH, vertical: true))
    }

    private func opponentSlot(player: Player, vertical: Bool, opW: CGFloat, opH: CGFloat) -> some View {
        VStack(spacing: 3) {
            Text(resolvedDisplayName(for: player))
                .font(.system(size: 10, weight: .semibold))
                .foregroundColor(viewModel.gameState.currentPlayerID == player.id ? accentGreen : .white)
                .lineLimit(1)
            if vertical {
                VStack(spacing: 2) {
                    HStack(spacing: 2) { ForEach(0..<min(2, player.hand.count), id: \.self) { idx in opponentCard(player: player, index: idx, w: opW, h: opH) } }
                    HStack(spacing: 2) { ForEach(2..<min(4, player.hand.count), id: \.self) { idx in opponentCard(player: player, index: idx, w: opW, h: opH) } }
                }
            } else {
                HStack(spacing: 2) { ForEach(player.hand.indices, id: \.self) { idx in opponentCard(player: player, index: idx, w: opW, h: opH) } }
            }
        }
        .padding(5)
        .background(selectedOpponentID == player.id ? accentGold.opacity(0.1) : Color.clear)
        .cornerRadius(8)
    }

    private func opponentCard(player: Player, index: Int, w: CGFloat, h: CGFloat) -> some View {
        let card = player.hand[index]
        let revealed = shouldRevealOpponentCard(playerID: player.id, at: index) || viewModel.gameState.winnerID != nil
        return cardView(text: card?.shortName ?? "", isFaceUp: card != nil && revealed, isEmpty: card == nil, isSelected: selectedOpponentID == player.id && selectedOpponentIndex == index, width: w, height: h)
            .onTapGesture { selectedOpponentID = player.id; selectedOpponentIndex = index }
    }

    private func emptySlot(opW: CGFloat, opH: CGFloat, vertical: Bool) -> some View {
        VStack(spacing: 3) {
            Text("Empty").font(.system(size: 9)).foregroundColor(.white.opacity(0.4))
            if vertical {
                VStack(spacing: 2) { HStack(spacing: 2) { emptyCard(w: opW, h: opH); emptyCard(w: opW, h: opH) }; HStack(spacing: 2) { emptyCard(w: opW, h: opH); emptyCard(w: opW, h: opH) } }
            } else {
                HStack(spacing: 2) { ForEach(0..<4, id: \.self) { _ in emptyCard(w: opW, h: opH) } }
            }
        }
        .opacity(0.35)
    }

    private func centerPiles(compact: Bool, pileW: CGFloat) -> some View {
        let w: CGFloat = pileW
        let h = w * 1.4
        return HStack(spacing: compact ? 16 : 24) {
            VStack(spacing: 3) {
                Text("Discard").font(.system(size: 9)).foregroundColor(.white.opacity(0.6))
                if let top = viewModel.gameState.discardPile.last {
                    pileView(text: top.shortName, isFaceUp: true, width: w, height: h)
                } else {
                    pileView(text: "", isFaceUp: false, width: w, height: h, isEmpty: true)
                }
            }
            VStack(spacing: 3) {
                Text(viewModel.gameState.pendingDraw != nil ? "Drawn" : "Deck").font(.system(size: 9)).foregroundColor(viewModel.gameState.pendingDraw != nil ? accentGold : .white.opacity(0.6))
                if isMyTurn, let pending = viewModel.gameState.pendingDraw?.card {
                    pileView(text: pending.shortName, isFaceUp: true, width: w, height: h, highlight: true)
                } else {
                    pileView(text: "", isFaceUp: false, width: w, height: h)
                }
            }
        }
    }

    private func myHand(myW: CGFloat, myH: CGFloat, compact: Bool) -> some View {
        VStack(spacing: 4) {
            Text("You").font(.system(size: 11, weight: .bold)).foregroundColor(isMyTurn ? accentGold : .white)
            if let me = localPlayer {
                HStack(spacing: compact ? 6 : 8) {
                    ForEach(me.hand.indices, id: \.self) { idx in
                        let card = me.hand[idx]
                        let revealed = shouldRevealOwnCard(at: idx) || viewModel.gameState.winnerID != nil
                        cardView(text: card?.shortName ?? "", isFaceUp: card != nil && revealed, isEmpty: card == nil, isSelected: idx == selectedOwnIndex, width: myW, height: myH)
                            .onTapGesture { selectedOwnIndex = idx }
                            .onTapGesture(count: 2) { tryPeekOwnCard(at: idx) }
                    }
                }
            }
        }
        .padding(.vertical, 8).padding(.horizontal, 12)
        .background(isMyTurn ? accentGold.opacity(0.12) : Color.white.opacity(0.05))
        .cornerRadius(10)
    }

    private var statusBanner: some View {
        Group {
            if let winnerID = viewModel.gameState.winnerID,
               let winner = viewModel.gameState.players.first(where: { $0.id == winnerID }) {
                statusPill("Winner: \(winner.name) (\(winner.score()) pts)", color: .mint)
            } else if let callerID = viewModel.gameState.caboCallerID,
                      let caller = viewModel.gameState.players.first(where: { $0.id == callerID }) {
                statusPill("Cabo! \(caller.name) - Final turns", color: .orange)
            } else if viewModel.gameState.phase == .waitingForSpecialResolution && isMyTurn {
                // Only the player resolving the special should see the
                // explanation text; opponents shouldn't be told what the
                // active card is.
                statusPill(powerTextForSpecial(), color: accentGreen)
            } else {
                EmptyView()
            }
        }
    }

    private var matchResultBanner: some View {
        Group {
            if let matchStatus = viewModel.matchDiscardStatusText {
                let isCorrect = matchStatus.lowercased().hasPrefix("correct")
                statusPill(matchStatus, color: isCorrect ? accentGreen : .red)
            } else {
                EmptyView()
            }
        }
    }

    private func powerTextForSpecial() -> String {
        guard let rank = activeSpecialRank else { return "Resolve power" }
        switch rank {
        case .king:
            return "K: Swap one of your cards with any opponents card"
        case .jack:
            return "J: Peek at one of your cards"
        case .queen:
            return "Q: Peek at one of any opponents card"
        default:
            return "\(rank.display): resolve power"
        }
    }

    private func controls(compact: Bool) -> some View {
        let h: CGFloat = compact ? 36 : 40
        let fs: CGFloat = compact ? 11 : 12
        return Group {
            if viewModel.gameState.winnerID != nil {
                HStack {
                    if viewModel.isHostUser {
                        action("New Game", primary: true, h: h, fs: fs) { viewModel.startNewGameRound() }
                    } else {
                        action(viewModel.isLocalPlayerReadyForNewGame ? "Ready!" : "Ready", primary: true, h: h, fs: fs) { viewModel.toggleReadyForNewGame() }
                            .disabled(!viewModel.gameState.rematchRequestedByHost)
                    }
                }
            } else if viewModel.gameState.phase == .waitingForSpecialResolution && isMyTurn {
                specialResolutionControls(compact: compact, h: h, fs: fs)
            } else {
                VStack(spacing: 6) {
                    HStack(spacing: 6) {
                        action("Draw Deck", primary: true, h: h, fs: fs) { viewModel.draw(from: .deck) }.disabled(!isMyTurn || viewModel.gameState.phase != .waitingForDraw)
                        action("Draw Discard", primary: false, h: h, fs: fs) { viewModel.draw(from: .discardTop) }.disabled(!isMyTurn || viewModel.gameState.phase != .waitingForDraw)
                        action("Match", primary: false, h: h, fs: fs) { viewModel.attemptMatchDiscard(withOwnCardAt: selectedOwnIndex) }.disabled(viewModel.gameState.phase == .initialPeek || localPlayer == nil)
                    }
                    HStack(spacing: 6) {
                        action("Replace", primary: true, h: h, fs: fs) { viewModel.replaceWithDrawnCard(at: selectedOwnIndex) }.disabled(!isMyTurn || viewModel.gameState.phase != .waitingForPlacementOrDiscard)
                        action("Discard", primary: false, h: h, fs: fs) { viewModel.discardForEffect() }.disabled(!isMyTurn || viewModel.gameState.phase != .waitingForPlacementOrDiscard)
                        action("Cabo!", primary: false, h: h, fs: fs, accent: accentGold) { viewModel.callCabo() }.disabled(!isMyTurn || viewModel.gameState.phase != .waitingForDraw || viewModel.gameState.pendingDraw != nil || viewModel.gameState.caboCallerID != nil)
                    }
                }
            }
        }
    }

    private func specialResolutionControls(compact: Bool, h: CGFloat, fs: CGFloat) -> some View {
        let rank = activeSpecialRank
        switch rank {
        case .king:
            return AnyView(HStack(spacing: 8) {
                action("Swap", primary: true, h: h, fs: fs, accent: accentGreen) {
                    guard let me = localPlayer else { return }
                    guard let targetID = selectedOpponentID else { return }
                    guard me.hand.indices.contains(selectedOwnIndex), me.hand[selectedOwnIndex] != nil else { return }
                    guard let target = viewModel.gameState.players.first(where: { $0.id == targetID }),
                          target.hand.indices.contains(selectedOpponentIndex),
                          target.hand[selectedOpponentIndex] != nil else { return }

                    viewModel.resolveSpecial(
                        .swapCards(fromPlayerID: me.id, fromCardIndex: selectedOwnIndex, toPlayerID: targetID, toCardIndex: selectedOpponentIndex)
                    )
                }
                .disabled(selectedOpponentID == nil || localPlayer == nil)
                Text("Tap a card + opponent card, then swap.")
                    .foregroundColor(.white.opacity(0.55))
                    .font(.system(size: fs))
                    .frame(maxWidth: .infinity)
            })
        case .jack:
            return AnyView(HStack(spacing: 8) {
                action("Peek", primary: true, h: h, fs: fs, accent: accentGold) {
                    guard let me = localPlayer else { return }
                    guard me.hand.indices.contains(selectedOwnIndex), me.hand[selectedOwnIndex] != nil else { return }
                    viewModel.resolveSpecial(.lookOwnCard(playerID: me.id, cardIndex: selectedOwnIndex))
                }
                .disabled(
                    localPlayer == nil ||
                    !(localPlayer?.hand.indices.contains(selectedOwnIndex) ?? false) ||
                    localPlayer?.hand[selectedOwnIndex] == nil
                )
                Text("Choose a card").foregroundColor(.white.opacity(0.5)).font(.system(size: fs))
                    .frame(maxWidth: .infinity)
            })
        case .queen:
            return AnyView(HStack(spacing: 8) {
                action("Peek", primary: true, h: h, fs: fs, accent: accentGold) {
                    guard let targetID = selectedOpponentID else { return }
                    let targetPlayer = viewModel.gameState.players.first(where: { $0.id == targetID })
                    guard let target = targetPlayer else { return }
                    guard target.hand.indices.contains(selectedOpponentIndex), target.hand[selectedOpponentIndex] != nil else { return }
                    viewModel.resolveSpecial(.lookOtherCard(targetPlayerID: targetID, cardIndex: selectedOpponentIndex))
                }
                .disabled(selectedOpponentID == nil)
                Text("Choose an opponent card").foregroundColor(.white.opacity(0.5)).font(.system(size: fs))
                    .frame(maxWidth: .infinity)
            })
        default:
            return AnyView(EmptyView())
        }
    }

    private func action(_ title: String, primary: Bool, h: CGFloat, fs: CGFloat, accent: Color? = nil, onTap: @escaping () -> Void) -> some View {
        Button(action: onTap) {
            Text(title).font(.system(size: fs, weight: .semibold)).foregroundColor(primary ? bgDark : .white)
                .frame(maxWidth: .infinity).frame(height: h)
                .background(primary ? (accent ?? accentGreen) : Color.white.opacity(0.12))
                .cornerRadius(8)
        }.buttonStyle(.plain)
    }

    private func cardView(text: String, isFaceUp: Bool, isEmpty: Bool, isSelected: Bool, width: CGFloat, height: CGFloat, faceTextRotation: Angle = .zero) -> some View {
        ZStack {
            RoundedRectangle(cornerRadius: 5).fill(isEmpty ? Color.white.opacity(0.05) : (isFaceUp ? .white : cardBack))
            RoundedRectangle(cornerRadius: 5).stroke(isSelected ? accentGold : Color.white.opacity(0.2), lineWidth: isSelected ? 2 : 0.8)
            if isEmpty {
                RoundedRectangle(cornerRadius: 4).stroke(style: StrokeStyle(lineWidth: 0.8, dash: [3, 3])).foregroundColor(.white.opacity(0.2)).padding(3)
            } else if isFaceUp {
                HStack(spacing: 0) {
                    Text(String(text.dropLast(cardSuit(from: text).count)))
                    Text(cardSuit(from: text))
                }
                .lineLimit(1).minimumScaleFactor(0.7)
                .font(.system(size: width > 44 ? (height <= 34 ? 11 : 14) : 10, weight: .bold))
                .foregroundColor(cardColor(text))
                .rotationEffect(faceTextRotation)
            }
        }.frame(width: width, height: height)
    }

    private func pileView(text: String, isFaceUp: Bool, width: CGFloat, height: CGFloat, isEmpty: Bool = false, highlight: Bool = false) -> some View {
        ZStack {
            RoundedRectangle(cornerRadius: 7).fill(isEmpty ? Color.white.opacity(0.08) : (isFaceUp ? .white : cardBack))
            RoundedRectangle(cornerRadius: 7).stroke(highlight ? accentGold : Color.white.opacity(0.25), lineWidth: highlight ? 2.5 : 1)
            if isEmpty {
                RoundedRectangle(cornerRadius: 5).stroke(style: StrokeStyle(lineWidth: 1, dash: [4, 4])).foregroundColor(.white.opacity(0.2)).padding(4)
            } else if isFaceUp {
                HStack(spacing: 0) {
                    Text(String(text.dropLast(cardSuit(from: text).count)))
                    Text(cardSuit(from: text))
                }
                .lineLimit(1).minimumScaleFactor(0.7)
                .font(.system(size: 16, weight: .bold)).foregroundColor(cardColor(text))
            }
        }.frame(width: width, height: height).shadow(color: .black.opacity(0.3), radius: 4, y: 2)
    }

    private func emptyCard(w: CGFloat, h: CGFloat) -> some View {
        RoundedRectangle(cornerRadius: 5).stroke(style: StrokeStyle(lineWidth: 0.8, dash: [3, 3])).foregroundColor(.white.opacity(0.15)).frame(width: w, height: h)
    }

    private func statusPill(_ text: String, color: Color) -> some View {
        Text(text).font(.system(size: 12, weight: .semibold)).foregroundColor(.white).padding(.horizontal, 12).padding(.vertical, 6).background(color.opacity(0.3)).cornerRadius(8)
    }

    private func cardSuit(from text: String) -> String {
        guard let last = text.last, "♥♦♣♠".contains(last) else { return "" }
        return String(last)
    }

    private func cardColor(_ text: String) -> Color { (text.contains("♥") || text.contains("♦")) ? .red : .black }
    private var localPlayer: Player? {
        guard let localID = viewModel.localPlayerID else { return nil }
        return viewModel.gameState.players.first { $0.id == localID }
    }
    private var opponents: [Player] {
        guard let localID = viewModel.localPlayerID else { return [] }
        return viewModel.gameState.players.filter { $0.id != localID }
    }
    private var isMyTurn: Bool { viewModel.gameState.currentPlayerID == viewModel.localPlayerID }
    private var currentTurnName: String {
        guard let currentID = viewModel.gameState.currentPlayerID,
              let player = viewModel.gameState.players.first(where: { $0.id == currentID }) else { return "..." }
        return resolvedDisplayName(for: player)
    }

    private func resolvedDisplayName(for player: Player) -> String {
        // Some sessions may end up with placeholder `"Player"` for the other side.
        // If we can infer the real name from connected peers, prefer that for display.
        guard player.name == "Player" else { return player.name }

        let localName = viewModel.playerName.trimmingCharacters(in: .whitespacesAndNewlines)
        let peerCandidates = viewModel.peers
            .map { $0.displayName.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty && $0 != localName }

        if peerCandidates.count == 1 {
            return peerCandidates[0]
        }
        return player.name
    }
    private var activeSpecialRank: Rank? {
        guard viewModel.gameState.phase == .waitingForSpecialResolution,
              let rank = viewModel.gameState.discardPile.last?.rank else { return nil }
        switch rank { case .jack, .queen, .king: return rank; default: return nil }
    }
    private var remainingInitialPeeks: Int { max(0, 2 - viewModel.initialPeekedOwnIndices.count) }
    private func shouldRevealOwnCard(at index: Int) -> Bool {
        if viewModel.gameState.phase == .initialPeek && viewModel.initialPeekedOwnIndices.contains(index) { return true }
        return viewModel.jackPeekedOwnIndex == index
    }
    private func shouldRevealOpponentCard(playerID: UUID, at index: Int) -> Bool {
        viewModel.queenPeekedOpponentPlayerID == playerID && viewModel.queenPeekedOpponentIndex == index
    }

    private func tryPeekOwnCard(at index: Int) {
        guard viewModel.gameState.phase == .initialPeek else { return }
        guard remainingInitialPeeks > 0 else { return }
        // During initial peek, peeking should not depend on "whose turn" anymore.
        if let secs = viewModel.initialPeekSecondsRemaining, secs == 0 { return }
        viewModel.peekInitialCard(at: index)
    }
}