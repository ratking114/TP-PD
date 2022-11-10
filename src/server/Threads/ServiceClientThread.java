package server.Threads;

import server.ServerClasses.ServerModel;
import shared.Message;

import java.io.IOException;
import java.net.Socket;

/**
 * ServiceClientThread is the Thread that services a client.
 */
public class ServiceClientThread extends Thread {
    private ServerModel _server_model;
    private Socket _client_socket;


    public ServiceClientThread(ServerModel server_model, Socket client_socket){
        _server_model = server_model;
        _client_socket = client_socket;
    }

    @Override
    public void run() {
        while(true){
            try {
                //read a Message from the Client
                Message received_message = ServerModel.deserializeMessage(_client_socket.getInputStream());

                //now see what kind of message it is an process it
                //if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.LOGIN)




            }catch(IOException | ClassNotFoundException e){
                System.out.println(e);
            }

        }
    }
}
