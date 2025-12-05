package com.example.voicechat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Client implements Runnable {

    private Socket socket;
    private String userId;
    private String roomId;

    // static shared rooms and client sets
    public static Map<String, Set<Client>> rooms = new ConcurrentHashMap<>();

    public Client(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // --- WebSocket Handshake ---
            String line;
            String key = null;
            boolean isWS = false;

            while ((line = in.readLine()) != null) {
                if (line.contains("GET /ws")) isWS = true;
                if (line.startsWith("Sec-WebSocket-Key:"))
                    key = line.substring("Sec-WebSocket-Key:".length()).trim();

                if (line.isEmpty()) break;
            }

            if (!isWS || key == null) {
                socket.close();
                return;
            }

            WebSocket.doHandshake(socket.getOutputStream(), key);

            // --- Client loop ---
            while (true) {
                String msg = WebSocket.readMessage(socket.getInputStream());
                if (msg == null) break;

                Map<String, String> map = parseJson(msg);
                String type = map.get("type");

                if ("join".equals(type)) {
                    this.userId = map.get("from");
                    this.roomId = map.get("roomId");

                    rooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(this);
                    broadcast(roomId, "{\"type\":\"user-joined\",\"user\":\"" + userId + "\"}");
                }
                else if ("leave".equals(type)) {
                    leaveRoom();
                    break;
                }
                else {
                    broadcast(roomId, msg);
                }
            }
        }
        catch (Exception ignored) {}
        finally {
            leaveRoom();
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    private void leaveRoom() {
        if (roomId != null && rooms.containsKey(roomId)) {
            rooms.get(roomId).remove(this);
            if (rooms.get(roomId).isEmpty())
                rooms.remove(roomId);
        }
    }

    private void broadcast(String room, String msg) {
        if (room == null || !rooms.containsKey(room)) return;

        for (Client c : rooms.get(room)) {
            try {
                WebSocket.sendMessage(c.socket.getOutputStream(), msg);
            } catch (Exception ignored) {}
        }
    }

    private Map<String, String> parseJson(String s) {
        Map<String, String> m = new HashMap<>();
        s = s.trim();
        if (s.startsWith("{")) s = s.substring(1);
        if (s.endsWith("}")) s = s.substring(0, s.length() - 1);

        String[] pairs = s.split(",");
        for (String p : pairs) {
            if (!p.contains(":")) continue;
            String[] kv = p.split(":", 2);
            String key = kv[0].replace("\"", "").trim();
            String val = kv[1].replace("\"", "").trim();
            m.put(key, val);
        }
        return m;
    }
}
