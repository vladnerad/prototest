package com.dst;

import com.dst.msg.WarehouseMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskStorage {
    // List of task builder for each dispatcher
    public static Map<String, WarehouseMessage.ListofTaskDisp.Builder> dispatcherTaskLists = new ConcurrentHashMap<>();
    // Id for task
    public static volatile AtomicInteger idCounter = new AtomicInteger(0);
}
