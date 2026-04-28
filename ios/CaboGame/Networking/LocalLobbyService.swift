import Foundation
import MultipeerConnectivity

final class LocalLobbyService: NSObject, LobbyService {
    weak var delegate: LobbyServiceDelegate?
    private(set) var joinCode: String?

    private var role: LobbyRole?
    private var myPeerID: MCPeerID?
    private var session: MCSession?
    private var advertiser: MCNearbyServiceAdvertiser?
    private var browser: MCNearbyServiceBrowser?
    private var discoveredHostByCode: [String: MCPeerID] = [:]

    private let serviceType = "cabo-local"

    func startHosting(displayName: String) throws {
        cleanup()
        role = .host

        let code = Self.makeCode()
        joinCode = code

        let peerID = MCPeerID(displayName: displayName)
        myPeerID = peerID

        let session = MCSession(peer: peerID, securityIdentity: nil, encryptionPreference: .required)
        session.delegate = self
        self.session = session

        let advertiser = MCNearbyServiceAdvertiser(
            peer: peerID,
            discoveryInfo: ["code": code],
            serviceType: serviceType
        )
        advertiser.delegate = self
        advertiser.startAdvertisingPeer()
        self.advertiser = advertiser

        delegate?.lobbyServiceDidChangeConnectionState("Hosting with code \(code)")
    }

    func startJoining(displayName: String, code: String) throws {
        cleanup()
        role = .guest
        joinCode = code.uppercased()

        let peerID = MCPeerID(displayName: displayName)
        myPeerID = peerID

        let session = MCSession(peer: peerID, securityIdentity: nil, encryptionPreference: .required)
        session.delegate = self
        self.session = session

        let browser = MCNearbyServiceBrowser(peer: peerID, serviceType: serviceType)
        browser.delegate = self
        browser.startBrowsingForPeers()
        self.browser = browser

        delegate?.lobbyServiceDidChangeConnectionState("Searching for host...")
    }

    func stop() {
        cleanup()
        delegate?.lobbyServiceDidChangeConnectionState("Disconnected")
    }

    func send(_ message: NetworkMessage) throws {
        guard let session else { return }
        guard !session.connectedPeers.isEmpty else { return }
        let data = try JSONEncoder().encode(message)
        try session.send(data, toPeers: session.connectedPeers, with: .reliable)
    }

    private func cleanup() {
        advertiser?.stopAdvertisingPeer()
        browser?.stopBrowsingForPeers()
        session?.disconnect()

        advertiser = nil
        browser = nil
        session = nil
        myPeerID = nil
        discoveredHostByCode = [:]
    }

    private static func makeCode() -> String {
        let letters = Array("ABCDEFGHJKLMNPQRSTUVWXYZ23456789")
        return String((0..<5).map { _ in letters.randomElement()! })
    }
}

extension LocalLobbyService: MCNearbyServiceAdvertiserDelegate {
    func advertiser(
        _ advertiser: MCNearbyServiceAdvertiser,
        didReceiveInvitationFromPeer peerID: MCPeerID,
        withContext context: Data?,
        invitationHandler: @escaping (Bool, MCSession?) -> Void
    ) {
        invitationHandler(true, session)
        delegate?.lobbyServiceDidChangeConnectionState("\(peerID.displayName) joined")
    }
}

extension LocalLobbyService: MCNearbyServiceBrowserDelegate {
    func browser(_ browser: MCNearbyServiceBrowser, foundPeer peerID: MCPeerID, withDiscoveryInfo info: [String: String]?) {
        guard role == .guest else { return }
        guard let targetCode = joinCode else { return }
        guard info?["code"]?.uppercased() == targetCode else { return }
        discoveredHostByCode[targetCode] = peerID
        browser.invitePeer(peerID, to: session!, withContext: nil, timeout: 10)
        delegate?.lobbyServiceDidChangeConnectionState("Joining \(peerID.displayName)...")
    }

    func browser(_ browser: MCNearbyServiceBrowser, lostPeer peerID: MCPeerID) {
        let peers = (session?.connectedPeers ?? []).map {
            LobbyPeer(id: $0.displayName, displayName: $0.displayName)
        }
        delegate?.lobbyServiceDidUpdatePeers(peers)
    }
}

extension LocalLobbyService: MCSessionDelegate {
    func session(_ session: MCSession, peer peerID: MCPeerID, didChange state: MCSessionState) {
        let peers = session.connectedPeers.map { LobbyPeer(id: $0.displayName, displayName: $0.displayName) }
        DispatchQueue.main.async {
            self.delegate?.lobbyServiceDidUpdatePeers(peers)
            switch state {
            case .connected:
                self.delegate?.lobbyServiceDidChangeConnectionState("Connected to \(peerID.displayName)")
            case .connecting:
                self.delegate?.lobbyServiceDidChangeConnectionState("Connecting to \(peerID.displayName)...")
            case .notConnected:
                self.delegate?.lobbyServiceDidChangeConnectionState("\(peerID.displayName) disconnected")
            @unknown default:
                self.delegate?.lobbyServiceDidChangeConnectionState("Unknown connection state")
            }
        }
    }

    func session(_ session: MCSession, didReceive data: Data, fromPeer peerID: MCPeerID) {
        guard let message = try? JSONDecoder().decode(NetworkMessage.self, from: data) else { return }
        DispatchQueue.main.async {
            self.delegate?.lobbyServiceDidReceive(message: message, from: peerID.displayName)
        }
    }

    func session(_ session: MCSession, didReceive stream: InputStream, withName streamName: String, fromPeer peerID: MCPeerID) {}
    func session(_ session: MCSession, didStartReceivingResourceWithName resourceName: String, fromPeer peerID: MCPeerID, with progress: Progress) {}
    func session(_ session: MCSession, didFinishReceivingResourceWithName resourceName: String, fromPeer peerID: MCPeerID, at localURL: URL?, withError error: (any Error)?) {}
}
