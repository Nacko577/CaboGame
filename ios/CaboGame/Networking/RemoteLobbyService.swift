import Foundation

/// LobbyService implementation that talks to the Node.js relay server over
/// WebSockets. All app-level game logic remains host-authoritative; this
/// transport just routes JSON messages between the host and guests.
///
/// Proxies and cellular networks often tear down idle sockets ("Software caused
/// connection abort"). We send periodic JSON `ping` messages (server replies with
/// `pong`), and reconnect automatically. Hosts reclaim the same lobby using
/// `resumeCode` after the relay briefly keeps the room alive without a host socket.
final class RemoteLobbyService: NSObject, LobbyService {
    weak var delegate: LobbyServiceDelegate?
    private(set) var joinCode: String?

    private let serverURL: URL
    private var role: LobbyRole?
    private var localDisplayName: String = ""
    /// Join code the guest entered (kept across reconnects).
    private var guestLobbyCode: String?
    /// Authoritative code for this hosting session (survives transport teardown).
    private var hostLobbyCode: String?
    private var session: URLSession?
    private var task: URLSessionWebSocketTask?
    private var didOpen = false
    private var peerNames: [String: String] = [:]
    private var intentionalDisconnect = false
    private var reconnectWorkItem: DispatchWorkItem?
    private var keepAliveTimer: Timer?
    private var reconnectDelay: TimeInterval = 0.5

    init(serverURL: URL = ServerConfig.current) {
        self.serverURL = serverURL
        super.init()
    }

    func startHosting(displayName: String) throws {
        intentionalDisconnect = false
        cancelReconnectAndKeepAlive()
        teardownTransport(preserveLobbyState: false)
        role = .host
        localDisplayName = displayName
        guestLobbyCode = nil
        notify("Connecting to server...")
        openSocket()
    }

    func startJoining(displayName: String, code: String) throws {
        intentionalDisconnect = false
        cancelReconnectAndKeepAlive()
        teardownTransport(preserveLobbyState: false)
        role = .guest
        localDisplayName = displayName
        guestLobbyCode = code.uppercased()
        notify("Connecting to server...")
        openSocket()
    }

    func stop() {
        intentionalDisconnect = true
        cancelReconnectAndKeepAlive()
        sendControl(["type": "leave"])
        teardownTransport(preserveLobbyState: false)
        DispatchQueue.main.async { [weak self] in
            self?.delegate?.lobbyServiceDidUpdateJoinCode(nil)
        }
        notify("Disconnected")
    }

    func send(_ message: NetworkMessage) throws {
        guard let task else { return }
        let wire = WireNetworkMessage(from: message)
        let payload = try JSONEncoder().encode(wire)
        guard let payloadObj = try JSONSerialization.jsonObject(with: payload) as? [String: Any] else { return }
        let envelope: [String: Any] = ["type": "relay", "payload": payloadObj]
        let data = try JSONSerialization.data(withJSONObject: envelope)
        guard let line = String(data: data, encoding: .utf8) else { return }
        task.send(.string(line)) { [weak self] error in
            if let error {
                self?.notify("Send failed: \(error.localizedDescription)")
            }
        }
    }

    private func openSocket() {
        guard !intentionalDisconnect else { return }
        let config = URLSessionConfiguration.default
        config.waitsForConnectivity = true
        let session = URLSession(configuration: config, delegate: self, delegateQueue: nil)
        let task = session.webSocketTask(with: serverURL)
        self.session = session
        self.task = task
        task.resume()
        receiveLoop()
    }

    private func receiveLoop() {
        guard let task else { return }
        task.receive { [weak self] result in
            guard let self else { return }
            switch result {
            case .failure(let error):
                self.handleSocketFailure(error)
            case .success(.string(let text)):
                self.handleIncomingText(text)
                self.receiveLoop()
            case .success(.data(let data)):
                if let text = String(data: data, encoding: .utf8) {
                    self.handleIncomingText(text)
                }
                self.receiveLoop()
            @unknown default:
                self.receiveLoop()
            }
        }
    }

    private func handleSocketFailure(_ error: Error) {
        if intentionalDisconnect { return }
        let nsError = error as NSError
        // Still reconnect — these often follow an aborted relay/proxy connection.
        if nsError.domain == NSPOSIXErrorDomain, nsError.code == 53 || nsError.code == 57 {
            notify("Reconnecting…")
            teardownTransport(preserveLobbyState: true)
            scheduleReconnect()
            return
        }
        notify("Reconnecting…")
        teardownTransport(preserveLobbyState: true)
        scheduleReconnect()
    }

    private func scheduleReconnect() {
        guard !intentionalDisconnect, role != nil else { return }
        reconnectWorkItem?.cancel()
        let delay = reconnectDelay
        reconnectDelay = min(reconnectDelay * 2, 30)
        let item = DispatchWorkItem { [weak self] in
            guard let self, !self.intentionalDisconnect else { return }
            self.openSocket()
        }
        reconnectWorkItem = item
        DispatchQueue.main.asyncAfter(deadline: .now() + delay, execute: item)
    }

    private func cancelReconnectAndKeepAlive() {
        reconnectWorkItem?.cancel()
        reconnectWorkItem = nil
        keepAliveTimer?.invalidate()
        keepAliveTimer = nil
    }

