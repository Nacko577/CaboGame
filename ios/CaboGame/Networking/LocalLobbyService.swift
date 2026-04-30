import Foundation
import Network
import Darwin

final class LocalLobbyService: NSObject, LobbyService {
    weak var delegate: LobbyServiceDelegate?
    private(set) var joinCode: String?

    private var role: LobbyRole?
    private var localDisplayName: String = ""

    private let serviceType = "_cabo-local._tcp"
    private let serviceTypeWithDot = "_cabo-local._tcp."
    private var listener: NWListener?
    private var browsers: [NWBrowser] = []
    private var netServiceBrowser: NetServiceBrowser?
    private var resolvingServices: [String: NetService] = [:]

    // UDP broadcast discovery fallback (more reliable cross-platform than mDNS).
    private let udpDiscoveryPort: UInt16 = 49888
    private var udpDiscoverySocket: Int32 = -1
    private var udpDiscoveryQueue: DispatchQueue?
    private var udpDiscoveryTimer: DispatchSourceTimer?
    private var udpDiscoveryActive: Bool = false
    private var udpProbeCount: Int = 0

    private final class PeerConnection {
        let connection: NWConnection
        var peerName: String?
        var buffer = Data()

        init(connection: NWConnection) {
            self.connection = connection
        }
    }

    private var hostConnections: [ObjectIdentifier: PeerConnection] = [:]
    private var guestConnection: PeerConnection?

    func startHosting(displayName: String) throws {
        cleanup()
        role = .host
        localDisplayName = displayName

        let code = Self.makeCode()
        joinCode = code
        DispatchQueue.main.async { [weak self] in
            self?.delegate?.lobbyServiceDidUpdateJoinCode(code)
        }

        let listener = try NWListener(using: .tcp, on: .any)
        listener.service = NWListener.Service(name: "cabo-\(code)", type: serviceType)

        listener.newConnectionHandler = { [weak self] connection in
            self?.handleIncoming(connection)
        }
        listener.stateUpdateHandler = { [weak self] state in
            guard let self else { return }
            DispatchQueue.main.async {
                switch state {
                case .ready:
                    self.delegate?.lobbyServiceDidChangeConnectionState("Hosting with code \(code)")
                case .failed(let error):
                    self.delegate?.lobbyServiceDidChangeConnectionState("Host failed: \(error.localizedDescription)")
                default:
                    break
                }
            }
        }
        self.listener = listener
        listener.start(queue: .global(qos: .userInitiated))
    }

    func startJoining(displayName: String, code: String) throws {
        cleanup()
        role = .guest
        localDisplayName = displayName
        joinCode = code.uppercased()

        let params = NWParameters.tcp
        // Keep descriptors strictly valid for NWBrowser:
        // - type must be Bonjour service type form (leading underscore + protocol suffix)
        // - domain can be "local." or nil.
        let descriptors: [NWBrowser.Descriptor] = [
            .bonjour(type: serviceType, domain: "local."),
            .bonjour(type: serviceTypeWithDot, domain: "local."),
            .bonjour(type: serviceType, domain: nil)
        ]

        let createdBrowsers = descriptors.map { descriptor in
            let browser = NWBrowser(for: descriptor, using: params)
            browser.stateUpdateHandler = { [weak self] state in
                guard let self else { return }
                DispatchQueue.main.async {
                    switch state {
                    case .ready:
                        self.delegate?.lobbyServiceDidChangeConnectionState("Searching for host...")
                    case .failed(let error):
                        self.delegate?.lobbyServiceDidChangeConnectionState("Browse failed: \(error.localizedDescription)")
                    default:
                        break
                    }
                }
            }
            browser.browseResultsChangedHandler = { [weak self] results, _ in
                self?.attemptJoin(from: results)
            }
            return browser
        }

        self.browsers = createdBrowsers
        for browser in createdBrowsers {
            browser.start(queue: .global(qos: .userInitiated))
        }
        startLegacyServiceBrowse()
        startUdpDiscoveryProbe(code: code.uppercased())
    }

