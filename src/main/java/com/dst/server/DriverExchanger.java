package com.dst.server;

import com.dst.TaskStorage;
import com.dst.msg.WarehouseMessage;
import com.dst.observer.EventListener;
import com.dst.users.DriverStatus;
import com.dst.users.User;
import com.dst.users.UserDriver;
import com.google.protobuf.Any;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.stream.Collectors;

public class DriverExchanger implements EventListener, Exchanger {

    private static final Logger logger = LogManager.getLogger(DriverExchanger.class);
    private volatile UserDriver userDriver;
    private InputStream inputStream;
    private OutputStream outputStream;

    public DriverExchanger(User user, InputStream inputStream, OutputStream outputStream) {
        if (user.getRole() == WarehouseMessage.LogInResponse.Role.DRIVER) {
            this.userDriver = (UserDriver) user;
            this.inputStream = inputStream;
            this.outputStream = outputStream;
            userDriver.setStatus(DriverStatus.FREE);
            TaskStorage.addDriver(userDriver);
        } else
            logger.trace("DriverExchanger constructor error"); /*System.out.println("DriverExchanger constructor error");*/
    }

    // Get list of tasks for current dispatcher
    @Override
    public void initListFromCache() throws IOException {
        WarehouseMessage.ListofTaskDisp.Builder listBuilder = WarehouseMessage.ListofTaskDisp.newBuilder();
        listBuilder.addAllTask(
                TaskStorage.getAllTasks()
                        .stream()
                        .filter(task2 -> task2.getReporter().equals("Not assigned"))
                        .filter(t -> t.getWeight().getNumber() <= userDriver.getWeightClass().getNumber())
//                        .map(WarehouseMessage.Task2.Builder::build)
                        .collect(Collectors.toList()));
        if (!listBuilder.getTaskList().isEmpty()) {
            Any.pack(listBuilder.build()).writeDelimitedTo(outputStream);
        }
    }

    @Override
    public void exchange() throws IOException {
        if (userDriver.getStatus() == DriverStatus.FREE) {
            logger.debug(userDriver.getUserName() + " is " + userDriver.getStatus());
            startTaskFromPool();
        }

        Any any = Any.parseDelimitedFrom(inputStream); // Команда
        // Create new task
        if (any == null) {
            TaskStorage.driverCancelTask(userDriver);
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
                    TaskStorage.startNewTask(userDriver);
                    logger.debug("Driver: " + userDriver.getUserName() + " has repaired");
                } else if (status.getStatus() == WarehouseMessage.UserStatus.Status.BROKEN
                        && userDriver.getStatus() != DriverStatus.BROKEN) {
                    if (userDriver.getStatus() == DriverStatus.BUSY) TaskStorage.driverCancelTask(userDriver);
                    userDriver.setStatus(DriverStatus.BROKEN);
                    Any.pack(status).writeDelimitedTo(outputStream);
                    logger.debug("Driver: " + userDriver.getUserName() + " has broken");
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
        logger.trace("Closing connection for: " + userDriver.getUserName() + " has status " + userDriver.getStatus());
        if (userDriver.getStatus() == DriverStatus.BUSY) TaskStorage.driverCancelTask(userDriver);
        TaskStorage.eventManager.unsubscribeAll(this);
        TaskStorage.removeDriver(userDriver);
    }

    public void startTaskFromPool() {
        TaskStorage.startNewTask(userDriver);
    }

    public void finishCurrentTask() {
        logger.debug(userDriver.getUserName() + " finishCurrentTask");
        WarehouseMessage.Task2 t2 = TaskStorage.getCurrentDriverTask(userDriver);
        if (t2 != null) {
            TaskStorage.finishTask(t2);
            logger.debug("Task finished: " + t2.getId());
        } else logger.debug("Task not founded");
        userDriver.setStatus(DriverStatus.FREE);
    }

//    public void returnTaskToList() {
//        TaskStorage.driverCancelTask(userDriver);
//    }

    @Override
    public void update(String event, WarehouseMessage.Task2 task) throws IOException {
        if (task.getWeight().getNumber() <= userDriver.getWeightClass().getNumber()) {
            Any.pack(task).writeDelimitedTo(outputStream);
        }
    }
}