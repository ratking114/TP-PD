package server.Threads;

import server.ServerClasses.ServerModel;
import shared.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
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
                if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.HELLO_I_AM_SERVER){

                    //send the server.database file to the other server
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(connected_client.getOutputStream());
                    objectOutputStream.writeObject(
                            new Message(
                                Message.TYPE_OF_MESSAGE.HELLO_I_AM_SERVER,
                                new FileInputStream(new File(server_model.getDatabaseFileName().toUri())).readAllBytes()
                            )
                    );

                   // ServerHeartBeatInfo serverHeartBeatInfo=((ServerHeartBeatInfo)received_message.attachment);

                }
                else{

                }



                //print its contents
                System.out.println(received_message.type_of_message);
            }
            //server_model.closeTcpSocket();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}