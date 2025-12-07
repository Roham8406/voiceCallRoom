package com.example;

import com.example.model.GameServer;
import com.example.model.JsonMessageHandler;
import lombok.Getter;
import lombok.Setter;

import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

@Getter
@Setter
public class ClientHandler extends Thread {
    private Socket clientSocket;
    private final AtomicReference<JsonMessageHandler> jsonMessageHandler = new AtomicReference<>();
    private GameServer gameServer;
    private boolean running = true;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        try {
            this.jsonMessageHandler.set(new JsonMessageHandler(clientSocket.getInputStream(), clientSocket.getOutputStream()));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void run() {
        try {
            String message;
            while (running && (message = jsonMessageHandler.get().receive()) != null) {
                try {
                    gameServer.sendAll(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (EOFException eofException) {
            System.out.println("client disconnect: " + clientSocket);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void send(String message) throws IOException {
        synchronized(jsonMessageHandler) {
            jsonMessageHandler.get().send(message);
        }
    }
}
