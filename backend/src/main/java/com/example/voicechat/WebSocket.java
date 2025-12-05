package com.example.voicechat;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Base64;

public class WebSocket {

    private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    public static void doHandshake(OutputStream out, String clientKey) throws Exception {
        String acceptKey = createAcceptKey(clientKey);
        String response =
                "HTTP/1.1 101 Switching Protocols\r\n" +
                        "Upgrade: websocket\r\n" +
                        "Connection: Upgrade\r\n" +
                        "Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n";

        out.write(response.getBytes());
        out.flush();
    }

    private static String createAcceptKey(String key) throws Exception {
        String concat = key + GUID;
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] hash = sha1.digest(concat.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(hash);
    }

    // --- Read WebSocket Frame ---
    public static String readMessage(InputStream in) throws Exception {
        int b1 = in.read();
        if (b1 == -1) return null;

        int b2 = in.read();
        boolean masked = (b2 & 0x80) != 0;
        int length = b2 & 0x7F;

        if (length == 126) {
            length = (in.read() << 8) | in.read();
        }
        else if (length == 127) {
            // too big for this example — skip
            for (int i = 0; i < 8; i++) in.read();
            return null;
        }

        byte[] mask = new byte[4];
        if (masked) {
            in.read(mask);
        }

        byte[] payload = new byte[length];
        int read = 0;
        while (read < length) {
            int r = in.read(payload, read, length - read);
            if (r == -1) return null;
            read += r;
        }

        if (masked) {
            for (int i = 0; i < payload.length; i++)
                payload[i] ^= mask[i % 4];
        }

        return new String(payload, "UTF-8");
    }

    // --- Write WebSocket Frame ---
    public static void sendMessage(OutputStream out, String message) throws Exception {
        byte[] data = message.getBytes("UTF-8");
        int length = data.length;

        out.write(0x81);  // FIN + text frame

        if (length < 126) {
            out.write(length);
        } else if (length < 65536) {
            out.write(126);
            out.write((length >> 8) & 0xFF);
            out.write(length & 0xFF);
        } else {
            // too large for this minimal implementation
            return;
        }

        out.write(data);
        out.flush();
    }
}
