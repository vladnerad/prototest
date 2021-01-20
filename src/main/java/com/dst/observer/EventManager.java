package com.dst.observer;

import com.dst.msg.WarehouseMessage;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager {
    Map<String, List<EventListener>> listeners = new ConcurrentHashMap<>();

    public EventManager(String... operations) {
        for (String operation : operations) {
            this.listeners.put(operation, new CopyOnWriteArrayList<>());
        }
    }

    public void subscribe(String eventType, EventListener listener) {
        List<EventListener> users = listeners.get(eventType);
        users.add(listener);
    }

    public void unsubscribe(String eventType, EventListener listener) {
        List<EventListener> users = listeners.get(eventType);
        users.remove(listener);
    }

    public void notify(String eventType, WarehouseMessage.Task2 task) {
        List<EventListener> users = listeners.get(eventType);
        for (EventListener listener : users) {
            try {
                listener.update(eventType, task);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
