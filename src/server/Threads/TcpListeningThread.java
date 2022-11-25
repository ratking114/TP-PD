package server.Threads;

import server.ServerClasses.ServerModel;
import shared.ClientActivity;
import shared.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.channels.SocketChannel;

public class TcpListeningThread extends Thread{
    private final ServerModel server_model;

    public TcpListeningThread(ServerModel server_model) {
        this.server_model = server_model;
    }

    public void run() {
        try {
            while (true) {
                SocketChannel connected_client = server_model.getTcpSocket().accept();
                System.out.println("Got a connection from someone!");

                //read its message
                Message received_message = ServerModel.extractMessageFromChannel(connected_client);
                if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.HELLO_I_AM_SERVER){
                    //print a status message
                    System.out.println("Got a request from a Server!");
                    new DatabaseSenderThread(server_model, connected_client).start();
                }
                //it is a client launch a Thread for it
                else{
                    System.out.println("New client connected, current number of connections = " + server_model.getTcpConnections());
                    System.out.println("Creating a Thread to Service this Client");
                    server_model.newTcpConnection();
                    ServiceClientThread new_thread = new ServiceClientThread(server_model, connected_client, (ClientActivity) received_message.attachment);
                    this.server_model.addServiceClientThread(new_thread);
                    this.server_model.sendHeartBeat();
                    new_thread.start();


                    //send a message to all the clients to tell that our workload changed
                    server_model.sendUpdatedServerListToClients(true);
                }

            }
            //server_model.closeTcpSocket();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }
}