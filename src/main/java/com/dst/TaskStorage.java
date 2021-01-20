package com.dst;

import com.dst.msg.WarehouseMessage;
import com.dst.observer.EventManager;

import java.util.Comparator;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskStorage {
    // Id for task
    public static volatile AtomicInteger idCounter = new AtomicInteger(0);

    public static final String noDriverLogin = "Not assigned";

    public static ConcurrentSkipListSet<WarehouseMessage.Task2.Builder> allTasks = new ConcurrentSkipListSet<>(Comparator.comparingInt(o -> o.getPriority().getNumber()));

    public static volatile EventManager eventManager = new EventManager("changed"/*"create", "delete", "started"*/);
}
