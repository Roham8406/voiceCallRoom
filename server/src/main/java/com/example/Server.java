package com.example;


import com.example.model.GameThread;

public class Server {

    public static void main(String[] args) {
        GameThread.getInstance().start();
    }
}
