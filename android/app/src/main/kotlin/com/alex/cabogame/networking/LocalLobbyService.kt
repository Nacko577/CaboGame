package com.alex.cabogame.networking

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.ServerSocket
import java.net.Socket

class LocalLobbyService(private val context: Context) : LobbyService {
    override var delegate: LobbyServiceDelegate? = null
    override var joinCode: String? = null
        private set

    private val nsdManager by lazy { context.getSystemService(Context.NSD_SERVICE) as NsdManager }
    private val wifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    // NSD/Bonjour service type WITHOUT trailing dot (RFC 6763, Android docs).
    private val serviceType = "_cabo-local._tcp"
    private val json = Json { classDiscriminator = "type"; ignoreUnknownKeys = true }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var serverSocket: ServerSocket? = null
    private val hostConnections = mutableMapOf<String, PeerConnection>()
    private var guestConnection: PeerConnection? = null

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    // UDP broadcast discovery fallback (more reliable across iOS/Android than NSD/Bonjour)
    private val udpDiscoveryPort = 49888
    private var udpDiscoverySocket: DatagramSocket? = null
    private var udpDiscoveryJob: Job? = null

    private inner class PeerConnection(val socket: Socket, val name: String) {
        private val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

        fun sendLine(line: String) {
            try {
                writer.write(line)
                writer.newLine()
                writer.flush()
            } catch (_: Exception) {}
        }

        fun close() = runCatching { socket.close() }
    }

    override fun startHosting(displayName: String) {
        stop()
        val code = makeCode()
        joinCode = code
        notifyMain { delegate?.onJoinCodeUpdated(code) }

        acquireMulticastLock()

        val ss = ServerSocket(0)
        serverSocket = ss

        scope.launch {
            try {
                while (true) {
                    val socket = ss.accept()
                    launch { handleIncomingConnection(socket) }
                }
            } catch (_: Exception) {}
        }

        registerNsdService(code, ss.localPort)
        startUdpDiscoveryResponder(code, ss.localPort)
        notifyMain { delegate?.onConnectionStateChanged("Hosting with code $code") }
    }