    func stop() {
        cleanup()
        DispatchQueue.main.async {
            self.delegate?.lobbyServiceDidUpdateJoinCode(nil)
            self.delegate?.lobbyServiceDidChangeConnectionState("Disconnected")
        }
    }

    func send(_ message: NetworkMessage) throws {
        let wire = WireNetworkMessage(from: message)
        let data = try JSONEncoder().encode(wire)
        guard var line = String(data: data, encoding: .utf8) else { return }
        line.append("\n")
        guard let lineData = line.data(using: .utf8) else { return }

        switch role {
        case .host:
            for peer in hostConnections.values {
                sendRaw(lineData, via: peer.connection)
            }
        case .guest:
            if let guestConnection {
                sendRaw(lineData, via: guestConnection.connection)
            }
        case .none:
            break
        }
    }

    private func attemptJoin(from results: Set<NWBrowser.Result>) {
        guard role == .guest, guestConnection == nil, let targetCode = joinCode else { return }

        let targetName = "cabo-\(targetCode)".lowercased()
        let targetCodeLower = targetCode.lowercased()
        guard let match = results.first(where: { result in
            if case let .service(name, _, _, _) = result.endpoint {
                let lowered = name.lowercased()
                // Android/NSD may mutate advertised names in ways that don't always preserve
                // a strict "cabo-<code>" prefix format, so match by exact prefix first,
                // then code containment, then any cabo-* service as a fallback.
                return lowered.hasPrefix(targetName)
                    || lowered.contains(targetCodeLower)
                    || lowered.hasPrefix("cabo-")
            }
            return false
        }) else { return }

        for browser in browsers {
            browser.cancel()
        }
        browsers = []
        stopLegacyServiceBrowse()

        let conn = NWConnection(to: match.endpoint, using: .tcp)
        let peer = PeerConnection(connection: conn)
        guestConnection = peer

        conn.stateUpdateHandler = { [weak self] state in
            guard let self else { return }
            switch state {
            case .ready:
                DispatchQueue.main.async {
                    self.delegate?.lobbyServiceDidChangeConnectionState("Connected to host")
                    self.delegate?.lobbyServiceDidUpdatePeers([LobbyPeer(id: "host", displayName: "host")])
                }
                self.sendHandshake(for: peer)
                self.receiveLoop(for: peer, fromID: "host")
            case .failed(let error):
                DispatchQueue.main.async {
                    self.delegate?.lobbyServiceDidChangeConnectionState("Disconnected: \(error.localizedDescription)")
                }
            default:
                break
            }
        }

        conn.start(queue: .global(qos: .userInitiated))
    }

    private func handleIncoming(_ connection: NWConnection) {
        let peer = PeerConnection(connection: connection)
        hostConnections[ObjectIdentifier(connection)] = peer

        connection.stateUpdateHandler = { [weak self] state in
            guard let self else { return }
            switch state {
            case .ready:
                self.receiveLoop(for: peer, fromID: "unknown")
            case .failed, .cancelled:
                self.removeHostConnection(connection)
            default:
                break
            }
        }
        connection.start(queue: .global(qos: .userInitiated))
    }

    private func receiveLoop(for peer: PeerConnection, fromID fallbackID: String) {
        peer.connection.receive(minimumIncompleteLength: 1, maximumLength: 4096) { [weak self] data, _, isComplete, error in
            guard let self else { return }
            if let data, !data.isEmpty {
                peer.buffer.append(data)
                self.processBufferedLines(for: peer, fallbackID: fallbackID)
            }

            if isComplete || error != nil {
                if self.role == .host {
                    self.removeHostConnection(peer.connection)
                } else {
                    self.guestConnection = nil
                    DispatchQueue.main.async {
                        self.delegate?.lobbyServiceDidChangeConnectionState("Disconnected")
                    }
                }
                return
            }

            self.receiveLoop(for: peer, fromID: fallbackID)
        }
    }

