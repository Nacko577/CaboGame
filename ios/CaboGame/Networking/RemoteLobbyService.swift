import Foundation

/// LobbyService implementation that talks to the Node.js relay server over
/// WebSockets. All app-level game logic remains host-authoritative; this
/// transport just routes JSON messages between the host and guests.
final class RemoteLobbyService: NSObject, LobbyService {
    weak var delegate: LobbyServiceDelegate?
    private(set) var joinCode: String?

    private let serverURL: URL
    private var role: LobbyRole?
    private var localDisplayName: String = ""
    private var session: URLSession?
    private var task: URLSessionWebSocketTask?
    private var didOpen = false
    private var pendingHost = false
    private var pendingJoinCode: String?
    private var peerNames: [String: String] = [:]

    init(serverURL: URL = ServerConfig.current) {
        self.serverURL = serverURL
        super.init()
    }

    func startHosting(displayName: String) throws {
        cleanup()
        role = .host
        localDisplayName = displayName
        pendingHost = true
        pendingJoinCode = nil
        notify("Connecting to server...")
        connect()
    }

    func startJoining(displayName: String, code: String) throws {
        cleanup()
        role = .guest
        localDisplayName = displayName
        pendingHost = false
        pendingJoinCode = code.uppercased()
        notify("Connecting to server...")
        connect()
    }

    func stop() {
        sendControl(["type": "leave"])
        cleanup()
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

    private func connect() {
        let config = URLSessionConfiguration.default
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
        let nsError = error as NSError
        // POSIX 53 (connection reset) / 57 (not connected) just mean we closed; ignore noise.
        if nsError.domain == NSPOSIXErrorDomain, nsError.code == 53 || nsError.code == 57 {
            return
        }
        notify("Disconnected: \(error.localizedDescription)")
    }

    private func handleIncomingText(_ text: String) {
        guard let data = text.data(using: .utf8),
              let raw = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let type = raw["type"] as? String else { return }

        switch type {
        case "hosted":
            let code = (raw["code"] as? String) ?? ""
            joinCode = code
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
        case "hostLeft":
            cleanup()
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

    private func cleanup() {
        // invalidateAndCancel() cancels every outstanding task on this session
        // and releases the delegate. Calling task.cancel(with:reason:) on a
        // task that hasn't fully connected yet can trip Foundation's
        // "invalid reuse after initialization" guard, so we let the session
        // handle teardown.
        session?.invalidateAndCancel()
        session = nil
        task = nil
        didOpen = false
        pendingHost = false
        pendingJoinCode = nil
        joinCode = nil
        role = nil
        peerNames = [:]
    }
}

extension RemoteLobbyService: URLSessionWebSocketDelegate {
    func urlSession(_ session: URLSession,
                    webSocketTask: URLSessionWebSocketTask,
                    didOpenWithProtocol protocol: String?) {
        didOpen = true
        if pendingHost {
            sendControl(["type": "host", "name": localDisplayName])
        } else if let code = pendingJoinCode {
            sendControl(["type": "join", "name": localDisplayName, "code": code])
        }
        pendingHost = false
        pendingJoinCode = nil
    }

    func urlSession(_ session: URLSession,
                    webSocketTask: URLSessionWebSocketTask,
                    didCloseWith closeCode: URLSessionWebSocketTask.CloseCode,
                    reason: Data?) {
        notify("Disconnected")
    }
}
