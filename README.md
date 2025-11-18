# Voice Chat App (Demo)

This repository contains a minimal demo of a voice chat application:
- Java Spring Boot backend (WebSocket signaling)
- JavaScript frontend (WebRTC + WebSocket)
- Local mute implemented by muting audio elements on the client

How to run (requires Docker and Docker Compose):

    docker-compose up --build

- Backend signaling server on http://localhost:8080 (WebSocket at ws://localhost:8080/ws)
- Frontend static server on http://localhost:3000

Notes:
- This is a demo starter project. The signaling server keeps all state in-memory.
- For production: add authentication, TLS (wss), TURN server, and persistence.
