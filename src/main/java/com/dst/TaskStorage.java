package com.dst;

import com.dst.msg.WarehouseMessage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TaskStorage {
    // List of task builder for each dispatcher
//    public static Map<String, WarehouseMessage.ListofTaskDisp.Builder> dispatcherTaskLists = new ConcurrentHashMap<>();
    // Id for task
    public static volatile AtomicInteger idCounter = new AtomicInteger(0);

    public static Set<WarehouseMessage.Task2> allTasks = new HashSet<>();

//    public void fullUpdate(){
////        for (Map.Entry<String, WarehouseMessage.ListofTaskDisp.Builder> entry: dispatcherTaskLists.entrySet()){
////
////        }
//
//        allTasks = Arrays.stream(dispatcherTaskLists
//                .values()
//                .stream()
//                .map(WarehouseMessage.ListofTaskDisp.Builder::getTaskList)
//                .toArray())
//                .filter(obj -> obj instanceof WarehouseMessage.Task2)
//                .map(obj -> (WarehouseMessage.Task2) obj)
//                .collect(Collectors.toSet());
//        // taskList to List
//    }
}
