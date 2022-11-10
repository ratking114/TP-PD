package server.Threads;

import server.ServerClasses.ServerModel;
import shared.Message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.SQLException;

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
                if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.LOGIN){
                    System.out.println("Received a Login command!");
                    _server_model.getEventDatabase().login(received_message);
                }


            }catch(IOException | ClassNotFoundException | SQLException e){
                System.out.println(e);
            }

        }
    }

    public void sendMessageToClient(Message message_to_send) throws IOException {
        //serialize the message
        ByteArrayOutputStream byte_array_output_stream = new ByteArrayOutputStream();
        new ObjectOutputStream(byte_array_output_stream).writeObject(message_to_send);

        //send the bytes to the client
        _client_socket.getOutputStream().write(byte_array_output_stream.toByteArray());
    }
}
