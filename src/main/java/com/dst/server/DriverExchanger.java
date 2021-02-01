package com.dst.server;

import com.dst.TaskStorage;
import com.dst.msg.WarehouseMessage;
import com.dst.observer.EventListener;
import com.dst.users.DriverStatus;
import com.dst.users.Role;
import com.dst.users.User;
import com.dst.users.UserDriver;
import com.google.protobuf.Any;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.stream.Collectors;

import static com.dst.TaskStorage.*;

public class DriverExchanger implements EventListener, Exchanger {

    private UserDriver userDriver;
    private InputStream inputStream;
    private OutputStream outputStream;

    public DriverExchanger(User user, InputStream inputStream, OutputStream outputStream) {
        if (user.getRole() == /*Role.DRIVER*/ WarehouseMessage.LogInResponse.Role.DRIVER) {
            this.userDriver = (UserDriver) user;
            this.inputStream = inputStream;
            this.outputStream = outputStream;
            userDriver.setStatus(DriverStatus.FREE);
        } else System.out.println("DriverExchanger constructor error");
    }

    // Get list of tasks for current dispatcher
    @Override
    public void initListFromCache() throws IOException {
        WarehouseMessage.ListofTaskDisp.Builder listBuilder = WarehouseMessage.ListofTaskDisp.newBuilder();
        listBuilder.addAllTask(TaskStorage.allTasks.stream().filter(task2 -> task2.getReporter().equals("Not assigned")).map(WarehouseMessage.Task2.Builder::build).collect(Collectors.toList()));
        if (!listBuilder.getTaskList().isEmpty()) {
            Any.pack(listBuilder.build()).writeDelimitedTo(outputStream);
        }
        System.out.println(userDriver.getUserName() + " has status " + userDriver.getStatus());
    }

    @Override
    public void exchange() throws IOException {
        if (userDriver.getStatus() == DriverStatus.FREE) {
            startTaskFromPool();
        }
//        else if (userDriver.getStatus() == DriverStatus.BROKEN){
//
//        }
        Any any = Any.parseDelimitedFrom(inputStream); // Команда
        // Create new task
        if (any == null) {
            returnTaskToList();
            throw new SocketException("Any in driver exchanger is null");
        } else {
            if (any.is(WarehouseMessage.Action.class)) {
                WarehouseMessage.Action action = any.unpack(WarehouseMessage.Action.class);
                if (action.getAct() == WarehouseMessage.Action.Act.FINISH) {
                    finishCurrentTask();
                }
            } else if (any.is(WarehouseMessage.UserStatus.class)) {
                WarehouseMessage.UserStatus status = any.unpack(WarehouseMessage.UserStatus.class);
                if (status.getStatus() == WarehouseMessage.UserStatus.Status.READY
                        && userDriver.getStatus() == DriverStatus.BROKEN) {
                    userDriver.setStatus(DriverStatus.FREE);
                    Any.pack(status).writeDelimitedTo(outputStream);
                    System.out.println("Driver: " + userDriver.getUserName() + " has repaired");
                } else if (status.getStatus() == WarehouseMessage.UserStatus.Status.BROKEN
                        && userDriver.getStatus() != DriverStatus.BROKEN) {
                    if (userDriver.getStatus() == DriverStatus.BUSY) returnTaskToList();
                    userDriver.setStatus(DriverStatus.BROKEN);
                    Any.pack(status).writeDelimitedTo(outputStream);
                    System.out.println("Driver: " + userDriver.getUserName() + " has broken");
                }
            }
        }
    }

    @Override
    public EventListener getEventListener() {
        return this;
    }

    @Override
    public void close() {
        if (userDriver.getStatus() == DriverStatus.BUSY) returnTaskToList();
        TaskStorage.eventManager.unsubscribeAll(this);
    }

    public WarehouseMessage.Task2.Builder getNextTask() {
        return TaskStorage.allTasks
                .stream()
                .filter(t -> t.getStatus() == WarehouseMessage.Task2.Status.WAIT)
                .findFirst()
                .orElse(null);
    }

    public WarehouseMessage.Task2 startTaskFromPool() {
        WarehouseMessage.Task2.Builder task = getNextTask();
        if (task != null && userDriver.getStatus() == DriverStatus.FREE) {
            userDriver.setStatus(DriverStatus.BUSY);
            task.setStatus(WarehouseMessage.Task2.Status.STARTED).setReporter(userDriver.getUserName());
            System.out.println("Task " + task.getId() + " started by: " + userDriver.getUserName());
            TaskStorage.eventManager.notify(changeAct, task.build());
            return task.build();
        } else {
            System.out.println("New task in pool for " + userDriver.getUserName());
            return null;
        }
    }

    public void startFreshTask(WarehouseMessage.Task2 task) {
        boolean isTaskWait = false;
        WarehouseMessage.Task2.Builder checkTask = TaskStorage.allTasks
                .stream()
                .filter(t -> t.getId() == task.getId())
                .findFirst()
                .orElse(null);
        if (checkTask != null) isTaskWait = checkTask.getStatus() == WarehouseMessage.Task2.Status.WAIT;

        if (task != null && userDriver.getStatus() == DriverStatus.FREE && isTaskWait) {
            userDriver.setStatus(DriverStatus.BUSY);
            checkTask.setStatus(WarehouseMessage.Task2.Status.STARTED).setReporter(userDriver.getUserName());
            System.out.println("Task " + task.getId() + " started by: " + userDriver.getUserName());
            TaskStorage.eventManager.notify(changeAct, checkTask.build());
        } else {
            System.out.println("Fresh task not started by " + userDriver.getUserName());
        }
    }

    public void finishCurrentTask() {
        userDriver.setStatus(DriverStatus.FREE);
        WarehouseMessage.Task2.Builder t2 = TaskStorage.allTasks
                .stream()
                .filter(t -> t.getStatus() == WarehouseMessage.Task2.Status.STARTED)
                .filter(t -> t.getReporter().equals(userDriver.getUserName()))
                .findAny()
                .orElse(null);
        if (t2 != null) {
            t2.setStatus(WarehouseMessage.Task2.Status.FINISHED);
            TaskStorage.eventManager.notify(changeAct, t2.build());
            TaskStorage.allTasks.remove(t2);
            System.out.println("Task finished: " + t2.getId());
        } else System.out.println("Task not founded");
    }

    public void returnTaskToList() {
        if (userDriver.getStatus() == DriverStatus.BUSY) {
            WarehouseMessage.Task2.Builder task = TaskStorage.allTasks
                    .stream()
                    .filter(t -> t.getStatus() == WarehouseMessage.Task2.Status.STARTED)
                    .filter(t -> t.getReporter().equals(userDriver.getUserName()))
                    .findAny()
                    .orElse(null);
            if (task != null) {
                task.setStatus(WarehouseMessage.Task2.Status.WAIT).setReporter(noDriverLogin);
                TaskStorage.eventManager.notify(changeAct, task.build());
                System.out.println("Task " + task.getId() + " has been returned by: " + userDriver.getUserName());
            } else System.out.println("No tasks available");
        }
    }

    @Override
    public void update(String event, WarehouseMessage.Task2 task) throws IOException {
        if (event.equals(addAfterEmpty)){
            startFreshTask(task);
        }
        else Any.pack(task).writeDelimitedTo(outputStream);
    }
}