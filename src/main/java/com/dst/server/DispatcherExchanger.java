package com.dst.server;

import com.dst.TaskStorage;
import com.dst.msg.WarehouseMessage;
import com.dst.users.Role;
import com.dst.users.User;
import com.dst.users.UserDispatcher;
import com.google.protobuf.Any;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Collectors;

public class DispatcherExchanger {

    private UserDispatcher userDispatcher;
    private InputStream inputStream;
    private OutputStream outputStream;
    private WarehouseMessage.ListofTaskDisp.Builder listBuilder;

    public DispatcherExchanger(User user, InputStream inputStream, OutputStream outputStream) {
        if (user.getRole() == Role.DISPATCHER) {
            this.userDispatcher = (UserDispatcher) user;
            this.inputStream = inputStream;
            this.outputStream = outputStream;
        } else System.out.println("DispatcherExchanger constructor error");
    }

    public void exchange() throws IOException {
        Any any = Any.parseDelimitedFrom(inputStream); // Команда
        // Create new task
        if (any.is(WarehouseMessage.NewTask.class)) {
            WarehouseMessage.NewTask newTask = any.unpack(WarehouseMessage.NewTask.class);
            createTask(newTask);
        }
        // Action with task
        else if (any.is(WarehouseMessage.Action.class)) {
            WarehouseMessage.Action action = any.unpack(WarehouseMessage.Action.class);
            WarehouseMessage.Task2.Builder t2 = TaskStorage.allTasks
                    .stream()
                    .filter(t -> t.getId() == action.getId())
                    .findAny()
                    .orElse(null);
            if (t2 != null) {
                // Dispatcher removes task
                if (action.getAct() == WarehouseMessage.Action.Act.CANCEL) {
                    t2.setStatus(WarehouseMessage.Task2.Status.CANCELLED);
                    TaskStorage.allTasks.remove(t2);
                    System.out.println("Task removed: " + t2.getId());
                }
                // Dispatcher creates new task
                else if (action.getAct() == WarehouseMessage.Action.Act.START) {
                    t2.setStatus(WarehouseMessage.Task2.Status.STARTED).build();
                    System.out.println("Status changed");
                }
                // Action doesn't exists
                else {
                    System.out.println("Unknown action");
                }
                // Send updated list to dispatcher client
                Any.pack(t2.build()).writeDelimitedTo(outputStream);
            } else {
                System.out.println("Task isn't found");
            }
        }
    }

    // Get list of tasks for current dispatcher
    public void initListBuilder2() throws IOException {
        listBuilder = WarehouseMessage.ListofTaskDisp.newBuilder();
        listBuilder.addAllTask(
                TaskStorage.allTasks
                        .stream()
                        .filter(task2 -> task2.getAssignee().equals(userDispatcher.getUserName()))
                        .map(WarehouseMessage.Task2.Builder::build)
                        .collect(Collectors.toList())
        );
        if (!listBuilder.getTaskList().isEmpty()) {
            Any.pack(listBuilder.build()).writeDelimitedTo(outputStream);
        }
    }

    public void createTask(WarehouseMessage.NewTask newTask) throws IOException {
//        WarehouseMessage.NewTask newTask = any.unpack(WarehouseMessage.NewTask.class);
        WarehouseMessage.Task2.Builder t2builder = WarehouseMessage.Task2.newBuilder();
        t2builder.setId(TaskStorage.idCounter.incrementAndGet());
        t2builder.setWeight(newTask.getWeight());
        t2builder.setPriority(newTask.getPriority());
        t2builder.setTimeCreate(String.valueOf(System.currentTimeMillis()));
        t2builder.setStatus(WarehouseMessage.Task2.Status.WAIT);
        t2builder.setAssignee(userDispatcher.getUserName());
        t2builder.setReporter("Not assigned");
        TaskStorage.allTasks.add(t2builder);
        listBuilder.addTask(t2builder.build());
        System.out.println("Task added: " + t2builder.getId());
        Any.pack(t2builder.build()).writeDelimitedTo(outputStream);
    }
}