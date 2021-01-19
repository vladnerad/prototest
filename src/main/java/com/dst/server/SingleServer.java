package com.dst.server;

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
                DispatcherExchanger dispatcherExchanger = new DispatcherExchanger(sessionUser, inputStream, outputStream);
                dispatcherExchanger.initListBuilder2();
                while (!socket.isClosed()){
                    try {
                        dispatcherExchanger.exchange();
                    } catch (SocketException e) {
//                        e.printStackTrace();
                        socket.close();
                    }
                }
                System.out.println("Connection closed " + socket.getInetAddress());
            }
            // Driver
            else if (sessionUser != null && sessionUser.getRole() == Role.DRIVER) {
                System.out.println("Authorized: " + socket.getInetAddress() + " as DRIVER");
                DriverExchanger driverExchanger = new DriverExchanger(sessionUser, inputStream, outputStream);
                driverExchanger.initListBuilder2();
                while (!socket.isClosed()){
                    try {
                        driverExchanger.exchange();
                    } catch (SocketException e) {
//                        e.printStackTrace();
                        socket.close();
                    }
                }
                System.out.println("Connection closed " + socket.getInetAddress());
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