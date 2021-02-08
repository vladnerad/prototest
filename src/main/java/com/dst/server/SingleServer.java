package com.dst.server;

import com.dst.TaskStorage;
import com.dst.TaskStorage2;
import com.dst.msg.WarehouseMessage;
import com.dst.users.Role;
import com.dst.users.User;
import com.dst.users.UserStorage;
import com.google.protobuf.Any;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import static com.dst.TaskStorage.addAfterEmpty;
import static com.dst.TaskStorage.changeAct;

public class SingleServer implements Runnable {

    private static final Logger logger = LogManager.getLogger(SingleServer.class);
    private final Socket socket;

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
                logger.info("Authorized: " + socket.getInetAddress() + " as DISPATCHER " + sessionUser.getUserName());
                exchanger = new DispatcherExchanger(sessionUser, inputStream, outputStream);
            }
            // Driver
            else if (sessionUser != null && sessionUser.getRole() == /*Role.DRIVER*/ WarehouseMessage.LogInResponse.Role.DRIVER) {
                logger.info("Authorized: " + socket.getInetAddress() + " as DRIVER " + sessionUser.getUserName());
                exchanger = new DriverExchanger(sessionUser, inputStream, outputStream);
//                TaskStorage.eventManager.subscribe(addAfterEmpty, exchanger.getEventListener());
            }
            // User not authorized
            else {
                logger.info("Not authorized: " + socket.getInetAddress());
                exchanger = null;
            }
//            TaskStorage.eventManager.printInfo();
            if (exchanger != null) process(socket, exchanger);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                        response.setLoginStatus(WarehouseMessage.LogInResponse.Status.OK);
                        response.setUserInfo(usr.getUserInfo());
                        user = usr;
                    } else {
                        logger.debug("Incorrect password");
                        response.setLoginStatus(WarehouseMessage.LogInResponse.Status.WRONG_PASS);
                    }
                    response.setUserRole(usr.getRole());
                    break;
                }
            }
            if (!isFound) {
                logger.debug("Incorrect login");
                response.setLoginStatus(WarehouseMessage.LogInResponse.Status.WRONG_LOGIN);
            }
            Any.pack(response.build()).writeDelimitedTo(outputStream);
            return user;
        }
        return null;
    }

    public void process(Socket socket, Exchanger exchanger) throws IOException {
//        TaskStorage.eventManager.subscribe(changeAct, exchanger.getEventListener());
        TaskStorage2.eventManager.subscribe(changeAct, exchanger.getEventListener());
        exchanger.initListFromCache();
        while (!socket.isClosed()) {
            try {
                exchanger.exchange();
            } catch (SocketException e) {
                socket.close();
            }
        }
        exchanger.close();
        logger.info("Connection closed " + socket.getInetAddress());
    }
}