    private func startKeepAlive() {
        keepAliveTimer?.invalidate()
        keepAliveTimer = Timer.scheduledTimer(withTimeInterval: 25, repeats: true) { [weak self] _ in
            guard let self, !self.intentionalDisconnect else { return }
            self.sendControl(["type": "ping"])
        }
        keepAliveTimer?.tolerance = 2
        if let timer = keepAliveTimer {
            RunLoop.main.add(timer, forMode: .common)
        }
    }

    private func sendHandshake() {
        switch role {
        case .host:
            let resume = hostLobbyCode ?? joinCode
            if let resume {
                sendControl(["type": "host", "name": localDisplayName, "resumeCode": resume])
            } else {
                sendControl(["type": "host", "name": localDisplayName])
            }
        case .guest:
            guard let code = guestLobbyCode else { return }
            sendControl(["type": "join", "name": localDisplayName, "code": code])
        case .none:
            break
        }
    }

    private func handleIncomingText(_ text: String) {
        guard let data = text.data(using: .utf8),
              let raw = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let type = raw["type"] as? String else { return }

        switch type {
        case "hosted":
            let code = (raw["code"] as? String) ?? ""
            let previous = joinCode
            hostLobbyCode = code
            joinCode = code
            if let previous, previous != code {
                notify("Game code changed to \(code) — deploy the latest relay to keep the same room after drops.")
            }
            DispatchQueue.main.async { [weak self] in
                self?.delegate?.lobbyServiceDidUpdateJoinCode(code)
            }
            notify("Hosting with code \(code)")
        case "joined":
            joinCode = (raw["code"] as? String) ?? joinCode
            DispatchQueue.main.async { [weak self] in
                guard let self else { return }
                self.delegate?.lobbyServiceDidUpdatePeers([LobbyPeer(id: "host", displayName: "host")])
            }
            notify("Connected to host")
        case "peerJoined":
            let id = (raw["peerId"] as? String) ?? ""
            let name = (raw["name"] as? String) ?? id
            guard !id.isEmpty else { return }
            peerNames[id] = name
            broadcastPeerListIfHost()
            notify("\(name) joined")
        case "peerLeft":
            let id = (raw["peerId"] as? String) ?? ""
            let name = peerNames[id] ?? (raw["name"] as? String) ?? id
            peerNames.removeValue(forKey: id)
            broadcastPeerListIfHost()
            notify("\(name) disconnected")
        case "hostReconnecting":
            notify("Host reconnecting…")
        case "hostLeft":
            intentionalDisconnect = true
            cancelReconnectAndKeepAlive()
            teardownTransport(preserveLobbyState: false)
            DispatchQueue.main.async { [weak self] in
                self?.delegate?.lobbyServiceDidUpdateJoinCode(nil)
            }
            notify("Host left the lobby")
        case "relay":
            let from = (raw["from"] as? String) ?? "peer"
            guard let payload = raw["payload"],
                  let payloadData = try? JSONSerialization.data(withJSONObject: payload),
                  let wire = try? JSONDecoder().decode(WireNetworkMessage.self, from: payloadData),
                  let app = wire.toAppMessage() else { return }
            DispatchQueue.main.async { [weak self] in
                self?.delegate?.lobbyServiceDidReceive(message: app, from: from)
            }
        case "error":
            let message = (raw["message"] as? String) ?? "Unknown error"
            notify("Server: \(message)")
        case "pong":
            break
        default:
            break
        }
    }

    private func broadcastPeerListIfHost() {
        guard role == .host else { return }
        let peers = peerNames.map { LobbyPeer(id: $0.value, displayName: $0.value) }
        DispatchQueue.main.async { [weak self] in
            self?.delegate?.lobbyServiceDidUpdatePeers(peers)
        }
    }

    private func sendControl(_ obj: [String: Any]) {
        guard let task else { return }
        guard let data = try? JSONSerialization.data(withJSONObject: obj),
              let text = String(data: data, encoding: .utf8) else { return }
        task.send(.string(text)) { _ in }
    }

    private func notify(_ text: String) {
        DispatchQueue.main.async { [weak self] in
            self?.delegate?.lobbyServiceDidChangeConnectionState(text)
        }
    }

    /// Tear down the URLSession / socket. Optionally keep `joinCode`, `role`,
    /// `guestLobbyCode`, and `peerNames` so we can reconnect.
    private func teardownTransport(preserveLobbyState: Bool) {
        session?.invalidateAndCancel()
        session = nil
        task = nil
        didOpen = false
        if !preserveLobbyState {
            joinCode = nil
            hostLobbyCode = nil
            role = nil
            guestLobbyCode = nil
            peerNames = [:]
        }
    }
}

extension RemoteLobbyService: URLSessionWebSocketDelegate {
    func urlSession(_ session: URLSession,
                    webSocketTask: URLSessionWebSocketTask,
                    didOpenWithProtocol protocol: String?) {
        guard webSocketTask === task else { return }
        didOpen = true
        reconnectWorkItem?.cancel()
        reconnectWorkItem = nil
        reconnectDelay = 0.5
        sendHandshake()
        startKeepAlive()
    }

    func urlSession(_ session: URLSession,
                    webSocketTask: URLSessionWebSocketTask,
                    didCloseWith closeCode: URLSessionWebSocketTask.CloseCode,
                    reason: Data?) {
        guard webSocketTask === task else { return }
        if intentionalDisconnect {
            notify("Disconnected")
            return
        }
        cancelReconnectAndKeepAlive()
        teardownTransport(preserveLobbyState: true)
        notify("Reconnecting…")
        scheduleReconnect()
    }
}
