package com.dst.server;

import com.dst.TaskStorage;
import com.dst.msg.WarehouseMessage;
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

import static com.dst.TaskStorage.noDriverLogin;

public class DriverExchanger {

    private UserDriver userDriver;
    private InputStream inputStream;
    private OutputStream outputStream;

    public DriverExchanger(User user, InputStream inputStream, OutputStream outputStream) {
        if (user.getRole() == Role.DRIVER) {
            this.userDriver = (UserDriver) user;
            this.inputStream = inputStream;
            this.outputStream = outputStream;
            userDriver.setStatus(DriverStatus.FREE);
        } else System.out.println("DriverExchanger constructor error");
    }

    // Get list of tasks for current dispatcher
    public void initListBuilder2() throws IOException {
        WarehouseMessage.ListofTaskDisp.Builder listBuilder = WarehouseMessage.ListofTaskDisp.newBuilder();
        listBuilder.addAllTask(TaskStorage.allTasks.stream().filter(task2 -> task2.getReporter().equals("Not assigned")).map(WarehouseMessage.Task2.Builder::build).collect(Collectors.toList()));
        if (!listBuilder.getTaskList().isEmpty()) {
            Any.pack(listBuilder.build()).writeDelimitedTo(outputStream);
        }
        System.out.println(userDriver.getUserName() + " has status " + userDriver.getStatus());
    }

    public void exchange() throws IOException {
        if (userDriver.getStatus() == DriverStatus.FREE) startNewTask();
        Any any = Any.parseDelimitedFrom(inputStream); // Команда
        // Create new task
        if (any == null) {
            returnTaskToList();
            throw new SocketException("Any in driver exchanger is null");
        } else {
            if (any.is(WarehouseMessage.Action.class)) {
                WarehouseMessage.Action action = any.unpack(WarehouseMessage.Action.class);
                if (action.getAct() == WarehouseMessage.Action.Act.START) {
                    WarehouseMessage.Task2.Builder t2 = TaskStorage.allTasks.stream().filter(t -> t.getId() == action.getId()).findFirst().orElse(null);
                    if (t2 != null) {
                        t2
                                .setStatus(WarehouseMessage.Task2.Status.STARTED)
                                .setReporter(userDriver.getUserName());
                        Any.pack(t2.build()).writeDelimitedTo(outputStream);
                    }
                    System.out.println("Status changed");
                } else if (action.getAct() == WarehouseMessage.Action.Act.FINISH) {
                    finishCurrentTask();
                }
            }
        }
    }

    public WarehouseMessage.Task2.Builder getNextTask() {
        return TaskStorage.allTasks
                .stream()
                .filter(t -> t.getStatus() == WarehouseMessage.Task2.Status.WAIT)
                .findFirst()
                .orElse(null);
    }

    public void startNewTask() throws IOException {
        WarehouseMessage.Task2.Builder task = getNextTask();
        if (task != null) {
            userDriver.setStatus(DriverStatus.BUSY);
            Any.pack(task/*getNextTask()*/
                    .setStatus(WarehouseMessage.Task2.Status.STARTED)
                    .setReporter(userDriver.getUserName())
                    .build()).writeDelimitedTo(outputStream);
            System.out.println("New task started by: " + userDriver.getUserName());
        } else System.out.println("No tasks available");
    }

    public void finishCurrentTask() throws IOException {
        userDriver.setStatus(DriverStatus.FREE);
        WarehouseMessage.Task2.Builder t2 = TaskStorage.allTasks
                .stream()
                .filter(t -> t.getStatus() == WarehouseMessage.Task2.Status.STARTED)
                .filter(t -> t.getReporter().equals(userDriver.getUserName()))
                .findAny()
                .orElse(null);
        if (t2 != null) {
            Any.pack(t2
                    .setStatus(WarehouseMessage.Task2.Status.FINISHED)
                    .build()).writeDelimitedTo(outputStream);
            TaskStorage.allTasks.remove(t2);
            System.out.println("Task finished: " + t2.getId());
        } else System.out.println("Task not founded");
    }

    public void returnTaskToList () throws IOException {
        if (userDriver.getStatus() == DriverStatus.BUSY){
            WarehouseMessage.Task2.Builder task = TaskStorage.allTasks
                    .stream()
                    .filter(t -> t.getStatus() == WarehouseMessage.Task2.Status.STARTED)
                    .filter(t -> t.getReporter().equals(userDriver.getUserName()))
                    .findAny()
                    .orElse(null);
            if (task != null) {
                Any.pack(task
                        .setStatus(WarehouseMessage.Task2.Status.WAIT)
                        .setReporter(noDriverLogin)
                        .build()).writeDelimitedTo(outputStream);
                System.out.println("Task " + task.getId() + " has been returned by: " + userDriver.getUserName());
            } else System.out.println("No tasks available");
        }
    }
}