    private fun handleIncomingConnection(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val peerName = reader.readLine() ?: return
            val conn = PeerConnection(socket, peerName)
            synchronized(hostConnections) { hostConnections[peerName] = conn }

            val peers = synchronized(hostConnections) {
                hostConnections.values.map { LobbyPeer(it.name, it.name) }
            }
            notifyMain {
                delegate?.onPeersUpdated(peers)
                delegate?.onConnectionStateChanged("$peerName joined")
            }

            while (true) {
                val line = conn.reader.readLine() ?: break
                val msg = runCatching { json.decodeFromString<NetworkMessage>(line) }.getOrNull() ?: continue
                notifyMain { delegate?.onMessageReceived(msg, peerName) }
            }
        } catch (_: Exception) {
        } finally {
            val peerName = synchronized(hostConnections) {
                hostConnections.entries.find { it.value.socket == socket }?.key
                    ?.also { hostConnections.remove(it) }
            }
            if (peerName != null) {
                val remaining = synchronized(hostConnections) {
                    hostConnections.values.map { LobbyPeer(it.name, it.name) }
                }
                notifyMain {
                    delegate?.onPeersUpdated(remaining)
                    delegate?.onConnectionStateChanged("$peerName disconnected")
                }
            }
        }
    }

    override fun startJoining(displayName: String, code: String) {
        stop()
        joinCode = code.uppercase()
        acquireMulticastLock()
        browseForService(displayName, code.uppercase())
        notifyMain { delegate?.onConnectionStateChanged("Searching for host...") }
    }

    private fun browseForService(displayName: String, code: String) {
        val targetName = "cabo-$code"
        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                val name = serviceInfo.serviceName ?: return
                // Prefix match handles NSD collision suffixes like "cabo-XYZ (2)".
                if (name == targetName || name.startsWith(targetName)) {
                    discoveryListener?.let { runCatching { nsdManager.stopServiceDiscovery(it) } }
                    discoveryListener = null
                    resolveAndConnect(serviceInfo, displayName)
                }
            }
        }
        discoveryListener = listener
        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    private fun resolveAndConnect(serviceInfo: NsdServiceInfo, displayName: String) {
        nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                notifyMain { delegate?.onConnectionStateChanged("Could not find host") }
            }
            override fun onServiceResolved(resolved: NsdServiceInfo) {
                val host = resolved.host?.hostAddress ?: return
                connectToHost(host, resolved.port, displayName)
            }
        })
    }

    private fun connectToHost(host: String, port: Int, displayName: String) {
        scope.launch {
            try {
                val socket = Socket(host, port)
                val conn = PeerConnection(socket, "host")
                guestConnection = conn
                conn.sendLine(displayName)

                notifyMain { delegate?.onConnectionStateChanged("Connected to host") }

                while (true) {
                    val line = conn.reader.readLine() ?: break
                    val msg = runCatching { json.decodeFromString<NetworkMessage>(line) }.getOrNull() ?: continue
                    notifyMain { delegate?.onMessageReceived(msg, "host") }
                }
            } catch (_: Exception) {
                notifyMain { delegate?.onConnectionStateChanged("Disconnected") }
            }
        }
    }

    override fun send(message: NetworkMessage) {
        val line = runCatching { json.encodeToString(message) }.getOrNull() ?: return
        scope.launch {
            synchronized(hostConnections) { hostConnections.values.toList() }.forEach { it.sendLine(line) }
            guestConnection?.sendLine(line)
        }
    }

    override fun stop() {
        registrationListener?.let { runCatching { nsdManager.unregisterService(it) } }
        discoveryListener?.let { runCatching { nsdManager.stopServiceDiscovery(it) } }
        registrationListener = null
        discoveryListener = null

        synchronized(hostConnections) { hostConnections.values.forEach { it.close() }; hostConnections.clear() }
        guestConnection?.close(); guestConnection = null
        serverSocket?.close(); serverSocket = null
        stopUdpDiscoveryResponder()
        val hadCode = joinCode != null
        joinCode = null
        if (hadCode) notifyMain { delegate?.onJoinCodeUpdated(null) }
        releaseMulticastLock()
    }

    private fun registerNsdService(code: String, port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "cabo-$code"
            serviceType = this@LocalLobbyService.serviceType
            this.port = port
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {}
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
            override fun onServiceUnregistered(info: NsdServiceInfo) {}
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
        }
        registrationListener = listener
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    private fun startUdpDiscoveryResponder(code: String, tcpPort: Int) {
        stopUdpDiscoveryResponder()
        udpDiscoveryJob = scope.launch(Dispatchers.IO) {
            val socket: MulticastSocket = try {
                MulticastSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(udpDiscoveryPort))
                    broadcast = true
                }
            } catch (_: Exception) {
                return@launch
            }
            runCatching {
                socket.joinGroup(InetAddress.getByName("239.255.99.99"))
            }
            udpDiscoverySocket = socket

            val buffer = ByteArray(1024)
            val expected = "CABO_DISCOVER:$code"
            while (isActive && !socket.isClosed) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val received = String(packet.data, 0, packet.length, Charsets.UTF_8).trim()
                    if (received == expected) {
                        val reply = "CABO_HOST:$tcpPort".toByteArray(Charsets.UTF_8)
                        socket.send(DatagramPacket(reply, reply.size, packet.address, packet.port))
                    }
                } catch (_: Exception) {
                    if (socket.isClosed) break
                }
            }
        }
    }

    private fun stopUdpDiscoveryResponder() {
        runCatching { udpDiscoverySocket?.close() }
        udpDiscoverySocket = null
        udpDiscoveryJob?.cancel()
        udpDiscoveryJob = null
    }

    private fun acquireMulticastLock() {
        if (multicastLock != null) return
        runCatching {
            val lock = wifiManager.createMulticastLock("cabo-mdns").apply {
                setReferenceCounted(true)
                acquire()
            }
            multicastLock = lock
        }
    }

    private fun releaseMulticastLock() {
        multicastLock?.let {
            runCatching { if (it.isHeld) it.release() }
        }
        multicastLock = null
    }

    private fun notifyMain(block: () -> Unit) {
        scope.launch(Dispatchers.Main) { block() }
    }

    companion object {
        private fun makeCode(): String {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            return (1..5).map { chars.random() }.joinToString("")
        }
    }
}
