import SwiftUI

struct LobbyView: View {
    @EnvironmentObject private var viewModel: GameViewModel
    let onBack: () -> Void

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(red: 0.06, green: 0.34, blue: 0.22), Color(red: 0.04, green: 0.24, blue: 0.16)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 12) {
                    HStack {
                        Button("Back") {
                            onBack()
                        }
                        .buttonStyle(.bordered)
                        .tint(.white)
                        Spacer()
                    }

                    panel {
                        Text("Player")
                            .font(.headline)
                            .foregroundStyle(.white)
                        TextField("Your name", text: $viewModel.playerName)
                            .textInputAutocapitalization(.words)
                            .padding(10)
                            .background(Color.white.opacity(0.12))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                            .foregroundStyle(.white)
                    }

                    panel {
                        Text("Host")
                            .font(.headline)
                            .foregroundStyle(.white)
                        Button("Host Game") {
                            viewModel.hostLobby()
                        }
                        .buttonStyle(.borderedProminent)
                        if let code = viewModel.hostedCode {
                            Text("Join code: \(code)")
                                .font(.title3.weight(.bold))
                                .foregroundStyle(.yellow)
                        }
                    }

                    panel {
                        Text("Join")
                            .font(.headline)
                            .foregroundStyle(.white)
                        TextField("Code", text: $viewModel.joinCodeInput)
                            .textInputAutocapitalization(.characters)
                            .autocorrectionDisabled()
                            .padding(10)
                            .background(Color.white.opacity(0.12))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                            .foregroundStyle(.white)
                        Button("Join by Code") {
                            viewModel.joinLobby()
                        }
                        .buttonStyle(.bordered)
                        .tint(.white)
                    }

                    panel {
                        Text("Lobby")
                            .font(.headline)
                            .foregroundStyle(.white)
                        Text(viewModel.statusText)
                            .foregroundStyle(.white.opacity(0.85))
                        if viewModel.peers.isEmpty {
                            Text("No peers connected")
                                .foregroundStyle(.white.opacity(0.65))
                        } else {
                            ForEach(viewModel.peers) { peer in
                                Text("• \(peer.displayName)")
                                    .foregroundStyle(.white.opacity(0.9))
                            }
                        }
                    }

                    if viewModel.hostedCode != nil {
                        panel {
                            Text("Game Setup")
                                .font(.headline)
                                .foregroundStyle(.white)
                            let playerCount = viewModel.gameState.players.count
                            Text("Players: \(playerCount)")
                                .foregroundStyle(.white.opacity(0.85))
                            Button("Start Game") {
                                viewModel.startGameAsHost()
                            }
                            .buttonStyle(.borderedProminent)
                            .disabled(playerCount < 2)
                        }
                    }

                    if let error = viewModel.lastError {
                        panel {
                            Text("Error")
                                .font(.headline)
                                .foregroundStyle(.white)
                            Text(error)
                                .foregroundStyle(.red)
                        }
                }
                }
            }
        }
        .navigationTitle("Find Lobby")
        .navigationBarBackButtonHidden(true)
    }

    private func panel<Content: View>(@ViewBuilder _ content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 8, content: content)
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white.opacity(0.10))
            .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
