package com.example.model;


import com.example.ClientHandler;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.util.*;

@Getter
@Setter
public class GameServer {
    private transient final ArrayList<Entry<ServerPlayer, ClientHandler>> clients = new ArrayList<>();
    private int roomId;

    public GameServer(int roomId) {
        this.roomId = roomId;
    }

    public void sendAll(String message, ClientHandler excluded) {
        clients.forEach(client -> {
            if (client.getValue() != excluded) {
                try {
                    client.getValue().send(message);
                } catch (Exception ignored) {
                }
            }
        });
    }
}
