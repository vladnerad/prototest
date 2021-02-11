package com.dst.server;

import com.dst.TaskStorage;
import com.dst.msg.WarehouseMessage;
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
            User sessionUser = getAuthUser(inputStream, outputStream);
            Exchanger exchanger;
            // Dispatcher
            if (sessionUser != null && sessionUser.getRole() == WarehouseMessage.LogInResponse.Role.DISPATCHER) {
                logger.info("Authorized: " + socket.getInetAddress() + " as DISPATCHER " + sessionUser.getUserName());
                exchanger = new DispatcherExchanger(sessionUser, inputStream, outputStream);
            }
            // Driver
            else if (sessionUser != null && sessionUser.getRole() == WarehouseMessage.LogInResponse.Role.DRIVER) {
                logger.info("Authorized: " + socket.getInetAddress() + " as DRIVER " + sessionUser.getUserName());
                exchanger = new DriverExchanger(sessionUser, inputStream, outputStream);
            }
            // User not authorized
            else {
                logger.info("Not authorized: " + socket.getInetAddress());
                exchanger = null;
            }
            if (exchanger != null) process(socket, exchanger);
            else socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private User getAuthUser(InputStream inputStream, OutputStream outputStream) throws IOException { // Заместитель
        User user = null;
        Any auth = Any.parseDelimitedFrom(inputStream);
        if (auth != null && auth.is(WarehouseMessage.Credentials.class)) {
            WarehouseMessage.Credentials credentials = auth.unpack(WarehouseMessage.Credentials.class);
            WarehouseMessage.LogInResponse.Builder response = WarehouseMessage.LogInResponse.newBuilder();
            response.setLogin(credentials.getLogin());
            boolean isFound = false;
            // Find user login
            for (User usr : UserStorage.getUsers()) {
                if (usr.getUserName().equals(credentials.getLogin())) {
                    isFound = true;
                    // Check password
                    if (usr.getPassword().equals(credentials.getPassword())) {
                        response.setUserInfo(usr.getUserInfo());
                        // Is driver already logged in?
                        if (TaskStorage.getDrivers().stream().anyMatch(d -> d.getUserName().equals(usr.getUserName()))) {
                            response.setLoginStatus(WarehouseMessage.LogInResponse.Status.ACCESS_DENIED);
                            user = null;
                            logger.debug("User " + usr.getUserName() + " already logged in");
                        }
                        // Success
                        else {
                            response.setLoginStatus(WarehouseMessage.LogInResponse.Status.OK);
                            user = usr;
                        }
                    }
                    // Wrong password
                    else {
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
        TaskStorage.eventManager.subscribe(changeAct, exchanger.getEventListener());
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