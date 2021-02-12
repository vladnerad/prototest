package com.dst;

import com.dst.server.SingleServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App 
{
    private static final Logger logger = LogManager.getLogger(App.class);
    static volatile int counter = 0;
    public static void main( String[] args )
    {
        ExecutorService executorService = Executors.newCachedThreadPool();
        try (ServerSocket serverSocket = new ServerSocket(8185)){
            logger.info("Server started at port: " + serverSocket.getLocalPort());
            while (!serverSocket.isClosed()){
                Socket socket = serverSocket.accept();
                socket.setSoTimeout(10000);
                counter++;
                logger.info("IP " + socket.getInetAddress() + " connected: #" + counter);
                executorService.execute(new SingleServer(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}