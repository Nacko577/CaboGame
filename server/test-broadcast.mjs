// Verifies host -> all-guests broadcast and guest -> host-only routing.
import { WebSocket } from "ws";

const port = process.argv[2] || 8080;
const url = `ws://localhost:${port}/ws`;

function open() {
  return new Promise((resolve, reject) => {
    const ws = new WebSocket(url);
    const queue = [];
    const waiters = [];
    ws.on("message", (raw) => {
      const msg = JSON.parse(raw.toString());
      queue.push(msg);
      drain();
    });
    function drain() {
      for (const w of [...waiters]) {
        const idx = queue.findIndex(w.predicate);
        if (idx >= 0) {
          const [m] = queue.splice(idx, 1);
          waiters.splice(waiters.indexOf(w), 1);
          clearTimeout(w.timer);
          w.resolve(m);
        }
      }
    }
    ws.queue = queue;
    ws.awaitMessage = (predicate) => new Promise((res, rej) => {
      const w = { predicate, resolve: res, reject: rej };
      waiters.push(w);
      w.timer = setTimeout(() => {
        const i = waiters.indexOf(w);
        if (i >= 0) waiters.splice(i, 1);
        rej(new Error(`timeout (queue: ${JSON.stringify(queue)})`));
      }, 3000);
      drain();
    });
    ws.once("open", () => resolve(ws));
    ws.once("error", reject);
  });
}

const host = await open();
host.send(JSON.stringify({ type: "host", name: "Host" }));
const hosted = await host.awaitMessage((m) => m.type === "hosted");

const g1 = await open();
g1.send(JSON.stringify({ type: "join", name: "Guest1", code: hosted.code }));
await g1.awaitMessage((m) => m.type === "joined");

const g2 = await open();
g2.send(JSON.stringify({ type: "join", name: "Guest2", code: hosted.code }));
await g2.awaitMessage((m) => m.type === "joined");

await host.awaitMessage((m) => m.type === "peerJoined" && m.peerId === "Guest1");
await host.awaitMessage((m) => m.type === "peerJoined" && m.peerId === "Guest2");

// Host -> all guests
host.send(JSON.stringify({ type: "relay", payload: { kind: "gameState", n: 1 } }));
const r1 = await g1.awaitMessage((m) => m.type === "relay");
const r2 = await g2.awaitMessage((m) => m.type === "relay");
if (r1.from !== "host" || r2.from !== "host") throw new Error("expected from:host");
if (r1.payload.n !== 1 || r2.payload.n !== 1) throw new Error("payload mismatch");
console.log("host broadcast received by both guests");

// Guest -> host only
const g2QueueLen = g2.queue.length;
g1.send(JSON.stringify({ type: "relay", payload: { kind: "playerAction", a: 1 } }));
const fromG1 = await host.awaitMessage((m) => m.type === "relay");
if (fromG1.from !== "Guest1") throw new Error("expected from:Guest1");
await new Promise((r) => setTimeout(r, 200));
if (g2.queue.length > g2QueueLen) throw new Error("guest relay leaked to other guest");
console.log("guest -> host only confirmed");

host.send(JSON.stringify({ type: "relay", payload: { kind: "gameState", n: 2 } }));
await g1.awaitMessage((m) => m.type === "relay" && m.payload.n === 2);
await g2.awaitMessage((m) => m.type === "relay" && m.payload.n === 2);

host.close();
g1.close();
g2.close();
console.log("✅ broadcast routing verified");
process.exit(0);
