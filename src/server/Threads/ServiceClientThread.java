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
    private final ServerModel _server_model;
    private final Socket _client_socket;

    public ServiceClientThread(ServerModel server_model, Socket client_socket){
        _server_model = server_model;
        _client_socket = client_socket;
    }

    public Socket getClientSocket(){
        return _client_socket;
    }


    @Override
    public void run() {
        while(true){
            try {
                //read a Message from the Client
                Message received_message = null;
                synchronized (_client_socket) {
                    received_message = ServerModel.deserializeMessage(_client_socket.getInputStream());
                }
                //now see what kind of message it is and process it
                if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.LOGIN){
                    //print that we received a command
                    System.out.println("Received a Login command!");

                    //send the answer to the Client
                    sendMessageToClient(_server_model.getEventDatabase().login(received_message));
                }

                //a client wishes to exit and as such we must terminate
                else if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.CLIENT_EXIT){
                    //throw an exception to go into the catch clause
                    throw new IOException();
                }
            //this client disconnected so we must take him out
            }catch(IOException | ClassNotFoundException | SQLException e){
                //print a status information
                System.out.println("The client closed the socket so we must remove him");

                //close the client socket
                try {
                    _client_socket.close();
                } catch (IOException ignored) {}

                //decrement the Server workload
                _server_model.lostTcpConnection();

                //remove ourselves from the collection of ServiceClientThreads
                _server_model.removeServiceClientThread(this);

                //terminate our execution
                return;
            }

        }
    }

    public void sendMessageToClient(Message message_to_send) throws IOException {
        //serialize the message
        ByteArrayOutputStream byte_array_output_stream = new ByteArrayOutputStream();
        new ObjectOutputStream(byte_array_output_stream).writeObject(message_to_send);

        //send the bytes to the client
        synchronized (_client_socket) {
            _client_socket.getOutputStream().write(byte_array_output_stream.toByteArray());
        }
    }
}
