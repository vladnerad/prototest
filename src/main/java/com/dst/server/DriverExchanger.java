package com.dst.server;

import com.dst.TaskStorage;
import com.dst.msg.WarehouseMessage;
import com.dst.users.Role;
import com.dst.users.User;
import com.dst.users.UserDriver;
import com.google.protobuf.Any;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Collectors;

public class DriverExchanger {

    private UserDriver userDriver;
    private InputStream inputStream;
    private OutputStream outputStream;
    private WarehouseMessage.ListofTaskDisp.Builder listBuilder;

    public DriverExchanger(User user, InputStream inputStream, OutputStream outputStream) {
        if (user.getRole() == Role.DRIVER) {
            this.userDriver = (UserDriver) user;
            this.inputStream = inputStream;
            this.outputStream = outputStream;
        } else System.out.println("DriverExchanger constructor error");
    }

    // Get list of tasks for current dispatcher
    public void initListBuilder2() throws IOException {
        listBuilder = WarehouseMessage.ListofTaskDisp.newBuilder();
        listBuilder.addAllTask(TaskStorage.allTasks.stream().filter(task2 -> task2.getReporter().equals("Not assigned")).map(WarehouseMessage.Task2.Builder::build).collect(Collectors.toList()));
        if (!listBuilder.getTaskList().isEmpty()) {
            Any.pack(listBuilder.build()).writeDelimitedTo(outputStream);
        }
    }

    public void exchange() throws IOException {
        Any any = Any.parseDelimitedFrom(inputStream); // Команда
        // Create new task
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
            }
        }
    }
}
