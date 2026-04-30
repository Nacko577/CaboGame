// Quick smoke test: host + 2 guests, verify relay routing and disconnect events.
// Run: node test-smoke.mjs <port>
import { WebSocket } from "ws";

const port = process.argv[2] || 8080;
const url = `ws://localhost:${port}/ws`;

function open() {
  return new Promise((resolve, reject) => {
    const ws = new WebSocket(url);
    ws.once("open", () => resolve(ws));
    ws.once("error", reject);
  });
}

function nextMessage(ws, predicate) {
  return new Promise((resolve, reject) => {
    const onMessage = (raw) => {
      const msg = JSON.parse(raw.toString());
      if (!predicate || predicate(msg)) {
        ws.off("message", onMessage);
        resolve(msg);
      }
    };
    ws.on("message", onMessage);
    setTimeout(() => {
      ws.off("message", onMessage);
      reject(new Error("timeout"));
    }, 3000);
  });
}

const host = await open();
host.send(JSON.stringify({ type: "host", name: "Alice" }));
const hosted = await nextMessage(host, (m) => m.type === "hosted");
console.log("hosted:", hosted);

const guest = await open();
guest.send(JSON.stringify({ type: "join", name: "Bob", code: hosted.code }));
const joined = await nextMessage(guest, (m) => m.type === "joined");
console.log("joined:", joined);

const peerJoined = await nextMessage(host, (m) => m.type === "peerJoined");
console.log("host received peerJoined:", peerJoined);

guest.send(JSON.stringify({ type: "relay", payload: { hello: "from guest" } }));
const relayed = await nextMessage(host, (m) => m.type === "relay");
console.log("host received relay:", relayed);
if (relayed.from !== "Bob" || relayed.payload.hello !== "from guest") {
  throw new Error("relay payload mismatch");
}

host.send(JSON.stringify({ type: "relay", payload: { gameState: "from host" } }));
const relayedToGuest = await nextMessage(guest, (m) => m.type === "relay");
console.log("guest received relay:", relayedToGuest);
if (relayedToGuest.from !== "host" || relayedToGuest.payload.gameState !== "from host") {
  throw new Error("relay payload mismatch from host");
}

const guest2 = await open();
guest2.send(JSON.stringify({ type: "join", name: "Bob", code: hosted.code }));
const joined2 = await nextMessage(guest2, (m) => m.type === "joined");
console.log("joined2:", joined2);
if (joined2.peerId === "Bob") throw new Error("expected disambiguated id");

const hostLeftPromise = nextMessage(guest, (m) => m.type === "hostLeft");
host.close();
const hostLeft = await hostLeftPromise;
console.log("guest received hostLeft:", hostLeft);

guest.close();
guest2.close();
console.log("✅ all good");
process.exit(0);