    private func processBufferedLines(for peer: PeerConnection, fallbackID: String) {
        while let newlineIndex = peer.buffer.firstIndex(of: 0x0A) { // '\n'
            let lineData = peer.buffer.prefix(upTo: newlineIndex)
            peer.buffer.removeSubrange(...newlineIndex)
            guard !lineData.isEmpty else { continue }
            guard let line = String(data: lineData, encoding: .utf8) else { continue }

            // Host side expects the first line from each guest to be handshake displayName.
            if role == .host && peer.peerName == nil {
                peer.peerName = line.trimmingCharacters(in: .whitespacesAndNewlines)
                notifyPeerListChanged()
                DispatchQueue.main.async {
                    self.delegate?.lobbyServiceDidChangeConnectionState("\(peer.peerName ?? "Guest") joined")
                }
                continue
            }

            guard let lineBytes = line.data(using: .utf8),
                  let wire = try? JSONDecoder().decode(WireNetworkMessage.self, from: lineBytes),
                  let app = wire.toAppMessage()
            else { continue }

            let fromID = peer.peerName ?? fallbackID
            DispatchQueue.main.async {
                self.delegate?.lobbyServiceDidReceive(message: app, from: fromID)
            }
        }
    }

    private func sendHandshake(for peer: PeerConnection) {
        var line = localDisplayName
        line.append("\n")
        guard let data = line.data(using: .utf8) else { return }
        sendRaw(data, via: peer.connection)
    }

    private func sendRaw(_ data: Data, via connection: NWConnection) {
        connection.send(content: data, completion: .contentProcessed { _ in })
    }

    private func removeHostConnection(_ connection: NWConnection) {
        let key = ObjectIdentifier(connection)
        let name = hostConnections[key]?.peerName
        hostConnections[key] = nil
        notifyPeerListChanged()
        if let name {
            DispatchQueue.main.async {
                self.delegate?.lobbyServiceDidChangeConnectionState("\(name) disconnected")
            }
        }
    }

    private func notifyPeerListChanged() {
        let peers = hostConnections.values.compactMap { peer -> LobbyPeer? in
            guard let name = peer.peerName, !name.isEmpty else { return nil }
            return LobbyPeer(id: name, displayName: name)
        }
        DispatchQueue.main.async {
            self.delegate?.lobbyServiceDidUpdatePeers(peers)
        }
    }

    private func cleanup() {
        listener?.cancel()
        listener = nil
        for browser in browsers {
            browser.cancel()
        }
        browsers = []
        stopLegacyServiceBrowse()
        stopUdpDiscoveryProbe()

        for peer in hostConnections.values {
            peer.connection.cancel()
        }
        hostConnections = [:]

        guestConnection?.connection.cancel()
        guestConnection = nil
    }

    private static func makeCode() -> String {
        let letters = Array("ABCDEFGHJKLMNPQRSTUVWXYZ23456789")
        return String((0..<5).map { _ in letters.randomElement()! })
    }

    private func startLegacyServiceBrowse() {
        let browser = NetServiceBrowser()
        browser.delegate = self
        netServiceBrowser = browser
        browser.searchForServices(ofType: serviceTypeWithDot, inDomain: "local.")
    }

    private func stopLegacyServiceBrowse() {
        netServiceBrowser?.stop()
        netServiceBrowser = nil
        for service in resolvingServices.values {
            service.stop()
        }
        resolvingServices = [:]
    }

    private func isPotentialJoinTarget(serviceName: String, code: String) -> Bool {
        let lowered = serviceName.lowercased()
        let target = "cabo-\(code.lowercased())"
        return lowered.hasPrefix(target) || lowered.contains(code.lowercased()) || lowered.hasPrefix("cabo-")
    }

    // MARK: - UDP Broadcast Discovery (cross-platform fallback)

