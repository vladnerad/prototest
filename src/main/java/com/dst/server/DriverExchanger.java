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
import java.net.Socket;
import java.net.SocketException;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DriverExchanger implements EventListener, Exchanger {

    private static final Logger logger = LogManager.getLogger(DriverExchanger.class);
    private volatile UserDriver userDriver;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Socket socket;
    private int checkSum;
    private TimerTask timerTask;
    private ScheduledExecutorService executorService;

    public DriverExchanger(User user, Socket socket) {
        if (user.getRole() == WarehouseMessage.LogInResponse.Role.DRIVER) {
            this.userDriver = (UserDriver) user;
            this.socket = socket;
            try {
                this.inputStream = socket.getInputStream();
                this.outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            timerTask = new TimerTask() {
                @Override
                public void run() {
                    try {
                        sendCheckMsg();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            executorService = Executors.newSingleThreadScheduledExecutor();
            startCheckingConn();
            userDriver.setStatus(DriverStatus.FREE);
            TaskStorage.addDriver(userDriver);
        } else
            logger.trace("DriverExchanger constructor error");
    }

    // Get list of tasks for current driver, without starting any task
    @Override
    public void initListFromCache() throws IOException {
        WarehouseMessage.ListofTaskDisp.Builder listBuilder = WarehouseMessage.ListofTaskDisp.newBuilder();
        listBuilder.addAllTask(
                TaskStorage.getAllTasks()
                        .stream()
                        .filter(task2 -> task2.getReporter().equals("Not assigned"))
                        .filter(t -> t.getWeight().getNumber() <= userDriver.getWeightClass().getNumber())
                        .collect(Collectors.toList()));
        if (!listBuilder.getTaskList().isEmpty()) {
            Any.pack(listBuilder.build()).writeDelimitedTo(outputStream);
        }
    }

    // Cyclic process
    @Override
    public void exchange() throws IOException {
        if (userDriver.getStatus() == DriverStatus.FREE) {
            logger.debug(userDriver.getUserName() + " is " + userDriver.getStatus());
            TaskStorage.startNewTask(userDriver);
        }
        Any any = Any.parseDelimitedFrom(inputStream); // Команда
        // Error - socket closed
        if (any == null) {
//            TaskStorage.driverCancelTask(userDriver);
//            throw new SocketException("Any in driver exchanger is null");
            close();
        }
        // Normal socket work
        else {
            // Driver wants to do something with his task
            if (any.is(WarehouseMessage.Action.class)) {
                WarehouseMessage.Action action = any.unpack(WarehouseMessage.Action.class);
                // Driver can only finish task
                if (action.getAct() == WarehouseMessage.Action.Act.FINISH) {
                    TaskStorage.finishTask(userDriver);
                }
            }
            // Driver wants to change his status
            else if (any.is(WarehouseMessage.UserStatus.class)) {
                WarehouseMessage.UserStatus status = any.unpack(WarehouseMessage.UserStatus.class);
                // Driver has been repaired
                if (status.getStatus() == WarehouseMessage.UserStatus.Status.READY
                        && userDriver.getStatus() == DriverStatus.BROKEN) {
                    driverRepaired(status);
                }
                // Driver has been broken
                else if (status.getStatus() == WarehouseMessage.UserStatus.Status.BROKEN
                        && userDriver.getStatus() != DriverStatus.BROKEN) {
                    driverBroken(status);
                }
            } else if (any.is(WarehouseMessage.ConnectionCheck.class)) {
                WarehouseMessage.ConnectionCheck resp = any.unpack(WarehouseMessage.ConnectionCheck.class);
                if (resp.getCheckMsg() != checkSum) {
                    logger.warn(userDriver.getUserName() + " lost connection");
                    stopCheckingConn();
                    close();
                } else logger.trace(userDriver.getUserName() + " Checksum correct");
            }
        }
    }

    private void driverRepaired(WarehouseMessage.UserStatus echo) throws IOException {
        userDriver.setStatus(DriverStatus.FREE);
        Any.pack(echo).writeDelimitedTo(outputStream);
        TaskStorage.startNewTask(userDriver);
        logger.debug("Driver: " + userDriver.getUserName() + " has repaired");
    }

    private void driverBroken(WarehouseMessage.UserStatus status) throws IOException {
        if (userDriver.getStatus() == DriverStatus.BUSY) TaskStorage.driverCancelTask(userDriver);
        userDriver.setStatus(DriverStatus.BROKEN);
        Any.pack(status).writeDelimitedTo(outputStream);
        logger.debug("Driver: " + userDriver.getUserName() + " has broken");
    }

    @Override
    public EventListener getEventListener() {
        return this;
    }

    @Override
    public void close() throws IOException {
        logger.trace("Closing connection for: " + userDriver.getUserName() + " has status " + userDriver.getStatus());
        if (userDriver.getStatus() == DriverStatus.BUSY) TaskStorage.driverCancelTask(userDriver);
        TaskStorage.eventManager.unsubscribeAll(this);
        TaskStorage.removeDriver(userDriver);
        stopCheckingConn();
        socket.close();
        logger.trace(userDriver.getUserName() + " dispatcher service closed");
    }

    @Override
    public void update(String event, WarehouseMessage.Task2 task) throws IOException {
        if (task.getWeight().getNumber() <= userDriver.getWeightClass().getNumber()) {
            Any.pack(task).writeDelimitedTo(outputStream);
            if (userDriver.getStatus() == DriverStatus.FREE) {
                logger.debug(userDriver.getUserName() + " on update has " + userDriver.getStatus());
                TaskStorage.startNewTask(userDriver);
            }
        }
    }

    @Override
    public void sendCheckMsg() throws IOException {
        checkSum = (int) (Math.random() * 1000);
        WarehouseMessage.ConnectionCheck.Builder msg = WarehouseMessage.ConnectionCheck.newBuilder();
        msg.setCheckMsg(checkSum);
        logger.trace("Connection check. Checksum = " + msg.getCheckMsg());
        try {
            Any.pack(msg.build()).writeDelimitedTo(outputStream);
        } catch (SocketException e) {
            e.printStackTrace();
            stopCheckingConn();
            close();
        }
    }

    @Override
    public void startCheckingConn() {
        int delay = 60;
        executorService.scheduleWithFixedDelay(timerTask, delay, delay, TimeUnit.SECONDS);
    }

    @Override
    public void stopCheckingConn() {
        executorService.shutdown();
    }
}