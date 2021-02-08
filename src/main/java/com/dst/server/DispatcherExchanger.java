package com.dst.server;

import com.dst.TaskStorage;
import com.dst.msg.WarehouseMessage;
import com.dst.observer.EventListener;
import com.dst.users.User;
import com.dst.users.UserDispatcher;
import com.google.protobuf.Any;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class DispatcherExchanger implements EventListener, Exchanger {

    private static final Logger logger = LogManager.getLogger(DispatcherExchanger.class);
    private UserDispatcher userDispatcher;
    private InputStream inputStream;
    private OutputStream outputStream;
    // Id for task
    public static volatile AtomicInteger idCounter = new AtomicInteger(0);
    public static final String noDriverLogin = "Not assigned";

    public DispatcherExchanger(User user, InputStream inputStream, OutputStream outputStream) {
        if (user.getRole() == WarehouseMessage.LogInResponse.Role.DISPATCHER) {
            this.userDispatcher = (UserDispatcher) user;
            this.inputStream = inputStream;
            this.outputStream = outputStream;
        } else logger.trace("DispatcherExchanger constructor error");
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
                WarehouseMessage.Task2 t2 = TaskStorage.getTaskById(action.getId());
                if (t2 != null) {
                    // Dispatcher removes task
                    if (action.getAct() == WarehouseMessage.Action.Act.CANCEL) {
                        TaskStorage.dispCancelTask(t2);
                        logger.debug("Task removed: " + t2.getId());
                    }
                    // Action doesn't exists
                    else {
                        logger.debug("Unknown action");
                    }
                } else {
                    logger.debug("Task isn't found");
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
                TaskStorage.getAllTasks()
                        .stream()
                        .filter(task2 -> task2.getAssignee().equals(userDispatcher.getUserName()))
//                        .map(WarehouseMessage.Task2.Builder::build)
                        .collect(Collectors.toList())
        );
        if (!listBuilder.getTaskList().isEmpty()) {
            Any.pack(listBuilder.build()).writeDelimitedTo(outputStream);
        }
    }

    public void createTask(WarehouseMessage.NewTask newTask) {
        WarehouseMessage.Task2.Builder t2builder = WarehouseMessage.Task2.newBuilder();
        int id = idCounter.incrementAndGet();
        t2builder.setId(id);
        t2builder.setWeight(newTask.getWeight());
        t2builder.setPriority(newTask.getPriority());
        t2builder.setTimeCreate(String.valueOf(System.currentTimeMillis()));
        t2builder.setStatus(WarehouseMessage.Task2.Status.WAIT);
        t2builder.setAssignee(userDispatcher.getUserName());
        t2builder.setReporter(noDriverLogin);
        TaskStorage.addTask(t2builder.build());
        logger.debug("Task added: " + t2builder.getId() + " by " + userDispatcher.getUserName());
        logger.debug("Storage size after createTask(): " + TaskStorage.getAllTasks().size());
    }

    @Override
    public void update(String event, WarehouseMessage.Task2 task) throws IOException {
        if (task.getAssignee().equals(userDispatcher.getUserName()))
            Any.pack(task).writeDelimitedTo(outputStream);
    }
}
