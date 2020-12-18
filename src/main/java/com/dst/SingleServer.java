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
            if (isAuthorized(inputStream, outputStream)) System.out.println("Authorized: " + socket.getInetAddress());
            else System.out.println("Not authorized: " + socket.getInetAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isAuthorized(InputStream inputStream, OutputStream outputStream) throws IOException {
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
                        System.out.println("Logged in");
                        response.setPassword("SUCCESS");
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

//            WarehouseMessage.Loader.Builder loader = WarehouseMessage.Loader.newBuilder();
//            loader.setId(1);
//            loader.setLat("123");
//            loader.setLon("345");
//            loader.setTaskId(7);
//
//            WarehouseMessage.Loader load = loader.build();

//            while(true) {
//                load.writeDelimitedTo(socket.getOutputStream());
//            }
//            System.out.println("Loader is sent");
//            Any any = Any.pack(load);
//            any.writeDelimitedTo(socket.getOutputStream());
//            System.out.println("Any is sent");


//            Scanner scanner = new Scanner(inputStream);
//            System.out.println("___________Scanner______________");
//            while (scanner.hasNext()){
//                System.out.println(scanner.next());
//            }
//            Any message = Any.parseDelimitedFrom(inputStream);
//            if (message != null) {
////                System.out.println("Any received: " + message);
//                if (message.is(WarehouseMessage.Credentials.class)) {
//                    System.out.println(message.unpack(WarehouseMessage.Credentials.class).toString());
//                } else if (message.is(WarehouseMessage.Loader.class)) {
//                    System.out.println("It's a loader");
//                    System.out.println(message.unpack(WarehouseMessage.Loader.class).toString());
//                } else if (message.is(WarehouseMessage.Task.class)) {
//                    System.out.println(message.unpack(WarehouseMessage.Task.class).toString());
//                } else {
//                    System.out.println("I don't know the type of Any");
//                }
//                socket.close();
//            } else System.out.println("Any message is null");