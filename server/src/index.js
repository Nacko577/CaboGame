import http from "node:http";
import { WebSocketServer } from "ws";

const PORT = Number(process.env.PORT ?? 8080);
const HEARTBEAT_INTERVAL_MS = 30_000;
const CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
const CODE_LENGTH = 5;
const MAX_LOBBIES = 1024;

/**
 * @typedef {Object} Member
 * @property {import("ws").WebSocket} ws
 * @property {string} role  "host" | "guest"
 * @property {string} name
 * @property {string} id    stable id used in relayed `from` field
 */

/** @type {Map<string, { code: string, host: Member|null, guests: Map<string, Member>, hostExpiresAt: number|null }>} */
const lobbies = new Map();

const httpServer = http.createServer((req, res) => {
  if (req.url === "/healthz" || req.url === "/") {
    res.writeHead(200, { "content-type": "text/plain" });
    res.end("cabo-relay ok");
    return;
  }
  res.writeHead(404);
  res.end();
});

const wss = new WebSocketServer({ server: httpServer, path: "/ws" });

wss.on("connection", (ws) => {
  ws.isAlive = true;
  ws.on("pong", () => { ws.isAlive = true; });

  // Connection metadata. Filled in once the client sends `host` or `join`.
  ws.cabo = { lobbyCode: null, role: null, name: null, id: null };

  ws.on("message", (raw) => {
    let msg;
    try {
      msg = JSON.parse(raw.toString());
    } catch {
      sendError(ws, "Invalid JSON");
      return;
    }
    handleMessage(ws, msg);
  });

  ws.on("close", () => handleDisconnect(ws));
  ws.on("error", () => { /* swallow; close fires next */ });
});

function handleMessage(ws, msg) {
  const type = msg?.type;
  if (!type) {
    sendError(ws, "Missing message type");
    return;
  }

  switch (type) {
    case "host":
      handleHost(ws, msg);
      return;
    case "join":
      handleJoin(ws, msg);
      return;
    case "relay":
      handleRelay(ws, msg);
      return;
    case "leave":
      ws.close();
      return;
    case "ping":
      send(ws, { type: "pong" });
      return;
    default:
      sendError(ws, `Unknown message type: ${type}`);
  }
}

function handleHost(ws, msg) {
  if (ws.cabo.lobbyCode) {
    sendError(ws, "Already in a lobby");
    return;
  }
  if (lobbies.size >= MAX_LOBBIES) {
    sendError(ws, "Server at capacity, try again later");
    return;
  }
  const name = sanitizeName(msg?.name);

  const code = generateUniqueCode();
  const id = "host";
  const lobby = { code, host: { ws, role: "host", name, id }, guests: new Map() };
  lobbies.set(code, lobby);

  ws.cabo = { lobbyCode: code, role: "host", name, id };
  send(ws, { type: "hosted", code });
  console.log(`[host] code=${code} name=${name} (lobbies=${lobbies.size})`);
}

function handleJoin(ws, msg) {
  if (ws.cabo.lobbyCode) {
    sendError(ws, "Already in a lobby");
    return;
  }
  const code = String(msg?.code ?? "").trim().toUpperCase();
  if (!code) {
    sendError(ws, "Missing code");
    return;
  }
  const lobby = lobbies.get(code);
  if (!lobby) {
    sendError(ws, "Lobby not found");
    return;
  }
  if (!lobby.host) {
    sendError(ws, "Host left the lobby");
    return;
  }

  const name = sanitizeName(msg?.name);
  const id = uniqueGuestId(lobby, name);
  const member = { ws, role: "guest", name, id };
  lobby.guests.set(id, member);

  ws.cabo = { lobbyCode: code, role: "guest", name, id };
  send(ws, { type: "joined", code, peerId: id });
  send(lobby.host.ws, { type: "peerJoined", peerId: id, name });
  console.log(`[join] code=${code} name=${name} id=${id} (guests=${lobby.guests.size})`);
}

