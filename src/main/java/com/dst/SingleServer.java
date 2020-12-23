package com.dst;

import com.dst.msg.WarehouseMessage;
import com.dst.users.User;
import com.dst.users.UserStorage;
import com.google.protobuf.Any;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

class SingleServer implements Runnable {

    Socket socket;

    public SingleServer(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (InputStream inputStream = socket.getInputStream();
             OutputStream outputStream = socket.getOutputStream()) {
            if (isAuthorized(inputStream, outputStream)) {
                System.out.println("Authorized: " + socket.getInetAddress());
//                List<WarehouseMessage.ListofTaskDisp.Task2> task2List = new ArrayList<>();
                WarehouseMessage.ListofTaskDisp.Builder listBuilder = WarehouseMessage.ListofTaskDisp.newBuilder();
                int taskIdcounter = 0;
                while (true) {

                    Any any = Any.parseDelimitedFrom(inputStream); // Команда
                    if (any.is(WarehouseMessage.NewTask.class)) {
                        WarehouseMessage.NewTask newTask = any.unpack(WarehouseMessage.NewTask.class);
//                        System.out.println("New task received: ");
//                        System.out.println(newTask.toString());
//                        System.out.println("1: " + newTask.getWeight());
                        WarehouseMessage.ListofTaskDisp.Task2.Builder t2builder = WarehouseMessage.ListofTaskDisp.Task2.newBuilder();
                        t2builder.setId(taskIdcounter++);
                        t2builder.setWeight(newTask.getWeight());
                        t2builder.setPriority(newTask.getPriority());
                        t2builder.setTimeCreate(String.valueOf(System.currentTimeMillis()));
                        t2builder.setStatus(WarehouseMessage.ListofTaskDisp.Task2.Status.WAIT);
//                    task2List.add(t2builder.build());
                        listBuilder.addTask(t2builder.build());
                        System.out.println("Task added: " + t2builder.getId());
//                        System.out.println(t2builder.toString());
//                        System.out.println("1: " + t2builder.getWeight());
                        Any.pack(listBuilder.build()).writeDelimitedTo(outputStream);
                    } else if (any.is(WarehouseMessage.Action.class)) {
                        WarehouseMessage.Action action = any.unpack(WarehouseMessage.Action.class);
                        WarehouseMessage.ListofTaskDisp.Task2 task2 = listBuilder.getTaskList()
                                .stream()
                                .filter(t -> action.getId() == t.getId())
                                .findAny()
                                .orElse(null);
//                    for (WarehouseMessage.ListofTaskDisp.Task2 t2: listBuilder.getTaskList()){
//                        if (t2.getId() == action.getId()){
//                            task2 = t2;
//                        }
//                    }
//                        System.out.println(listBuilder.build().getTaskList());
//                        System.out.println(listBuilder.getTaskList().toString());

                        if (action.getAct() == WarehouseMessage.Action.Act.CANCEL) {
                            listBuilder.removeTask(listBuilder.getTaskList().indexOf(task2));
                            System.out.println("Task removed: " + task2.getId());
                        } else if (action.getAct() == WarehouseMessage.Action.Act.START) {
                            task2.toBuilder().setStatus(WarehouseMessage.ListofTaskDisp.Task2.Status.STARTED).build();
                            System.out.println("Status changed");
                        } else {
                            System.out.println("Unknown action");
                        }
                        Any.pack(listBuilder.build()).writeDelimitedTo(outputStream);
                    }
                }
            } else System.out.println("Not authorized: " + socket.getInetAddress());
        } catch (
                IOException e) {
            e.printStackTrace();
        }

    }

    private boolean isAuthorized(InputStream inputStream, OutputStream outputStream) throws IOException { // Заместитель
        boolean result = false;
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
                        result = true;
                    } else {
                        System.out.println("Incorrect password");
                        response.setPassword("WRONG PASS");
                        result = false;
                    }
                    break;
                }
            }
            if (!isFound) {
                System.out.println("Incorrect login");
                response.setPassword("LOGIN NOT EXISTS");
                result = false;
            }
            Any.pack(response.build()).writeDelimitedTo(outputStream);
            return result;
        }
        return false;
    }
}