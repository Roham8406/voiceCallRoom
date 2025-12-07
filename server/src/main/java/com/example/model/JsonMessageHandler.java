package com.example.model;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class JsonMessageHandler {
    private final DataInputStream input;
    private final DataOutputStream output;

    public JsonMessageHandler(InputStream input, OutputStream output) {
        this.input = new DataInputStream(input);
        this.output = new DataOutputStream(output);
    }

    public void send(byte[] data) throws IOException {
        output.writeInt(data.length);
        output.write(data);
        output.flush();
    }


    public byte[] receive() throws IOException {
        int length = input.readInt();
        byte[] data = new byte[length];
        input.readFully(data);

        return data;
    }
}
