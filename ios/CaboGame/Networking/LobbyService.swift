import Foundation

enum LobbyRole {
    case host
    case guest
}

struct LobbyPeer: Identifiable, Equatable {
    let id: String
    let displayName: String
}

enum NetworkMessage: Codable {
    case lobbyState(players: [String], hostName: String)
    case gameState(GameState)
    case playerAction(PlayerNetworkAction)
    case startGame
}

enum PlayerNetworkAction: Codable {
    case initialPeek(playerID: UUID, index: Int)
    case startNewGameRound(playerID: UUID)
    case setReadyForNewGame(playerID: UUID, isReady: Bool)
    case attemptMatchDiscard(playerID: UUID, index: Int)
    case draw(playerID: UUID, source: DrawSource)
    case replace(playerID: UUID, index: Int)
    case discardForEffect(playerID: UUID)
    case resolveSpecial(playerID: UUID, action: SpecialAction)
    case callCabo(playerID: UUID)
}

protocol LobbyServiceDelegate: AnyObject {
    func lobbyServiceDidUpdatePeers(_ peers: [LobbyPeer])
    func lobbyServiceDidReceive(message: NetworkMessage, from peerID: String)
    func lobbyServiceDidChangeConnectionState(_ text: String)
}

protocol LobbyService {
    var delegate: LobbyServiceDelegate? { get set }
    var joinCode: String? { get }

    func startHosting(displayName: String) throws
    func startJoining(displayName: String, code: String) throws
    func stop()
    func send(_ message: NetworkMessage) throws
}
