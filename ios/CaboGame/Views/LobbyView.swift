import SwiftUI

struct LobbyView: View {
    @EnvironmentObject private var viewModel: GameViewModel
    let onBack: () -> Void

    private var trimmedPlayerName: String {
        viewModel.playerName.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var body: some View {
        ZStack {
            // Rich gradient background
            LinearGradient(
                colors: [
                    Color(red: 0.08, green: 0.12, blue: 0.10),
                    Color(red: 0.04, green: 0.08, blue: 0.06)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
            
            ScrollView {
                VStack(spacing: 20) {
                    // Header
                    HStack {
                        Button(action: onBack) {
                            HStack(spacing: 6) {
                                Image(systemName: "chevron.left")
                                    .font(.system(size: 14, weight: .semibold))
                                Text("Back")
                                    .font(.system(size: 15, weight: .semibold, design: .rounded))
                            }
                            .foregroundColor(.white.opacity(0.85))
                            .padding(.horizontal, 14)
                            .padding(.vertical, 10)
                            .background(Color.white.opacity(0.08))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                        }
                        
                        Spacer()
                        
                        Text("Game Lobby")
                            .font(.system(size: 20, weight: .bold, design: .rounded))
                            .foregroundStyle(
                                LinearGradient(
                                    colors: [
                                        Color(red: 0.95, green: 0.90, blue: 0.80),
                                        Color(red: 0.85, green: 0.78, blue: 0.65)
                                    ],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                        
                        Spacer()
                        
                        // Balance spacing
                        Color.clear
                            .frame(width: 70)
                    }
                    .padding(.horizontal, 4)
                    .padding(.top, 8)
                    
                    // Player Name Card
                    LobbyCard(icon: "person.fill", title: "Your Name", accentColor: Color(red: 0.18, green: 0.75, blue: 0.55)) {
                        let nameLocked = viewModel.hostedCode != nil || !viewModel.peers.isEmpty
                        TextField("", text: $viewModel.playerName, prompt: Text("Enter your name").foregroundColor(.white.opacity(0.35)))
                            .textInputAutocapitalization(.words)
                            .font(.system(size: 16, weight: .medium, design: .rounded))
                            .foregroundStyle(.white.opacity(nameLocked ? 0.5 : 1.0))
                            .padding(14)
                            .background(Color.white.opacity(0.06))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(Color.white.opacity(0.1), lineWidth: 1)
                            )
                            .disabled(nameLocked)
                    }

                    // Transport Card (Online vs Same Wi-Fi)
                    LobbyCard(icon: "network", title: "Connection", accentColor: Color(red: 0.55, green: 0.75, blue: 0.95)) {
                        HStack(spacing: 8) {
                            ForEach(LobbyTransport.allCases) { option in
                                Button {
                                    viewModel.switchTransport(option)
                                } label: {
                                    Text(option.label)
                                        .font(.system(size: 14, weight: .semibold, design: .rounded))
                                        .foregroundColor(viewModel.transport == option ? Color(red: 0.06, green: 0.10, blue: 0.08) : .white.opacity(0.75))
                                        .frame(maxWidth: .infinity)
                                        .frame(height: 40)
                                        .background(
                                            viewModel.transport == option ?
                                            AnyShapeStyle(Color(red: 0.55, green: 0.75, blue: 0.95)) :
                                            AnyShapeStyle(Color.white.opacity(0.06))
                                        )
                                        .clipShape(RoundedRectangle(cornerRadius: 10))
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 10)
                                                .stroke(Color.white.opacity(viewModel.transport == option ? 0 : 0.1), lineWidth: 1)
                                        )
                                }
                                .buttonStyle(ScaleButtonStyle())
                                .disabled(viewModel.hostedCode != nil || !viewModel.peers.isEmpty)
                            }
                        }

                        Text(viewModel.transport == .online
                             ? "Play across the internet via the relay server."
                             : "Both players must be on the same Wi-Fi network.")
                            .font(.system(size: 12, weight: .medium, design: .rounded))
                            .foregroundStyle(.white.opacity(0.4))
                    }
                    
                    // Host or Join Section
                    HStack(spacing: 12) {
                        // Host Card
                        LobbyCard(icon: "crown.fill", title: "Host", accentColor: Color(red: 0.95, green: 0.75, blue: 0.30)) {
                            TextField(
                                "",
                                text: Binding(
                                    get: { viewModel.hostedCode ?? "" },
                                    set: { _ in }
                                ),
                                prompt: Text("CODE").foregroundColor(.white.opacity(0.35))
                            )
                            .textInputAutocapitalization(.characters)
                            .autocorrectionDisabled()
                            .multilineTextAlignment(.center)
                            .font(.system(size: 16, weight: .bold, design: .monospaced))
                            .foregroundStyle(.white)
                            .padding(12)
                            .background(Color.white.opacity(0.06))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(Color.white.opacity(0.1), lineWidth: 1)
                            )
                            .disabled(true)
                            
                            Button(action: { viewModel.hostLobby() }) {
                                Text("Create Game")
                                    .font(.system(size: 14, weight: .bold, design: .rounded))
                                    .foregroundColor(Color(red: 0.06, green: 0.10, blue: 0.08))
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 44)
                                    .background(
                                        LinearGradient(
                                            colors: [
                                                Color(red: 0.95, green: 0.75, blue: 0.30),
                                                Color(red: 0.90, green: 0.65, blue: 0.20)
                                            ],
                                            startPoint: .top,
                                            endPoint: .bottom
                                        )
                                    )
                                    .clipShape(RoundedRectangle(cornerRadius: 10))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 10)
                                            .stroke(Color(red: 0.95, green: 0.75, blue: 0.30).opacity(0.5), lineWidth: 1)
                                    )
                            }
                            .buttonStyle(ScaleButtonStyle())
                            .disabled(viewModel.hostedCode != nil || trimmedPlayerName.isEmpty)
                        }
                        
                        // Join Card
                        LobbyCard(icon: "arrow.right.circle.fill", title: "Join", accentColor: Color(red: 0.55, green: 0.75, blue: 0.95)) {
                            TextField("", text: $viewModel.joinCodeInput, prompt: Text("CODE").foregroundColor(.white.opacity(0.35)))
                                .textInputAutocapitalization(.characters)
                                .autocorrectionDisabled()
                                .multilineTextAlignment(.center)
                                .font(.system(size: 16, weight: .bold, design: .monospaced))
                                .foregroundStyle(.white)
                                .padding(12)
                                .background(Color.white.opacity(0.06))
                                .clipShape(RoundedRectangle(cornerRadius: 10))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 10)
                                        .stroke(Color.white.opacity(0.1), lineWidth: 1)
                                )
                            
                            Button(action: { viewModel.joinLobby() }) {
                                Text("Join")
                                    .font(.system(size: 14, weight: .bold, design: .rounded))
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 44)
                                    .background(Color(red: 0.55, green: 0.75, blue: 0.95).opacity(0.25))
                                    .clipShape(RoundedRectangle(cornerRadius: 10))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 10)
                                            .stroke(Color(red: 0.55, green: 0.75, blue: 0.95).opacity(0.5), lineWidth: 1)
                                    )
                            }
                            .buttonStyle(ScaleButtonStyle())
                            .disabled(trimmedPlayerName.isEmpty || viewModel.joinCodeInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                        }
                    }
                    
                    // Lobby Status Card
                    LobbyCard(icon: "wifi", title: "Connection Status", accentColor: viewModel.peers.isEmpty ? Color.white.opacity(0.5) : Color(red: 0.18, green: 0.75, blue: 0.55)) {
                        HStack(spacing: 8) {
                            Circle()
                                .fill(viewModel.peers.isEmpty ? Color.white.opacity(0.3) : Color(red: 0.18, green: 0.75, blue: 0.55))
                                .frame(width: 8, height: 8)
                            Text(viewModel.statusText)
                                .font(.system(size: 14, weight: .medium, design: .rounded))
                                .foregroundStyle(.white.opacity(0.75))
                        }
                        
                        if viewModel.peers.isEmpty {
                            Text("Waiting for players...")
                                .font(.system(size: 13, weight: .regular, design: .rounded))
                                .foregroundStyle(.white.opacity(0.4))
                                .padding(.top, 4)
                        } else {
                            VStack(alignment: .leading, spacing: 8) {
                                ForEach(viewModel.peers) { peer in
                                    HStack(spacing: 10) {
                                        Circle()
                                            .fill(Color(red: 0.18, green: 0.75, blue: 0.55))
                                            .frame(width: 6, height: 6)
                                        Text(peer.displayName)
                                            .font(.system(size: 14, weight: .medium, design: .rounded))
                                            .foregroundStyle(.white.opacity(0.85))
                                    }
                                }
                            }
                            .padding(.top, 4)
                        }
                    }
                    
                    // Start Game Section (Host only)
                    if viewModel.hostedCode != nil {
                        LobbyCard(icon: "play.fill", title: "Start Game", accentColor: Color(red: 0.18, green: 0.75, blue: 0.55)) {
                            let playerCount = viewModel.gameState.players.count
                            
                            HStack {
                                Text("Players Ready")
                                    .font(.system(size: 14, weight: .medium, design: .rounded))
                                    .foregroundStyle(.white.opacity(0.6))
                                Spacer()
                                Text("\(playerCount)")
                                    .font(.system(size: 20, weight: .bold, design: .rounded))
                                    .foregroundStyle(playerCount >= 2 ? Color(red: 0.18, green: 0.75, blue: 0.55) : .white.opacity(0.4))
                            }
                            
                            Button(action: { viewModel.startGameAsHost() }) {
                                HStack(spacing: 10) {
                                    Image(systemName: "play.fill")
                                        .font(.system(size: 14, weight: .semibold))
                                    Text("Start Game")
                                        .font(.system(size: 16, weight: .bold, design: .rounded))
                                }
                                .foregroundColor(playerCount >= 2 ? Color(red: 0.06, green: 0.10, blue: 0.08) : .white.opacity(0.4))
                                .frame(maxWidth: .infinity)
                                .frame(height: 52)
                                .background(
                                    playerCount >= 2 ?
                                    AnyShapeStyle(LinearGradient(
                                        colors: [
                                            Color(red: 0.18, green: 0.75, blue: 0.55),
                                            Color(red: 0.14, green: 0.62, blue: 0.45)
                                        ],
                                        startPoint: .top,
                                        endPoint: .bottom
                                    )) :
                                    AnyShapeStyle(Color.white.opacity(0.1))
                                )
                                .clipShape(RoundedRectangle(cornerRadius: 14))
                            }
                            .disabled(playerCount < 2)
                            .buttonStyle(ScaleButtonStyle())
                            
                            if playerCount < 2 {
                                Text("Need at least 2 players to start")
                                    .font(.system(size: 12, weight: .medium, design: .rounded))
                                    .foregroundStyle(.white.opacity(0.4))
                                    .multilineTextAlignment(.center)
                            }
                        }
                    }
                    
                    // Error Section
                    if let error = viewModel.lastError {
                        LobbyCard(icon: "exclamationmark.triangle.fill", title: "Error", accentColor: Color(red: 0.95, green: 0.40, blue: 0.40)) {
                            Text(error)
                                .font(.system(size: 14, weight: .medium, design: .rounded))
                                .foregroundStyle(Color(red: 0.95, green: 0.40, blue: 0.40))
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 32)
            }
        }
        .navigationBarBackButtonHidden(true)
    }
}

// Reusable Lobby Card Component
struct LobbyCard<Content: View>: View {
    let icon: String
    let title: String
    let accentColor: Color
    @ViewBuilder let content: Content
    
    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 10) {
                Image(systemName: icon)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(accentColor)
                
                Text(title)
                    .font(.system(size: 14, weight: .bold, design: .rounded))
                    .foregroundStyle(.white.opacity(0.9))
                    .textCase(.uppercase)
                    .tracking(0.5)
            }
            
            content
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.white.opacity(0.04))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color.white.opacity(0.06), lineWidth: 1)
        )
    }
}