    private func startUdpDiscoveryProbe(code: String) {
        stopUdpDiscoveryProbe()
        udpProbeCount = 0

        let sock = socket(AF_INET, SOCK_DGRAM, 0)
        guard sock >= 0 else { return }

        var enable: Int32 = 1
        _ = setsockopt(sock, SOL_SOCKET, SO_BROADCAST, &enable, socklen_t(MemoryLayout<Int32>.size))
        _ = setsockopt(sock, SOL_SOCKET, SO_REUSEADDR, &enable, socklen_t(MemoryLayout<Int32>.size))

        var bindAddr = sockaddr_in()
        bindAddr.sin_family = sa_family_t(AF_INET)
        bindAddr.sin_addr.s_addr = INADDR_ANY.bigEndian
        bindAddr.sin_port = 0
        bindAddr.sin_len = UInt8(MemoryLayout<sockaddr_in>.size)
        _ = withUnsafePointer(to: &bindAddr) { ptr in
            ptr.withMemoryRebound(to: sockaddr.self, capacity: 1) { sa in
                bind(sock, sa, socklen_t(MemoryLayout<sockaddr_in>.size))
            }
        }

        udpDiscoverySocket = sock
        udpDiscoveryActive = true

        // Separate queues so the blocking recvfrom() loop never starves the probe timer.
        let receiveQueue = DispatchQueue(label: "com.cabogame.udpRecv", qos: .userInitiated)
        let timerQueue = DispatchQueue(label: "com.cabogame.udpProbe", qos: .userInitiated)
        udpDiscoveryQueue = timerQueue

        receiveQueue.async { [weak self] in
            self?.udpReceiveLoop(socket: sock)
        }

        let timer = DispatchSource.makeTimerSource(queue: timerQueue)
        timer.schedule(deadline: .now(), repeating: 1.0)
        timer.setEventHandler { [weak self] in
            self?.sendUdpDiscoveryProbe(socket: sock, code: code)
        }
        timer.resume()
        udpDiscoveryTimer = timer
    }

    private func sendUdpDiscoveryProbe(socket sock: Int32, code: String) {
        guard udpDiscoveryActive else { return }
        let message = "CABO_DISCOVER:\(code)"

        // Try the global broadcast plus any subnet-directed broadcasts we can derive
        // from the active network interfaces. iOS often blocks 255.255.255.255 but
        // delivers subnet-directed broadcasts (e.g. 192.168.1.255) reliably.
        // Also send to multicast 239.255.99.99 which can pass routers/AP-isolated networks
        // that drop broadcast packets.
        var targets: Set<String> = ["255.255.255.255", "239.255.99.99"]
        targets.formUnion(localSubnetBroadcastAddresses())

        // Some routers drop broadcast and multicast frames between Wi-Fi clients
        // (AP isolation). Unicast still works in those cases, so additionally
        // probe each address in our local /24 subnet directly.
        let unicastTargets = localSubnetUnicastAddresses()

        var ttl: UInt8 = 1
        _ = setsockopt(sock, IPPROTO_IP, IP_MULTICAST_TTL, &ttl, socklen_t(MemoryLayout<UInt8>.size))

        for target in targets {
            sendProbe(message: message, to: target, port: udpDiscoveryPort, on: sock)
        }
        for target in unicastTargets {
            sendProbe(message: message, to: target, port: udpDiscoveryPort, on: sock)
        }
        udpProbeCount += 1
    }

    private func sendProbe(message: String, to host: String, port: UInt16, on sock: Int32) {
        var addr = sockaddr_in()
        addr.sin_family = sa_family_t(AF_INET)
        addr.sin_port = port.bigEndian
        addr.sin_addr.s_addr = inet_addr(host)
        addr.sin_len = UInt8(MemoryLayout<sockaddr_in>.size)

        message.withCString { cstr in
            let len = strlen(cstr)
            _ = withUnsafePointer(to: &addr) { ptr in
                ptr.withMemoryRebound(to: sockaddr.self, capacity: 1) { sa in
                    sendto(sock, cstr, len, 0, sa, socklen_t(MemoryLayout<sockaddr_in>.size))
                }
            }
        }
    }

    /// Returns up to 254 unicast addresses representing every host in the /24 subnet
    /// of each active Wi-Fi/Ethernet interface (e.g. 192.168.1.1 through 192.168.1.254).
    private func localSubnetUnicastAddresses() -> [String] {
        var ifaddrPtr: UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&ifaddrPtr) == 0, let first = ifaddrPtr else { return [] }
        defer { freeifaddrs(ifaddrPtr) }

