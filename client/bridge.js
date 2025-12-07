const WebSocket = require("ws");
const net = require("net");

const JAVA_HOST = "localhost";
const JAVA_PORT = 5000;

const wss = new WebSocket.Server({ port: 8080 });

wss.on("connection", ws => {
    console.log("Browser connected");

    // Connect to Java TCP server
    const socket = new net.Socket();
    socket.connect(JAVA_PORT, JAVA_HOST, () => {
        console.log("Bridge connected to Java server");
    });

    // Browser → Java server
    ws.on("message", msg => {
        const data = Buffer.from(msg, "utf8");
        const len = Buffer.alloc(4);
        len.writeInt32BE(data.length);
        socket.write(Buffer.concat([len, data]));
    });

    // Java server → Browser
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

            const msg = buffer.slice(0, expected).toString("utf8");
            buffer = buffer.slice(expected);
            expected = 0;

            ws.send(msg);
        }
    });

    ws.on("close", () => {
        socket.destroy();
    });

    socket.on("close", () => {
        ws.close();
    });
});

console.log("WebSocket bridge running on ws://localhost:8080");
