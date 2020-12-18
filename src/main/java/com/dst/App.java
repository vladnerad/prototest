package com.dst;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class App 
{
    static volatile int counter = 0;
    public static void main( String[] args )
    {
        System.out.println( "Server started!" );
        try (ServerSocket serverSocket = new ServerSocket(8189)){
            while (true){
                Socket socket = serverSocket.accept();
                counter++;
                System.out.println("IP " + socket.getInetAddress() + " connected: #" + counter);
                new Thread(new SingleServer(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}