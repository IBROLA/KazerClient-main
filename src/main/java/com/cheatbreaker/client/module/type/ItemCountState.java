package com.cheatbreaker.client.module.type;

import java.util.ArrayList;
import java.util.List;

public class ItemCountState {
    public int version = 1;
    public List<ItemCountChildState> children = defaults();

    public static List<ItemCountChildState> defaults() {
        List<ItemCountChildState> result = new ArrayList<>();
        for (ItemCountType type : ItemCountType.values()) {
            result.add(new ItemCountChildState(type));
        }
        return result;
    }
}
