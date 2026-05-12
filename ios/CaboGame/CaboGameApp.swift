import SwiftUI

@main
struct CaboGameApp: App {
    @StateObject private var viewModel = GameViewModel()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(viewModel)
                .onAppear {
                    let ud = UserDefaults.standard
                    if ud.string(forKey: "cabogame.boardTheme") == "midnight" {
                        ud.set(BoardTheme.amethyst.rawValue, forKey: "cabogame.boardTheme")
                    }
                    if ud.string(forKey: "cabogame.cardDeckStyle") == "amethyst" {
                        ud.set(CardDeckStyle.velvet.rawValue, forKey: "cabogame.cardDeckStyle")
                    }
                }
        }
    }
}
