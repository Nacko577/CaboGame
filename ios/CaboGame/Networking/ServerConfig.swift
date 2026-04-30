import Foundation

// Single source of truth for the relay server URL.
//
// 1) For local testing against a dev server running on your Mac, set
//    `defaultLocalDevURL` to your Mac's LAN IP (NOT `localhost`, since the
//    phone needs to reach it).
// 2) For production / friends-across-the-internet play, set
//    `defaultProductionURL` to your deployed server (must be `wss://...` so
//    iOS App Transport Security accepts it).
// 3) Override at runtime by setting the user-default key
//    `cabogame.serverURL` (handy for testing without rebuilding).
enum ServerConfig {
    static let defaultProductionURL = URL(string: "wss://YOUR-CABO-RELAY.example.com/ws")!
    static let defaultLocalDevURL = URL(string: "ws://192.168.1.1:8080/ws")!

    static var current: URL {
        if let override = UserDefaults.standard.string(forKey: "cabogame.serverURL"),
           let url = URL(string: override) {
            return url
        }
        return defaultProductionURL
    }
}
