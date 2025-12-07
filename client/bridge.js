const fs = require("fs");
const https = require("https");
const WebSocket = require("ws");

// Load your certificate
const server = https.createServer({
    cert: fs.readFileSync("cert.pem"),
    key: fs.readFileSync("key.pem")
});

// Create secure WebSocket server
const wss = new WebSocket.Server({ server });

wss.on("connection", ws => {
    console.log("Browser connected via WSS");

    // Connect to Java TCP server
    const net = require("net");
    const socket = new net.Socket();
    socket.connect(5000, "localhost", () => console.log("Connected to Java backend"));

    // Browser → Java server
    ws.on("message", msg => {
        const data = Buffer.from(msg);
        const len = Buffer.alloc(4);
        len.writeInt32BE(data.length);
        socket.write(Buffer.concat([len, data]));
    });

    // Java → Browser
    let expected = 0;
    let buffer = Buffer.alloc(0);
    socket.on("data", chunk => {
        buffer = Buffer.concat([buffer, chunk]);
        while (true) {
            if (expected === 0) {
                if (buffer.length < 4) break;
                expected = buffer.readInt32BE(0);
                buffer = buffer.slice(4);
            }
            if (buffer.length < expected) break;
            const msg = buffer.slice(0, expected);
            buffer = buffer.slice(expected);
            expected = 0;
            ws.send(msg); // send raw bytes
        }
    });

    ws.on("close", () => socket.destroy());
    socket.on("close", () => ws.close());
});

// Listen with HTTPS
server.listen(8080, "0.0.0.0", () => {
    console.log("WSS bridge running on wss://0.0.0.0:8080");
});
