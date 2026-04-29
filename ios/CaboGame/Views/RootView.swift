import SwiftUI

struct RootView: View {
    @EnvironmentObject private var viewModel: GameViewModel
    @State private var screen: RootScreen = .loading

    enum RootScreen {
        case loading
        case menu
        case lobby
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
    }
}
