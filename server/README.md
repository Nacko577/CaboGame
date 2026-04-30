# Cabo Relay Server

A tiny WebSocket relay so CaboGame friends can play across networks. The server
is **stateless about the game itself** — it just routes JSON messages between a
host and the guests in the same lobby. The host's device still runs the
authoritative game engine.

## Protocol

All messages are JSON. A client opens a WebSocket to `/ws`, then sends one of:

| Client → Server | Description |
| --- | --- |
| `{"type":"host","name":"Alice"}` | Create a new lobby. Server replies with `{"type":"hosted","code":"ABCDE"}`. |
| `{"type":"join","name":"Bob","code":"ABCDE"}` | Join an existing lobby. Server replies with `{"type":"joined","code":"ABCDE","peerId":"Bob"}`. |
| `{"type":"relay","payload":{...}}` | Forward an opaque game payload to the other side of the lobby. |
| `{"type":"leave"}` | Disconnect. |
| `{"type":"ping"}` | Keepalive (server replies `{"type":"pong"}`). |

The server never inspects `payload` — it just routes it:

- Host → all guests
- Guest → host only

| Server → Client | Description |
| --- | --- |
| `{"type":"hosted","code":"ABCDE"}` | Lobby created. |
| `{"type":"joined","code":"ABCDE","peerId":"Bob"}` | You joined a lobby. |
| `{"type":"peerJoined","peerId":"Bob","name":"Bob"}` | Sent to host when a guest joins. |
| `{"type":"peerLeft","peerId":"Bob","name":"Bob"}` | Sent to host when a guest leaves. |
| `{"type":"hostLeft"}` | Sent to guests when the host disconnects (their connection is then closed). |
| `{"type":"relay","from":"<peerId>","payload":{...}}` | Relayed payload. |
| `{"type":"error","message":"..."}` | Error. |
| `{"type":"pong"}` | Reply to ping. |

A host disconnect destroys the lobby and kicks every guest.

## Run locally

```bash
cd server
npm install
npm start    # listens on :8080 by default
```

Sanity check: `curl http://localhost:8080/healthz` should return `cabo-relay ok`.

The iOS / Android apps can then point at `ws://<your-mac-ip>:8080/ws` (use a
real LAN IP — `localhost` won't work from a phone). For App Store builds /
release Android builds you should host this somewhere with TLS and use
`wss://...`.

## Deploy for free

The repo includes config for two free options. Pick whichever you prefer and
delete the other config file if you want.

### Render (recommended — actually free)

1. Push this repo to GitHub.
2. In Render, create a new **Blueprint** instance and point it at this repo.
   The included `render.yaml` will create a Docker-based web service on the
   free plan.
3. Note the public URL Render gives you (e.g. `https://cabo-relay.onrender.com`).
4. The WebSocket URL is `wss://cabo-relay.onrender.com/ws`. Plug that into the
   iOS and Android apps (see the per-platform README).

> ⚠️  Render's free plan spins the service down after 15 minutes of inactivity
> and the first reconnect can take 30–60 seconds while it boots. For a
> friends-only Cabo lobby that's perfectly fine.

### Fly.io

Free credit plus generous limits.

```bash
cd server
fly launch --copy-config --no-deploy   # adjust the app name in fly.toml
fly deploy
```

Your URL will be `wss://<app-name>.fly.dev/ws`.

### Anywhere else

The included `Dockerfile` works on any container host (a $4 VPS, Railway,
Koyeb, etc.). The only requirement is that the host terminates TLS and
forwards WebSocket upgrade requests to port 8080.
