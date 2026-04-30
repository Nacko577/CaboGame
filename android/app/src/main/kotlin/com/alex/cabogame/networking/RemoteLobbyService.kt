package com.alex.cabogame.networking

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
 */
class RemoteLobbyService(
    private val serverUrl: String = ServerConfig.current
) : LobbyService {
    override var delegate: LobbyServiceDelegate? = null
    override var joinCode: String? = null
        private set

    private val json = Json { classDiscriminator = "type"; ignoreUnknownKeys = true }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var socket: WebSocket? = null
    private var role: Role? = null
    private var localDisplayName: String = ""
    private var pendingHost: Boolean = false
    private var pendingJoinCode: String? = null
    private val peerNames = mutableMapOf<String, String>()

    private enum class Role { HOST, GUEST }

    override fun startHosting(displayName: String) {
        cleanup()
        role = Role.HOST
        localDisplayName = displayName
        pendingHost = true
        pendingJoinCode = null
        notify("Connecting to server...")
        connect()
    }

    override fun startJoining(displayName: String, code: String) {
        cleanup()
        role = Role.GUEST
        localDisplayName = displayName
        pendingHost = false
        pendingJoinCode = code.uppercase()
        notify("Connecting to server...")
        connect()
    }

    override fun stop() {
        runCatching { sendControl(buildJsonObject { put("type", JsonPrimitive("leave")) }) }
        cleanup()
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

    private fun connect() {
        val request = Request.Builder().url(serverUrl).build()
        socket = client.newWebSocket(request, listener)
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            when {
                pendingHost -> sendControl(buildJsonObject {
                    put("type", JsonPrimitive("host"))
                    put("name", JsonPrimitive(localDisplayName))
                })
                pendingJoinCode != null -> sendControl(buildJsonObject {
                    put("type", JsonPrimitive("join"))
                    put("name", JsonPrimitive(localDisplayName))
                    put("code", JsonPrimitive(pendingJoinCode))
                })
            }
            pendingHost = false
            pendingJoinCode = null
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            handleIncoming(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            notify("Disconnected")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            notify("Disconnected: ${t.message ?: "connection error"}")
        }
    }

    private fun handleIncoming(text: String) {
        val obj = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return
        val type = obj["type"]?.jsonPrimitive?.contentOrNull ?: return
        when (type) {
            "hosted" -> {
                val code = obj["code"]?.jsonPrimitive?.contentOrNull ?: ""
                joinCode = code
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
            "hostLeft" -> {
                cleanup()
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
        val text = obj.toString()
        socket?.send(text)
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

    private fun cleanup() {
        runCatching { socket?.close(1000, null) }
        socket = null
        joinCode = null
        role = null
        pendingHost = false
        pendingJoinCode = null
        peerNames.clear()
    }
}
