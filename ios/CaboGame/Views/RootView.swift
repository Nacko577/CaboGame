import SwiftUI
import UIKit

struct RootView: View {
    @EnvironmentObject private var viewModel: GameViewModel
    @Environment(\.scenePhase) private var scenePhase
    @State private var screen: RootScreen = .loading
    @State private var lobbyBackgroundTask: UIBackgroundTaskIdentifier = .invalid

    enum RootScreen {
        case loading
        case menu
        case lobby
    }

    private var shouldExtendBackgroundExecution: Bool {
        viewModel.hostedCode != nil
            || viewModel.gameState.hasStarted
            || !viewModel.peers.isEmpty
    }

    var body: some View {
        NavigationStack {
            switch screen {
            case .loading:
                SplashView()
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 1.8) {
                            withAnimation(.easeInOut(duration: 0.35)) {
                                screen = .menu
                            }
                        }
                    }
            case .menu:
                MainMenuView(onPlay: { screen = .lobby })
            case .lobby:
                if viewModel.gameState.hasStarted {
                    GameTableView(onLeaveGame: {
                        viewModel.leaveLobby()
                    })
                } else {
                    LobbyView(onBack: {
                        viewModel.leaveLobby()
                        screen = .menu
                    })
                }
            }
        }
        .onChange(of: scenePhase) { _, phase in
            switch phase {
            case .background, .inactive:
                guard shouldExtendBackgroundExecution else { return }
                if lobbyBackgroundTask == .invalid {
                    lobbyBackgroundTask = UIApplication.shared.beginBackgroundTask {
                        if lobbyBackgroundTask != .invalid {
                            UIApplication.shared.endBackgroundTask(lobbyBackgroundTask)
                            lobbyBackgroundTask = .invalid
                        }
                    }
                }
            case .active:
                if lobbyBackgroundTask != .invalid {
                    UIApplication.shared.endBackgroundTask(lobbyBackgroundTask)
                    lobbyBackgroundTask = .invalid
                }
            @unknown default:
                break
            }
        }
    }
}

