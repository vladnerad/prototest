package com.dst.server;

import com.dst.TaskStorage;
import com.dst.msg.WarehouseMessage;
import com.dst.users.Role;
import com.dst.users.User;
import com.dst.users.UserStorage;
import com.google.protobuf.Any;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import static com.dst.TaskStorage.addAfterEmpty;
import static com.dst.TaskStorage.changeAct;

public class SingleServer implements Runnable {

    Socket socket;

    public SingleServer(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (InputStream inputStream = socket.getInputStream();
             OutputStream outputStream = socket.getOutputStream()) {
            User sessionUser = getAuthUser2(inputStream, outputStream);
            Exchanger exchanger;
            // Dispatcher
            if (sessionUser != null && sessionUser.getRole() == /*Role.DISPATCHER*/ WarehouseMessage.LogInResponse.Role.DISPATCHER) {
                System.out.println("Authorized: " + socket.getInetAddress() + " as DISPATCHER " + sessionUser.getUserName());
                exchanger = new DispatcherExchanger(sessionUser, inputStream, outputStream);
//                TaskStorage.eventManager.subscribe(changeAct, exchanger.getEventListener());
            }
            // Driver
            else if (sessionUser != null && sessionUser.getRole() == /*Role.DRIVER*/ WarehouseMessage.LogInResponse.Role.DRIVER) {
                System.out.println("Authorized: " + socket.getInetAddress() + " as DRIVER " + sessionUser.getUserName());
                exchanger = new DriverExchanger(sessionUser, inputStream, outputStream);
                TaskStorage.eventManager.subscribe(addAfterEmpty, exchanger.getEventListener());
//                TaskStorage.eventManager.subscribe(changeAct, exchanger.getEventListener());
            }
            // User not authorized
            else {
                System.out.println("Not authorized: " + socket.getInetAddress());
                exchanger = null;
            }
//            TaskStorage.eventManager.printInfo();
            if (exchanger != null) process(socket, exchanger);
        } catch (IOException e) {
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
//                        System.out.println("Logged in: " + usr.getUserName());
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

    private User getAuthUser2(InputStream inputStream, OutputStream outputStream) throws IOException { // Заместитель
        User user = null;
        Any auth = Any.parseDelimitedFrom(inputStream);
        if (auth != null && auth.is(WarehouseMessage.Credentials.class)) {
            WarehouseMessage.Credentials credentials = auth.unpack(WarehouseMessage.Credentials.class);
            WarehouseMessage.LogInResponse.Builder response = WarehouseMessage.LogInResponse.newBuilder();
            response.setLogin(credentials.getLogin());
            boolean isFound = false;
            for (User usr : UserStorage.getUsers()) {
                if (usr.getUserName().equals(credentials.getLogin())) {
                    isFound = true;
                    if (usr.getPassword().equals(credentials.getPassword())) {
//                        System.out.println("Logged in: " + usr.getUserName());
                        response.setLoginStatus(WarehouseMessage.LogInResponse.Status.OK);
                        response.setUserInfo(usr.getUserInfo());
                        user = usr;
                    } else {
                        System.out.println("Incorrect password");
                        response.setLoginStatus(WarehouseMessage.LogInResponse.Status.WRONG_PASS);
                    }
                    response.setUserRole(usr.getRole());
                    break;
                }
            }
            if (!isFound) {
                System.out.println("Incorrect login");
                response.setLoginStatus(WarehouseMessage.LogInResponse.Status.WRONG_LOGIN);
            }
            Any.pack(response.build()).writeDelimitedTo(outputStream);
            return user;
        }
        return null;
    }

    public void process(Socket socket, Exchanger exchanger) throws IOException {
        TaskStorage.eventManager.subscribe(changeAct, exchanger.getEventListener());
//        TaskStorage.eventManager.printInfo();
        exchanger.initListFromCache();
        while (!socket.isClosed()) {
            try {
                exchanger.exchange();
            } catch (SocketException e) {
                socket.close();
            }
        }
//        TaskStorage.eventManager.unsubscribeAll(exchanger.getEventListener());
        exchanger.close();
        System.out.println("Connection closed " + socket.getInetAddress());
        TaskStorage.eventManager.printInfo();
    }
}