function handleRelay(ws, msg) {
  const lobby = currentLobby(ws);
  if (!lobby) {
    sendError(ws, "Not in a lobby");
    return;
  }
  const payload = msg?.payload;
  if (payload === undefined) {
    sendError(ws, "Missing payload");
    return;
  }

  const fromId = ws.cabo.id;
  const envelope = { type: "relay", from: fromId, payload };

  if (ws.cabo.role === "host") {
    // Host -> all guests
    for (const guest of lobby.guests.values()) {
      send(guest.ws, envelope);
    }
  } else {
    // Guest -> host only (host is authoritative). Drop silently if no host.
    if (lobby.host) send(lobby.host.ws, envelope);
  }
}

function handleDisconnect(ws) {
  const meta = ws.cabo;
  if (!meta || !meta.lobbyCode) return;
  const lobby = lobbies.get(meta.lobbyCode);
  if (!lobby) return;

  if (meta.role === "host") {
    // Host left: kick guests and drop the lobby. Friends rehost with a new code.
    for (const guest of lobby.guests.values()) {
      try { send(guest.ws, { type: "hostLeft" }); } catch {}
      try { guest.ws.close(); } catch {}
    }
    lobbies.delete(meta.lobbyCode);
    console.log(`[hostLeft] code=${meta.lobbyCode} (lobbies=${lobbies.size})`);
  } else {
    lobby.guests.delete(meta.id);
    if (lobby.host) {
      send(lobby.host.ws, { type: "peerLeft", peerId: meta.id, name: meta.name });
    }
    console.log(`[guestLeft] code=${meta.lobbyCode} id=${meta.id}`);
  }
}

function currentLobby(ws) {
  const code = ws.cabo?.lobbyCode;
  return code ? lobbies.get(code) : null;
}

function sanitizeName(input) {
  const raw = (typeof input === "string" ? input : "").trim();
  if (!raw) return "Player";
  return raw.slice(0, 32);
}

function uniqueGuestId(lobby, name) {
  // Use the player name as the id when possible — clients display peers by
  // name and the host engine keys off displayName. Disambiguate collisions.
  let candidate = name;
  let i = 2;
  while (lobby.guests.has(candidate) || candidate === "host") {
    candidate = `${name}#${i++}`;
  }
  return candidate;
}

function generateUniqueCode() {
  for (let attempt = 0; attempt < 100; attempt++) {
    const code = randomCode();
    if (!lobbies.has(code)) return code;
  }
  // Astronomically unlikely with 32^5 = ~33M codes, but just in case.
  throw new Error("Could not allocate a unique lobby code");
}

function randomCode() {
  let out = "";
  for (let i = 0; i < CODE_LENGTH; i++) {
    out += CODE_ALPHABET[Math.floor(Math.random() * CODE_ALPHABET.length)];
  }
  return out;
}

function send(ws, obj) {
  if (ws.readyState !== ws.OPEN) return;
  try { ws.send(JSON.stringify(obj)); } catch {}
}

function sendError(ws, message) {
  send(ws, { type: "error", message });
}

const heartbeat = setInterval(() => {
  for (const ws of wss.clients) {
    if (ws.isAlive === false) {
      try { ws.terminate(); } catch {}
      continue;
    }
    ws.isAlive = false;
    try { ws.ping(); } catch {}
  }
}, HEARTBEAT_INTERVAL_MS);

wss.on("close", () => clearInterval(heartbeat));

httpServer.listen(PORT, () => {
  console.log(`cabo relay listening on :${PORT}`);
});

process.on("SIGTERM", shutdown);
process.on("SIGINT", shutdown);

function shutdown() {
  console.log("shutting down");
  for (const ws of wss.clients) {
    try { ws.close(1001, "server shutting down"); } catch {}
  }
  httpServer.close(() => process.exit(0));
  setTimeout(() => process.exit(0), 5_000).unref();
}