        var hosts = Set<String>()
        var ptr: UnsafeMutablePointer<ifaddrs>? = first
        while let current = ptr {
            let iface = current.pointee
            ptr = iface.ifa_next

            guard let addr = iface.ifa_addr,
                  addr.pointee.sa_family == UInt8(AF_INET)
            else { continue }

            let name = String(cString: iface.ifa_name)
            guard name.hasPrefix("en") || name.hasPrefix("bridge") || name.hasPrefix("eth") else { continue }

            var hostBuffer = [CChar](repeating: 0, count: Int(NI_MAXHOST))
            guard getnameinfo(addr, socklen_t(addr.pointee.sa_len),
                              &hostBuffer, socklen_t(hostBuffer.count),
                              nil, 0, NI_NUMERICHOST) == 0
            else { continue }

            let host = String(cString: hostBuffer)
            let parts = host.split(separator: ".").compactMap { UInt32($0) }
            guard parts.count == 4 else { continue }

            let prefix = "\(parts[0]).\(parts[1]).\(parts[2])."
            for octet in 1..<255 where octet != Int(parts[3]) {
                hosts.insert("\(prefix)\(octet)")
            }
        }
        return Array(hosts)
    }

    private func localSubnetBroadcastAddresses() -> [String] {
        var results = [String]()
        var ifaddrPtr: UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&ifaddrPtr) == 0, let first = ifaddrPtr else {
            return results
        }
        defer { freeifaddrs(ifaddrPtr) }

        var ptr: UnsafeMutablePointer<ifaddrs>? = first
        while let current = ptr {
            let iface = current.pointee
            ptr = iface.ifa_next

            guard let addr = iface.ifa_addr,
                  addr.pointee.sa_family == UInt8(AF_INET),
                  let mask = iface.ifa_netmask
            else { continue }

            let name = String(cString: iface.ifa_name)
            // Restrict to typical Wi-Fi/Ethernet interfaces; skip loopback and tunnels.
            guard name.hasPrefix("en") || name.hasPrefix("bridge") || name.hasPrefix("eth") else { continue }

            var hostBuffer = [CChar](repeating: 0, count: Int(NI_MAXHOST))
            var maskBuffer = [CChar](repeating: 0, count: Int(NI_MAXHOST))
            if getnameinfo(addr, socklen_t(addr.pointee.sa_len),
                           &hostBuffer, socklen_t(hostBuffer.count),
                           nil, 0, NI_NUMERICHOST) == 0,
               getnameinfo(mask, socklen_t(mask.pointee.sa_len),
                           &maskBuffer, socklen_t(maskBuffer.count),
                           nil, 0, NI_NUMERICHOST) == 0 {
                let host = String(cString: hostBuffer)
                let netmask = String(cString: maskBuffer)
                if let bcast = subnetBroadcast(host: host, mask: netmask) {
                    results.append(bcast)
                }
            }
        }
        return results
    }

    private func subnetBroadcast(host: String, mask: String) -> String? {
        let hostParts = host.split(separator: ".").compactMap { UInt32($0) }
        let maskParts = mask.split(separator: ".").compactMap { UInt32($0) }
        guard hostParts.count == 4, maskParts.count == 4 else { return nil }
        var parts = [UInt32]()
        for i in 0..<4 {
            let inverted = (~maskParts[i]) & 0xff
            parts.append((hostParts[i] & maskParts[i]) | inverted)
        }
        return parts.map { String($0) }.joined(separator: ".")
    }

    private func udpReceiveLoop(socket sock: Int32) {
        var buffer = [UInt8](repeating: 0, count: 1024)
        var senderAddr = sockaddr_in()
        var senderLen = socklen_t(MemoryLayout<sockaddr_in>.size)

        while udpDiscoveryActive && udpDiscoverySocket == sock {
            let bytesRead = withUnsafeMutablePointer(to: &senderAddr) { ptr -> ssize_t in
                ptr.withMemoryRebound(to: sockaddr.self, capacity: 1) { sa in
                    recvfrom(sock, &buffer, buffer.count, 0, sa, &senderLen)
                }
            }
            guard bytesRead > 0 else { break }

            guard let received = String(bytes: buffer[0..<Int(bytesRead)], encoding: .utf8)?
                .trimmingCharacters(in: .whitespacesAndNewlines)
            else { continue }

            let prefix = "CABO_HOST:"
            guard received.hasPrefix(prefix),
                  let portValue = UInt16(received.dropFirst(prefix.count))
            else { continue }

            var addrCopy = senderAddr
            let host = withUnsafePointer(to: &addrCopy.sin_addr) { ptr -> String in
                String(cString: inet_ntoa(ptr.pointee))
            }

            DispatchQueue.main.async { [weak self] in
                self?.handleUdpDiscoveryReply(host: host, port: portValue)
            }
            break
        }
    }

    private func handleUdpDiscoveryReply(host: String, port: UInt16) {
        guard role == .guest, guestConnection == nil else { return }
        guard let nwPort = NWEndpoint.Port(rawValue: port) else { return }

        for browser in browsers {
            browser.cancel()
        }
        browsers = []
        stopLegacyServiceBrowse()
        stopUdpDiscoveryProbe()

        let conn = NWConnection(host: NWEndpoint.Host(host), port: nwPort, using: .tcp)
        let peer = PeerConnection(connection: conn)
        guestConnection = peer

        conn.stateUpdateHandler = { [weak self] state in
            guard let self else { return }
            switch state {
            case .ready:
                DispatchQueue.main.async {
                    self.delegate?.lobbyServiceDidChangeConnectionState("Connected to host")
                    self.delegate?.lobbyServiceDidUpdatePeers([LobbyPeer(id: "host", displayName: "host")])
                }
                self.sendHandshake(for: peer)
                self.receiveLoop(for: peer, fromID: "host")
            case .failed(let error):
                DispatchQueue.main.async {
                    self.delegate?.lobbyServiceDidChangeConnectionState("Disconnected: \(error.localizedDescription)")
                }
            default:
                break
            }
        }

        conn.start(queue: .global(qos: .userInitiated))
    }

    private func stopUdpDiscoveryProbe() {
        udpDiscoveryActive = false
        udpDiscoveryTimer?.cancel()
        udpDiscoveryTimer = nil
        if udpDiscoverySocket >= 0 {
            close(udpDiscoverySocket)
            udpDiscoverySocket = -1
        }
        udpDiscoveryQueue = nil
    }
}

