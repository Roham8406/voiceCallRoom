package com.example;

import io.github.some_example_name.server.model.GameThread;

public class Server {

    public static void main(String[] args) {
        GameThread.getInstance().start();
    }
}
