package com.dst.observer;

import com.dst.msg.WarehouseMessage;

import java.io.IOException;

public interface EventListener {
    void update(String event, WarehouseMessage.Task2.Builder task) throws IOException;
}
