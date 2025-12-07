package com.example.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Entry<T, K> {
    private T key;
    private K value;

    public Entry(T x, K y) {
        this.key = x;
        this.value = y;
    }
}
