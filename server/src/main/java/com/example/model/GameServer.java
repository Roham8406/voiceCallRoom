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

    public void sendAll(String message) {
        clients.forEach(client -> {
            try {
                client.getValue().send(message);
            } catch (Exception ignored) {
            }
        });
    }
}
