package com.navitech.cabo.networking

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/**
 * LobbyService implementation that talks to the Node.js relay server over
 * WebSockets. The host's device still runs the authoritative game engine;
 * this transport just routes JSON between host and guests.
 *
 * Mobile networks and reverse proxies (e.g. Render) often abort idle sockets.
 * We send periodic JSON `ping` frames (server responds with `pong`), disable
 * OkHttp's own WS pings to avoid duplicate keepalive traffic, and auto-reconnect
 * after unexpected drops. Hosts resume the same lobby via `resumeCode`.
 */
class RemoteLobbyService(
    private val serverUrl: String = ServerConfig.current
) : LobbyService {
    override var delegate: LobbyServiceDelegate? = null
    override var joinCode: String? = null
        private set

    private val json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** OkHttp WebSocket pings disabled — we use JSON ping/pong so proxies see byte traffic. */
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(0, TimeUnit.MILLISECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var socket: WebSocket? = null
    private var role: Role? = null
    private var localDisplayName: String = ""
    /** Lobby code the guest typed (retained for reconnect). */
    private var guestLobbyCode: String? = null
    /** Authoritative code for this hosting session (survives transport teardown). */
    private var hostLobbyCode: String? = null
    private val peerNames = mutableMapOf<String, String>()
    private var intentionalDisconnect = false
    private var reconnectJob: Job? = null
    private var keepAliveJob: Job? = null
    private var reconnectDelayMs = 500L

    private enum class Role { HOST, GUEST }

    override fun startHosting(displayName: String) {
        intentionalDisconnect = false
        reconnectJob?.cancel()
        keepAliveJob?.cancel()
        runCatching { socket?.close(1000, null) }
        socket = null
        cleanupFull()
        role = Role.HOST
        localDisplayName = displayName
        guestLobbyCode = null
        notify("Connecting to server...")
        openSocket()
    }

    override fun startJoining(displayName: String, code: String) {
        intentionalDisconnect = false
        reconnectJob?.cancel()
        keepAliveJob?.cancel()
        runCatching { socket?.close(1000, null) }
        socket = null
        cleanupFull()
        role = Role.GUEST
        localDisplayName = displayName
        guestLobbyCode = code.uppercase()
        notify("Connecting to server...")
        openSocket()
    }

    override fun stop() {
        intentionalDisconnect = true
        reconnectJob?.cancel()
        reconnectJob = null
        keepAliveJob?.cancel()
        keepAliveJob = null
        runCatching {
            sendControl(buildJsonObject { put("type", JsonPrimitive("leave")) })
        }
        runCatching { socket?.close(1000, null) }
        socket = null
        cleanupFull()
        notifyJoinCode(null)
        notify("Disconnected")
    }

    override fun send(message: NetworkMessage) {
        val payloadString = runCatching { json.encodeToString(message) }.getOrNull() ?: return
        val payload = runCatching { json.parseToJsonElement(payloadString) }.getOrNull() ?: return
        val envelope = buildJsonObject {
            put("type", JsonPrimitive("relay"))
            put("payload", payload)
        }
        sendControl(envelope)
    }

    private fun openSocket() {
        if (intentionalDisconnect) return
        runCatching { socket?.close(1001, "reconnecting") }
        socket = null
        keepAliveJob?.cancel()
        socket = client.newWebSocket(Request.Builder().url(serverUrl).build(), listener)
    }

    private fun scheduleReconnect() {
        if (intentionalDisconnect || role == null) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(reconnectDelayMs)
            reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(30_000)
            if (!intentionalDisconnect && role != null) {
                notifyMain { delegate?.onConnectionStateChanged("Reconnecting…") }
                openSocket()
            }
        }
    }

    private fun startKeepAlive() {
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            while (isActive && !intentionalDisconnect && socket != null) {
                delay(25_000)
                if (intentionalDisconnect || socket == null) break
                sendControl(buildJsonObject { put("type", JsonPrimitive("ping")) })
            }
        }
    }

    private fun sendHandshake() {
        when (role) {
            Role.HOST -> {
                val resume = hostLobbyCode ?: joinCode
                if (resume != null) {
                    sendControl(buildJsonObject {
                        put("type", JsonPrimitive("host"))
                        put("name", JsonPrimitive(localDisplayName))
                        put("resumeCode", JsonPrimitive(resume))
                    })
                } else {
                    sendControl(buildJsonObject {
                        put("type", JsonPrimitive("host"))
                        put("name", JsonPrimitive(localDisplayName))
                    })
                }
            }
            Role.GUEST -> {
                val code = guestLobbyCode ?: return
                sendControl(buildJsonObject {
                    put("type", JsonPrimitive("join"))
                    put("name", JsonPrimitive(localDisplayName))
                    put("code", JsonPrimitive(code))
                })
            }
            null -> Unit
        }
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            reconnectJob?.cancel()
            reconnectJob = null
            reconnectDelayMs = 500L
            sendHandshake()
            startKeepAlive()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            handleIncoming(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (intentionalDisconnect) return
            if (webSocket !== socket) return
            keepAliveJob?.cancel()
            socket = null
            scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (intentionalDisconnect) return
            if (webSocket !== socket) return
            keepAliveJob?.cancel()
            socket = null
            notifyMain {
                delegate?.onConnectionStateChanged("Reconnecting… (${t.message ?: "dropped"})")
            }
            scheduleReconnect()
        }
    }

    private fun handleIncoming(text: String) {
        val obj = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return
        val type = obj["type"]?.jsonPrimitive?.contentOrNull ?: return
        when (type) {
            "hosted" -> {
                val code = obj["code"]?.jsonPrimitive?.contentOrNull ?: ""
                val previous = joinCode
                hostLobbyCode = code
                joinCode = code
                if (previous != null && previous != code) {
                    notify("Game code changed to $code — deploy the latest relay to keep the same room after drops.")
                }
                notifyJoinCode(code)
                notify("Hosting with code $code")
            }
            "joined" -> {
                joinCode = obj["code"]?.jsonPrimitive?.contentOrNull ?: joinCode
                notifyMain { delegate?.onPeersUpdated(listOf(LobbyPeer("host", "host"))) }
                notify("Connected to host")
            }
            "peerJoined" -> {
                val id = obj["peerId"]?.jsonPrimitive?.contentOrNull ?: return
                val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: id
                peerNames[id] = name
                broadcastPeerListIfHost()
                notify("$name joined")
            }
            "peerLeft" -> {
                val id = obj["peerId"]?.jsonPrimitive?.contentOrNull ?: return
                val name = peerNames.remove(id)
                    ?: obj["name"]?.jsonPrimitive?.contentOrNull
                    ?: id
                broadcastPeerListIfHost()
                notify("$name disconnected")
            }
            "hostReconnecting" -> {
                notify("Host reconnecting…")
            }
            "hostLeft" -> {
                intentionalDisconnect = true
                reconnectJob?.cancel()
                keepAliveJob?.cancel()
                runCatching { socket?.close(1000, null) }
                socket = null
                cleanupFull()
                notifyJoinCode(null)
                notify("Host left the lobby")
            }
            "relay" -> {
                val from = obj["from"]?.jsonPrimitive?.contentOrNull ?: "peer"
                val payload: JsonElement = obj["payload"] ?: return
                val msg = runCatching {
                    json.decodeFromString<NetworkMessage>(payload.toString())
                }.getOrNull() ?: return
                notifyMain { delegate?.onMessageReceived(msg, from) }
            }
            "error" -> {
                val msg = obj["message"]?.jsonPrimitive?.contentOrNull ?: "Unknown error"
                notify("Server: $msg")
            }
            "pong" -> Unit
        }
    }

    private fun broadcastPeerListIfHost() {
        if (role != Role.HOST) return
        val peers = peerNames.values.map { LobbyPeer(it, it) }
        notifyMain { delegate?.onPeersUpdated(peers) }
    }

    private fun sendControl(obj: JsonObject) {
        socket?.send(obj.toString())
    }

    private fun notify(text: String) {
        notifyMain { delegate?.onConnectionStateChanged(text) }
    }

    private fun notifyJoinCode(code: String?) {
        notifyMain { delegate?.onJoinCodeUpdated(code) }
    }

    private fun notifyMain(block: () -> Unit) {
        scope.launch(Dispatchers.Main) { block() }
    }

    private fun cleanupFull() {
        joinCode = null
        hostLobbyCode = null
        role = null
        guestLobbyCode = null
        peerNames.clear()
    }
}
