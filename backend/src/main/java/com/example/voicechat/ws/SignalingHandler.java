package com.example.voicechat.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple WebSocket signaling handler.
 * This handler accepts JSON messages with a 'type' field and optional 'to' field.
 * It routes messages to the intended recipient or broadcasts within a room.
 *
 * Message format (JSON):
 * {
 *   "type":"join"|"leave"|"offer"|"answer"|"ice"|"participants",
 *   "roomId":"room1",
 *   "from":"userA",
 *   "to":"userB",         // optional
 *   "payload":{...}      // optional
 * }
 */
@Component
public class SignalingHandler extends TextWebSocketHandler {

    private final ObjectMapper mapper = new ObjectMapper();

    // sessionId -> SessionInfo
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    // roomId -> set of sessionIds
    private final Map<String, Set<String>> rooms = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // nothing yet; client will send 'join' message
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> msg = mapper.readValue(message.getPayload(), Map.class);
        String type = (String) msg.get("type");
        String roomId = (String) msg.get("roomId");
        String from = (String) msg.get("from");
        String to = (String) msg.get("to");
        Object payload = msg.get("payload");

        if ("join".equals(type)) {
            String sessionId = session.getId();
            sessions.put(sessionId, new SessionInfo(session, from, roomId));
            rooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
            // notify others in room
            broadcastToRoom(roomId, makeEvent("participant-joined", from, null, Map.of("participants", getParticipants(roomId))));
            return;
        }

        if ("leave".equals(type)) {
            removeSession(session);
            return;
        }

        // routing: if 'to' provided, send to that userId in the same room.
        if (to != null) {
            // find target session in same room with matching userId
            String targetSessionId = findSessionIdByUserAndRoom(to, roomId);
            if (targetSessionId != null) {
                sendToSession(targetSessionId, makeEvent(type, from, to, Map.of("payload", payload)));
            }
            return;
        }

        // otherwise broadcast to room
        if (roomId != null) {
            broadcastToRoom(roomId, makeEvent(type, from, null, Map.of("payload", payload)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        removeSession(session);
    }

    private void removeSession(WebSocketSession session) {
        String sid = session.getId();
        SessionInfo info = sessions.remove(sid);
        if (info == null) return;
        String roomId = info.roomId;
        String userId = info.userId;
        Set<String> set = rooms.get(roomId);
        if (set != null) {
            set.remove(sid);
            if (set.isEmpty()) rooms.remove(roomId);
        }
        try {
            broadcastToRoom(roomId, makeEvent("participant-left", userId, null, Map.of("participants", getParticipants(roomId))));
        } catch (Exception ignored) {}
    }

    private void sendToSession(String sessionId, Map<String,Object> event) {
        SessionInfo info = sessions.get(sessionId);
        if (info == null) return;
        try {
            info.session.sendMessage(new TextMessage(mapper.writeValueAsString(event)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void broadcastToRoom(String roomId, Map<String,Object> event) {
        Set<String> set = rooms.get(roomId);
        if (set == null) return;
        String json;
        try {
            json = mapper.writeValueAsString(event);
        } catch (Exception e) { return; }
        for (String sid : set) {
            SessionInfo info = sessions.get(sid);
            if (info == null) continue;
            try {
                info.session.sendMessage(new TextMessage(json));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String findSessionIdByUserAndRoom(String userId, String roomId) {
        Set<String> set = rooms.get(roomId);
        if (set == null) return null;
        for (String sid : set) {
            SessionInfo info = sessions.get(sid);
            if (info != null && userId.equals(info.userId)) return sid;
        }
        return null;
    }

    private List<String> getParticipants(String roomId) {
        Set<String> set = rooms.get(roomId);
        if (set == null) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (String sid : set) {
            SessionInfo info = sessions.get(sid);
            if (info != null) out.add(info.userId);
        }
        return out;
    }

    private Map<String,Object> makeEvent(String type, String from, String to, Map<String,Object> extra) {
        Map<String,Object> m = new HashMap<>();
        m.put("type", type);
        if (from != null) m.put("from", from);
        if (to != null) m.put("to", to);
        if (extra != null) m.putAll(extra);
        return m;
    }

    static class SessionInfo {
        final WebSocketSession session;
        final String userId;
        final String roomId;
        SessionInfo(WebSocketSession s, String userId, String roomId) {
            this.session = s; this.userId = userId; this.roomId = roomId;
        }
    }

}
