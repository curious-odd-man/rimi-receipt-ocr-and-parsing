package com.github.curiousoddman.receipt.parsing.utils;

import java.io.Serial;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class ListValueHashMap<K, V> extends LinkedHashMap<K, List<V>> {

    @Serial
    private static final long serialVersionUID = 4274613793045131611L;

    public void add(K key, V value) {
        computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }
}
