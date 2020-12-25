package com.dst;

import com.dst.msg.WarehouseMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskStorage {
    public static Map<String, WarehouseMessage.ListofTaskDisp.Builder> dispatcherTaskLists = new ConcurrentHashMap<>();
    public static volatile AtomicInteger idCounter = new AtomicInteger(0);
}
