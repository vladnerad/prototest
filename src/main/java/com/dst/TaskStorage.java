package com.dst;

import com.dst.msg.WarehouseMessage;
import com.dst.observer.EventManager;

import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskStorage {
    // Id for task
    public static volatile AtomicInteger idCounter = new AtomicInteger(0);

    public static final String noDriverLogin = "Not assigned";
    public static final String changeAct = "change";
    public static final String addAfterEmpty = "not null";

    static Comparator<WarehouseMessage.Task2OrBuilder> comparator = Comparator
            .comparing(WarehouseMessage.Task2OrBuilder::getPriority)
            .reversed()
            .thenComparing(WarehouseMessage.Task2OrBuilder::getId);

    public static Set<WarehouseMessage.Task2.Builder> allTasks = new ConcurrentSkipListSet<>(comparator);

    public static volatile EventManager eventManager = new EventManager(changeAct, addAfterEmpty);

//    public static void printStatusInfo(){
//
//    }

}
