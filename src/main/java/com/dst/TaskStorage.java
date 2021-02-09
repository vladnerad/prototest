package com.dst;

import com.dst.msg.WarehouseMessage;
import com.dst.observer.EventManager;
import com.dst.server.DispatcherExchanger;
import com.dst.users.DriverStatus;
import com.dst.users.UserDriver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class TaskStorage {

    private static final Logger logger = LogManager.getLogger(TaskStorage.class);
    public static final String changeAct = "change";
    public static volatile EventManager eventManager = new EventManager(changeAct);

    static Comparator<WarehouseMessage.Task2OrBuilder> comparator = Comparator
            .comparing(WarehouseMessage.Task2OrBuilder::getPriority)
            .reversed()
            .thenComparing(WarehouseMessage.Task2OrBuilder::getId);

    private static final Set<WarehouseMessage.Task2> allTasks = new ConcurrentSkipListSet<>(comparator);

    private static final CopyOnWriteArrayList<UserDriver> drivers = new CopyOnWriteArrayList<>();

    private static Set<WarehouseMessage.Task2> getPrior(Set<WarehouseMessage.Task2> tasksSet, WarehouseMessage.NewTask.Priority priority) {
        return tasksSet.stream()
                .filter(t -> t.getPriority() == priority)
                .collect(Collectors.toSet());
    }

    private static Set<WarehouseMessage.Task2> getWeight(Set<WarehouseMessage.Task2> tasksSet, WarehouseMessage.NewTask.Weight weight) {
        return tasksSet.stream()
                .filter(t -> t.getWeight() == weight)
                .collect(Collectors.toSet());
    }

    private /*synchronized*/ static WarehouseMessage.Task2 getTaskForDriver(UserDriver driver) {
        for (int i = WarehouseMessage.NewTask.Priority.HIGH_VALUE; i >= 0; i--) {
            for (int j = driver.getWeightClass().getNumber(); j >= 0; j--) {
                TreeSet<WarehouseMessage.Task2> set = new TreeSet<>(comparator);
                set.addAll(
                        getWaitingTasks(WarehouseMessage.NewTask.Priority.forNumber(i), WarehouseMessage.NewTask.Weight.forNumber(j)));
                if (!set.isEmpty()) {
                    return set.stream().filter(t -> t.getStatus() == WarehouseMessage.Task2.Status.WAIT).findFirst().orElse(null);
                }
            }
        }
        return null;
    }

    private static Set<WarehouseMessage.Task2> getWaitingTasks(WarehouseMessage.NewTask.Priority priority, WarehouseMessage.NewTask.Weight weight) {
        return getWeight(
                getPrior(allTasks, priority),
                weight)
                .stream()
                .filter(t -> t.getStatus() == WarehouseMessage.Task2.Status.WAIT)
                .collect(Collectors.toSet());
    }

    private synchronized static WarehouseMessage.Task2 checkTaskBeforeAdd(WarehouseMessage.Task2 task) {
        if (isFreeDriverForTaskExist(task)) {
            for (int i = task.getWeightValue(); i <= WarehouseMessage.NewTask.Weight.KG_5000_VALUE; i++) {
                Set<UserDriver> set = getWeightDrivers(WarehouseMessage.NewTask.Weight.forNumber(i));
                if (!set.isEmpty()) {
                    UserDriver driver = set.stream().filter(d -> d.getStatus() == DriverStatus.FREE).findFirst().orElse(null);
                    if (driver != null) {
                        driver.setStatus(DriverStatus.BUSY);
                        return task.toBuilder().setStatus(WarehouseMessage.Task2.Status.STARTED).setReporter(driver.getUserName()).build();
                    }
                }
            }
        }
        return task;

    }

//    private boolean isFreeDriverExist(){
//        return drivers.stream().anyMatch(d -> d.getStatus() == DriverStatus.FREE);
//    }

    private static Set<UserDriver> getWeightDrivers(WarehouseMessage.NewTask.Weight weight) {
        return drivers.stream().filter(d -> d.getWeightClass() == weight).collect(Collectors.toSet());
    }

    private static boolean isFreeDriverForTaskExist(WarehouseMessage.Task2 task) {
        return drivers.stream()
                .filter(d -> d.getWeightClass().getNumber() >= task.getWeight().getNumber())
                .anyMatch(d -> d.getStatus() == DriverStatus.FREE);
    }

    private static synchronized void updateTask(WarehouseMessage.Task2 task) {
        allTasks.remove(getTaskById(task.getId()));
        allTasks.add(task);
    }

    public static WarehouseMessage.Task2 getTaskById(int id) {
        return allTasks.stream().filter(t -> t.getId() == id).findAny().orElse(null);
    }

    public static WarehouseMessage.Task2 getCurrentDriverTask(UserDriver userDriver) {
        return allTasks
                .stream()
                .filter(t -> t.getStatus() == WarehouseMessage.Task2.Status.STARTED)
                .filter(t -> t.getReporter().equals(userDriver.getUserName()))
                .findAny()
                .orElse(null);
    }

    //dispatcher adds task
    public static void addTask(WarehouseMessage.Task2 task) {
        WarehouseMessage.Task2 checkedTask = checkTaskBeforeAdd(task);
        allTasks.add(checkedTask);
        eventManager.notify(changeAct, checkedTask);
    }

//    public static void finishTask(WarehouseMessage.Task2 task) {
//        eventManager.notify(changeAct, task.toBuilder().setStatus(WarehouseMessage.Task2.Status.FINISHED).build());
//        allTasks.remove(task);
//        // updateTask - FINISHED
//        // update all listeners
//        // remove task from set
//    }

    public static void finishTask(UserDriver userDriver) {
//        logger.debug(userDriver.getUserName() + " finishCurrentTask");
        WarehouseMessage.Task2 t2 = TaskStorage.getCurrentDriverTask(userDriver);
        if (t2 != null) {
            eventManager.notify(changeAct, t2.toBuilder().setStatus(WarehouseMessage.Task2.Status.FINISHED).build());
            allTasks.remove(t2);
            logger.debug("Task finished: " + t2.getId() + " by " + userDriver.getUserName());
        } else logger.debug(userDriver.getUserName() + " can't finish null task");
        userDriver.setStatus(DriverStatus.FREE);
    }

    public static void driverCancelTask(UserDriver userDriver) {
        logger.trace(userDriver.getUserName() + " wants to return task");
        WarehouseMessage.Task2 task = getCurrentDriverTask(userDriver);
        if (task != null) {
            logger.trace("returning task " + task.getId());
            WarehouseMessage.Task2.Builder cancelled = task.toBuilder();
            cancelled.setStatus(WarehouseMessage.Task2.Status.WAIT);
            cancelled.setReporter(DispatcherExchanger.noDriverLogin);
            WarehouseMessage.Task2 t2 = cancelled.build();
            updateTask(t2);
            eventManager.notify(changeAct, t2);
        } else logger.trace("returning null task");
        // updateTask - WAIT
        // update all listeners
    }

    public static synchronized void startNewTask(UserDriver userDriver) {
//        logger.trace(userDriver.getUserName() + " wants to start a new task");
        WarehouseMessage.Task2 task = getTaskForDriver(userDriver);
        if (task != null) {
            logger.trace(userDriver.getUserName() + " wants to start task " + task.getId());
            WarehouseMessage.Task2.Builder startedTask = task.toBuilder();
            startedTask.setStatus(WarehouseMessage.Task2.Status.STARTED);
            startedTask.setReporter(userDriver.getUserName());
            WarehouseMessage.Task2 t2 = startedTask.build();
            updateTask(t2);
            eventManager.notify(changeAct, t2);
            userDriver.setStatus(DriverStatus.BUSY);
        } else logger.trace(userDriver.getUserName() + " can't start null task");
    }

    public static Set<WarehouseMessage.Task2> getAllTasks() {
        return allTasks;
    }

    public static void dispCancelTask(WarehouseMessage.Task2 task) {
        if (task != null && task.getReporter().equals(DispatcherExchanger.noDriverLogin)) {
            WarehouseMessage.Task2.Builder cancelled = task.toBuilder();
            cancelled.setStatus(WarehouseMessage.Task2.Status.CANCELLED);
            eventManager.notify(changeAct, cancelled.build());
            allTasks.remove(task);
        }
    }

    public static void addDriver(UserDriver userDriver) {
        drivers.add(userDriver);
    }

    public static void removeDriver(UserDriver userDriver) {
        drivers.remove(userDriver);
    }
}