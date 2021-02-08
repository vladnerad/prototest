package com.dst;

import com.dst.msg.WarehouseMessage;
import com.dst.observer.EventManager;
import com.dst.users.DriverStatus;
import com.dst.users.UserDriver;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class TaskStorage2 {

    public static final String changeAct = "change";
    public static volatile EventManager eventManager = new EventManager(changeAct);

    static Comparator<WarehouseMessage.Task2OrBuilder> comparator = Comparator
            .comparing(WarehouseMessage.Task2OrBuilder::getPriority)
            .reversed()
            .thenComparing(WarehouseMessage.Task2OrBuilder::getId);

    private static Set<WarehouseMessage.Task2> allTasks = new ConcurrentSkipListSet<>(comparator);

    private static CopyOnWriteArrayList<UserDriver> drivers = new CopyOnWriteArrayList<>();

    public static void driverFree(UserDriver userDriver){

        //check high prior -> check mass
        //check med prior -> check mass
        //check low
    }

    private static Set<WarehouseMessage.Task2> getPrior(Set<WarehouseMessage.Task2> tasksSet, WarehouseMessage.NewTask.Priority priority){
        return tasksSet.stream()
                .filter(t -> t.getPriority() == priority)
                .collect(Collectors.toSet());
    }

    private static Set<WarehouseMessage.Task2> getWeight (Set<WarehouseMessage.Task2> tasksSet, WarehouseMessage.NewTask.Weight weight) {
        return tasksSet.stream()
                .filter(t -> t.getWeight() == weight)
                .collect(Collectors.toSet());
    }

    private static WarehouseMessage.Task2 getTaskForDriver(UserDriver driver){
        for (int i = WarehouseMessage.NewTask.Priority.HIGH_VALUE; i >= 0; i--){
            for (int j = driver.getWeightClass().getNumber(); j >= 0; j--){
                TreeSet<WarehouseMessage.Task2> set = new TreeSet<>(comparator);
                set.addAll(getWeight(getPrior(allTasks, WarehouseMessage.NewTask.Priority.forNumber(i)), WarehouseMessage.NewTask.Weight.forNumber(j)));
                if (!set.isEmpty()) return set.first();
            }
        }
        return null;
    }

    private static WarehouseMessage.Task2 checkTaskBeforeAdd(WarehouseMessage.Task2 task){
        if (isFreeDriverForTaskExist(task)){
//            System.out.println("drivers online");
            for (int i = task.getWeightValue(); i <= WarehouseMessage.NewTask.Weight.KG_5000_VALUE; i++){
                Set<UserDriver> set = getWeightDrivers(WarehouseMessage.NewTask.Weight.forNumber(i));
                if(!set.isEmpty()){
                    UserDriver driver = set.iterator().next();
                    driver.setStatus(DriverStatus.BUSY);
                    return task.toBuilder().setStatus(WarehouseMessage.Task2.Status.STARTED).setReporter(driver.getUserName()).build();
                } /*else System.out.println("set is empty");*/
            }
        }
//        System.out.println("no drivers");
        return task;

    }

    private boolean isFreeDriverExist(){
        return drivers.stream().anyMatch(d -> d.getStatus() == DriverStatus.FREE);
    }

    private static Set<UserDriver> getWeightDrivers(WarehouseMessage.NewTask.Weight weight){
        return drivers.stream().filter(d -> d.getWeightClass() == weight).collect(Collectors.toSet());
    }

    private static boolean isFreeDriverForTaskExist(WarehouseMessage.Task2 task){
//        System.out.println("list of drivers size: " + drivers.size());
        return drivers.stream()
                .filter(d -> d.getWeightClass().getNumber() >= task.getWeight().getNumber())
                .anyMatch(d -> d.getStatus() == DriverStatus.FREE);
    }

    private static void updateTask(WarehouseMessage.Task2 task){
        allTasks.remove(getTaskById(task.getId()));
        allTasks.add(task);
    }

    public static WarehouseMessage.Task2 getTaskById(int id){
        return allTasks.stream().filter(t -> t.getId() == id).findAny().orElse(null);
    }

    public static WarehouseMessage.Task2 getCurrentDriverTask(UserDriver userDriver){
        return allTasks
                .stream()
                .filter(t -> t.getStatus() == WarehouseMessage.Task2.Status.STARTED)
                .filter(t -> t.getReporter().equals(userDriver.getUserName()))
                .findAny()
                .orElse(null);
    }

    //dispatcher adds task
    public static void addTask(WarehouseMessage.Task2 task){
        WarehouseMessage.Task2 checkedTask = checkTaskBeforeAdd(task);
        allTasks.add(checkedTask);
        eventManager.notify(changeAct, checkedTask);
    }

    public static void finishTask(WarehouseMessage.Task2 task){
        eventManager.notify(changeAct, task.toBuilder().setStatus(WarehouseMessage.Task2.Status.FINISHED).build());
        allTasks.remove(task);
        // updateTask - FINISHED
        // update all listeners
        // remove task from set
    }

    public static void driverCancelTask(UserDriver userDriver){
        WarehouseMessage.Task2 task = getTaskForDriver(userDriver);
        if(task != null){
            System.out.println("task not null");
            WarehouseMessage.Task2.Builder cancelled = task.toBuilder();
            cancelled.setStatus(WarehouseMessage.Task2.Status.WAIT);
            cancelled.setReporter(TaskStorage.noDriverLogin);
            WarehouseMessage.Task2 t2 = cancelled.build();
            System.out.println(t2);
            updateTask(t2);
            eventManager.notify(changeAct, t2);
        }
        // updateTask - WAIT
        // update all listeners
    }

    public static void startNewTask(UserDriver userDriver){
        WarehouseMessage.Task2 task = getTaskForDriver(userDriver);
        if(task != null){
            WarehouseMessage.Task2.Builder startedTask = task.toBuilder();
            startedTask.setStatus(WarehouseMessage.Task2.Status.STARTED);
            startedTask.setReporter(userDriver.getUserName());
            WarehouseMessage.Task2 t2 = startedTask.build();
            updateTask(t2);
            eventManager.notify(changeAct, t2);
            userDriver.setStatus(DriverStatus.BUSY);
        }
    }

    public static Set<WarehouseMessage.Task2> getAllTasks() {
        return allTasks;
    }

    public static void dispCancelTask(WarehouseMessage.Task2 task){
        if(task != null && task.getReporter().equals(TaskStorage.noDriverLogin)){
            WarehouseMessage.Task2.Builder cancelled = task.toBuilder();
            cancelled.setStatus(WarehouseMessage.Task2.Status.CANCELLED);
            eventManager.notify(changeAct, cancelled.build());
            allTasks.remove(task);
        }
    }

    public static void addDriver(UserDriver userDriver){
        drivers.add(userDriver);
    }

    public static void removeDriver(UserDriver userDriver){
        drivers.remove(userDriver);
    }
}