package com.example.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServerPlayer {
    public String username;
    public Integer port;

    public ServerPlayer(String username) {
        this.username = username;
    }
}
