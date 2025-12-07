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

    public void send(String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);

        output.writeInt(data.length);
        output.write(data);
        output.flush();
    }

    public String receive() throws IOException {
        int length = input.readInt();
        byte[] data = new byte[length];
        input.readFully(data);

        return new String(data, StandardCharsets.UTF_8);
    }
}
