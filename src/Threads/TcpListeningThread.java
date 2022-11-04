package Threads;

import ServerClasses.ServerModel;
import shared.Message;

import java.io.IOException;
import java.net.Socket;

public class TcpListeningThread extends Thread{
    private final ServerModel server_model;

    public TcpListeningThread(ServerModel server_model) {
        this.server_model = server_model;
    }

    public void run() {
        try {
            while (true) {
                Socket connected_client = server_model.getTcpSocket().accept();
                server_model.newTcpConnection();

                System.out.println("New client connected, current number of connections = " + server_model.getTcpConnections());

                //read its message
                Message received_message = ServerModel.deserializeMessage(connected_client.getInputStream());

                //print its contents
                System.out.println(received_message.type_of_message);
            }
            //server_model.closeTcpSocket();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}