extension LocalLobbyService: NetServiceBrowserDelegate, NetServiceDelegate {
    func netServiceBrowser(_ browser: NetServiceBrowser, didFind service: NetService, moreComing: Bool) {
        guard role == .guest, guestConnection == nil, let code = joinCode else { return }
        guard isPotentialJoinTarget(serviceName: service.name, code: code) else { return }

        service.delegate = self
        resolvingServices[service.name] = service
        service.resolve(withTimeout: 5)
    }

    func netServiceDidResolveAddress(_ sender: NetService) {
        guard role == .guest, guestConnection == nil else { return }
        guard let hostName = sender.hostName,
              !hostName.isEmpty,
              let port = NWEndpoint.Port(rawValue: UInt16(sender.port))
        else { return }

        for browser in browsers {
            browser.cancel()
        }
        browsers = []
        stopLegacyServiceBrowse()

        let conn = NWConnection(host: .name(hostName, nil), port: port, using: .tcp)
        let peer = PeerConnection(connection: conn)
        guestConnection = peer

        conn.stateUpdateHandler = { [weak self] state in
            guard let self else { return }
            switch state {
            case .ready:
                DispatchQueue.main.async {
                    self.delegate?.lobbyServiceDidChangeConnectionState("Connected to host")
                    self.delegate?.lobbyServiceDidUpdatePeers([LobbyPeer(id: "host", displayName: "host")])
                }
                self.sendHandshake(for: peer)
                self.receiveLoop(for: peer, fromID: "host")
            case .failed(let error):
                DispatchQueue.main.async {
                    self.delegate?.lobbyServiceDidChangeConnectionState("Disconnected: \(error.localizedDescription)")
                }
            default:
                break
            }
        }

        conn.start(queue: .global(qos: .userInitiated))
    }

    func netService(_ sender: NetService, didNotResolve errorDict: [String : NSNumber]) {
        resolvingServices[sender.name] = nil
    }
}
