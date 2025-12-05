package com.example.voicechat;

import java.net.ServerSocket;
import java.net.Socket;

public class VoiceChatApplication {

    public static void main(String[] args) throws Exception {
        int port = 8080;

        System.out.println("Starting WebSocket server on ws://localhost:" + port + "/ws");
        ServerSocket serverSocket = new ServerSocket(port);

        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(new Client(socket)).start();
        }
    }
}
