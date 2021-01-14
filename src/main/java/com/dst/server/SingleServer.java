package com.dst.server;

import com.dst.TaskStorage;
import com.dst.msg.WarehouseMessage;
import com.dst.users.User;
import com.dst.users.UserStorage;
import com.google.protobuf.Any;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import com.dst.users.Role;

public class SingleServer implements Runnable {

    Socket socket;

    public SingleServer(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (InputStream inputStream = socket.getInputStream();
             OutputStream outputStream = socket.getOutputStream()) {
            User sessionUser = getAuthUser(inputStream, outputStream);
            // Dispatcher
            if (sessionUser != null && sessionUser.getRole() == Role.DISPATCHER) {
                System.out.println("Authorized: " + socket.getInetAddress() + " as DISPATCHER");
                // Get list of tasks for current dispatcher
                WarehouseMessage.ListofTaskDisp.Builder listBuilder;
                // List for current dispatcher exists and not empty, get it's builder from storage
                if (TaskStorage.dispatcherTaskLists.containsKey(sessionUser.getUserName()) &&
                        !TaskStorage.dispatcherTaskLists.get(sessionUser.getUserName()).getTaskList().isEmpty()) {
                    Any.pack(TaskStorage.dispatcherTaskLists.get(sessionUser.getUserName()).build()).writeDelimitedTo(outputStream);
                }
                // Create new builder and put it in the storage
                else {
                    TaskStorage.dispatcherTaskLists.put(sessionUser.getUserName(), WarehouseMessage.ListofTaskDisp.newBuilder());
                }
                listBuilder = TaskStorage.dispatcherTaskLists.get(sessionUser.getUserName());
                // Cyclic conversation
                while (true) {
                    Any any = Any.parseDelimitedFrom(inputStream); // Команда
                    // Create new task
                    if (any.is(WarehouseMessage.NewTask.class)) {
                        WarehouseMessage.NewTask newTask = any.unpack(WarehouseMessage.NewTask.class);
                        WarehouseMessage.ListofTaskDisp.Task2.Builder t2builder = WarehouseMessage.ListofTaskDisp.Task2.newBuilder();
                        t2builder.setId(TaskStorage.idCounter.incrementAndGet());
                        t2builder.setWeight(newTask.getWeight());
                        t2builder.setPriority(newTask.getPriority());
                        t2builder.setTimeCreate(String.valueOf(System.currentTimeMillis()));
                        t2builder.setStatus(WarehouseMessage.ListofTaskDisp.Task2.Status.WAIT);
                        listBuilder.addTask(t2builder.build());
                        System.out.println("Task added: " + t2builder.getId());
                        Any.pack(listBuilder.build()).writeDelimitedTo(outputStream);
                    }
                    // Action with task
                    else if (any.is(WarehouseMessage.Action.class)) {
                        WarehouseMessage.Action action = any.unpack(WarehouseMessage.Action.class);
                        WarehouseMessage.ListofTaskDisp.Task2 task2 = listBuilder.getTaskList()
                                .stream()
                                .filter(t -> action.getId() == t.getId())
                                .findAny()
                                .orElse(null);
                        // Dispatcher removes task
                        if (action.getAct() == WarehouseMessage.Action.Act.CANCEL) {
                            listBuilder.removeTask(listBuilder.getTaskList().indexOf(task2));
                            System.out.println("Task removed: " + task2.getId());
                        }
                        // Dispatcher creates new task
                        else if (action.getAct() == WarehouseMessage.Action.Act.START) {
                            task2.toBuilder().setStatus(WarehouseMessage.ListofTaskDisp.Task2.Status.STARTED).build();
                            System.out.println("Status changed");
                        }
                        // Action doesn't exists
                        else {
                            System.out.println("Unknown action");
                        }
                        // Send updated list to dispatcher client
                        Any.pack(listBuilder.build()).writeDelimitedTo(outputStream);
                    }
                }
            }
            // Driver
            else if (sessionUser != null && sessionUser.getRole() == Role.DRIVER) {
                System.out.println("Authorized: " + socket.getInetAddress() + " as DRIVER");
            }
            // User not authorized
            else System.out.println("Not authorized: " + socket.getInetAddress());
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }

    private User getAuthUser(InputStream inputStream, OutputStream outputStream) throws IOException { // Заместитель
        User user = null;
        Any auth = Any.parseDelimitedFrom(inputStream);
        if (auth != null && auth.is(WarehouseMessage.Credentials.class)) {
            WarehouseMessage.Credentials credentials = auth.unpack(WarehouseMessage.Credentials.class);
            WarehouseMessage.Credentials.Builder response = WarehouseMessage.Credentials.newBuilder();
            response.setLogin(credentials.getLogin());
            boolean isFound = false;
            for (User usr : UserStorage.getUsers()) {
                if (usr.getUserName().equals(credentials.getLogin())) {
                    isFound = true;
                    if (usr.getPassword().equals(credentials.getPassword())) {
                        System.out.println("Logged in: " + usr.getUserName());
                        response.setPassword("SUCCESS: " + usr.getRole());
                        user = usr;
                    } else {
                        System.out.println("Incorrect password");
                        response.setPassword("WRONG PASS");
                    }
                    break;
                }
            }
            if (!isFound) {
                System.out.println("Incorrect login");
                response.setPassword("LOGIN NOT EXISTS");
            }
            Any.pack(response.build()).writeDelimitedTo(outputStream);
            return user;
        }
        return null;
    }
}