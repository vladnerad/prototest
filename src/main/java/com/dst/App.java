package com.dst;

import com.dst.server.SingleServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App 
{
    static volatile int counter = 0;
    public static void main( String[] args )
    {
        ExecutorService executorService = Executors.newCachedThreadPool();
        System.out.println( "Server started!" );
        try (ServerSocket serverSocket = new ServerSocket(8189)){
            while (!serverSocket.isClosed()){
                Socket socket = serverSocket.accept();
                counter++;
                System.out.println("IP " + socket.getInetAddress() + " connected: #" + counter);
                executorService.execute(new SingleServer(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}