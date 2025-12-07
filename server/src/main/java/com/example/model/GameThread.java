package com.example.model;

import com.example.ClientHandler;
import lombok.Getter;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class GameThread extends Thread {
    private static GameThread instance;
    private static final int PORT = 5000;
    private final HashMap<Integer, GameServer> games = new HashMap<>();
    private final Map<String, ClientHandler> connections = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, List<String>> users = new HashMap<>();

    private GameThread() {
        games.put(100, new GameServer(100));
    }

    public static GameThread getInstance() {
        if (instance == null) {
            instance = new GameThread();
        }
        return instance;
    }

    public GameServer getGameServerForStart(long id) {
        return games.get(id);
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
                ClientHandler clientThread = new ClientHandler(clientSocket, games.get(100));
                games.get(100).getClients().add(new Entry<>(new ServerPlayer("101"), clientThread));
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
