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

import static com.dst.TaskStorage.*;

public class DispatcherExchanger implements EventListener, Exchanger {

    private UserDispatcher userDispatcher;
    private InputStream inputStream;
    private OutputStream outputStream;

    public DispatcherExchanger(User user, InputStream inputStream, OutputStream outputStream) {
        if (user.getRole() == Role.DISPATCHER) {
            this.userDispatcher = (UserDispatcher) user;
            this.inputStream = inputStream;
            this.outputStream = outputStream;
//            TaskStorage.eventManager.subscribe(changeAct, this);
        } else System.out.println("DispatcherExchanger constructor error");
    }

    @Override
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

    @Override
    public EventListener getEventListener() {
        return this;
    }

    @Override
    public void close() {
        TaskStorage.eventManager.unsubscribeAll(this);
    }

    // Get list of tasks for current dispatcher
    @Override
    public void initListFromCache() throws IOException {
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
//        System.out.println("listBuilder size: " + listBuilder.getTaskList().size());
//        System.out.println("TaskStorage size: " + TaskStorage.allTasks.size());
    }

    public void createTask(WarehouseMessage.NewTask newTask) {
        WarehouseMessage.Task2.Builder t2builder = WarehouseMessage.Task2.newBuilder();
        int id = TaskStorage.idCounter.incrementAndGet();
        t2builder.setId(id);
        t2builder.setWeight(newTask.getWeight());
        t2builder.setPriority(newTask.getPriority());
        t2builder.setTimeCreate(String.valueOf(System.currentTimeMillis()));
        t2builder.setStatus(WarehouseMessage.Task2.Status.WAIT);
        t2builder.setAssignee(userDispatcher.getUserName());
        t2builder.setReporter(noDriverLogin);
        boolean wasEmpty = TaskStorage.allTasks.isEmpty() || TaskStorage.allTasks.stream().noneMatch(t -> t.getStatus() == WarehouseMessage.Task2.Status.WAIT);
        TaskStorage.allTasks.add(t2builder);
        if (wasEmpty){
            // когда начнется новая задача, слушатели будут оповещены
            TaskStorage.allTasks.stream().filter(t -> t.getId() == id).findFirst().ifPresent(t -> TaskStorage.eventManager.notify(addAfterEmpty, t.build()));
        } else {
            TaskStorage.allTasks.stream().filter(t -> t.getId() == id).findFirst().ifPresent(t -> TaskStorage.eventManager.notify(changeAct, t.build()));
        }

        System.out.println("Task added: " + t2builder.getId() + " by " + userDispatcher.getUserName());
        System.out.println("Storage size after addition: " + TaskStorage.allTasks.size());
    }

    @Override
    public void update(String event, WarehouseMessage.Task2 task) throws IOException {
        if (!event.equals(addAfterEmpty) && task.getAssignee().equals(userDispatcher.getUserName()))
            Any.pack(task).writeDelimitedTo(outputStream);
    }
}
