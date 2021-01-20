package com.dst.server;

import com.dst.TaskStorage;
import com.dst.msg.WarehouseMessage;
import com.dst.observer.EventListener;
import com.dst.users.Role;
import com.dst.users.User;
import com.dst.users.UserDispatcher;
import com.google.protobuf.Any;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.stream.Collectors;

import static com.dst.TaskStorage.changeAct;
import static com.dst.TaskStorage.noDriverLogin;

public class DispatcherExchanger implements EventListener {

    private UserDispatcher userDispatcher;
    private InputStream inputStream;
    private OutputStream outputStream;

    public DispatcherExchanger(User user, InputStream inputStream, OutputStream outputStream) {
        if (user.getRole() == Role.DISPATCHER) {
            this.userDispatcher = (UserDispatcher) user;
            this.inputStream = inputStream;
            this.outputStream = outputStream;
            TaskStorage.eventManager.subscribe(changeAct, this);
        } else System.out.println("DispatcherExchanger constructor error");
    }

    public void exchange() throws IOException {
        Any any = Any.parseDelimitedFrom(inputStream); // Команда
        if (any == null) {
            throw new SocketException("Any in dispatcher exchanger is null");
        } else {
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
                        TaskStorage.eventManager.notify(changeAct, t2.build());
                        TaskStorage.allTasks.remove(t2);
                        System.out.println("Task removed: " + t2.getId());
                    }
                    // Dispatcher creates new task
//                    else if (action.getAct() == WarehouseMessage.Action.Act.START) {
//                        t2.setStatus(WarehouseMessage.Task2.Status.STARTED).build();
//                        System.out.println("Status changed");
//                    }
                    // Action doesn't exists
                    else {
                        System.out.println("Unknown action");
                    }
                    // Send updated list to dispatcher client
//                    Any.pack(t2.build()).writeDelimitedTo(outputStream);
                } else {
                    System.out.println("Task isn't found");
                }
            }
        }
    }

    // Get list of tasks for current dispatcher
    public void initListBuilder2() throws IOException {
        WarehouseMessage.ListofTaskDisp.Builder listBuilder = WarehouseMessage.ListofTaskDisp.newBuilder();
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
        System.out.println("listBuilder size: " + listBuilder.getTaskList().size());
        System.out.println("TaskStorage size: " + TaskStorage.allTasks.size());
    }

    public void createTask(WarehouseMessage.NewTask newTask) throws IOException {
        WarehouseMessage.Task2.Builder t2builder = WarehouseMessage.Task2.newBuilder();
        t2builder.setId(TaskStorage.idCounter.incrementAndGet());
        t2builder.setWeight(newTask.getWeight());
        t2builder.setPriority(newTask.getPriority());
        t2builder.setTimeCreate(String.valueOf(System.currentTimeMillis()));
        t2builder.setStatus(WarehouseMessage.Task2.Status.WAIT);
        t2builder.setAssignee(userDispatcher.getUserName());
        t2builder.setReporter(noDriverLogin);
        TaskStorage.allTasks.add(t2builder);
//        listBuilder.addTask(t2builder.build());
        TaskStorage.eventManager.notify(changeAct, t2builder.build());
        System.out.println("Task added: " + t2builder.getId());
        System.out.println("Storage size after addition: " + TaskStorage.allTasks.size());
//        Any.pack(t2builder.build()).writeDelimitedTo(outputStream);
    }

    @Override
    public void update(String event, WarehouseMessage.Task2 task) throws IOException {
        Any.pack(task).writeDelimitedTo(outputStream);
    }
}
