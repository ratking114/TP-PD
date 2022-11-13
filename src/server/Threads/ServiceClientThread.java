package server.Threads;

import client.Client;
import server.ServerClasses.ServerModel;
import shared.Message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.sql.SQLException;
import java.util.Iterator;

/**
 * ServiceClientThread is the Thread that services a client.
 */
public class ServiceClientThread extends Thread {
    private final ServerModel _server_model;

    /**
     * The SocketChannel used to communicate with the client
     */
    private final SocketChannel _client_socket_channel;


    /**
     * The Pipe that allows us to receive commands from other threads
     */
    private Pipe _receive_commands;

    private Selector _selector;

    public ServiceClientThread(ServerModel server_model, SocketChannel client_socket_channel){
        //save the ServerModel
        _server_model = server_model;

        //save the client socket channel
        _client_socket_channel = client_socket_channel;

        //create the pipe used to receive commands from other Threads
        try {
            //open the Selector
            _selector = Selector.open();

            //create the pipe to read commands from other Threads
            _receive_commands = Pipe.open();

            //configure the pipe to be read in non blocking mode and register it in the selector
            _receive_commands.source().configureBlocking(false);
            _receive_commands.source().register(
                    _selector, SelectionKey.OP_READ, Client.TYPE_OF_CHANNEL.PIPE
            );

            //configure the socket to be read in non blocking mode and register it in the selector
            _client_socket_channel.configureBlocking(false);
            _client_socket_channel.register(
                    _selector, SelectionKey.OP_READ, Client.TYPE_OF_CHANNEL.SOCKET
            );
        }
        catch (IOException e) {
            System.out.println(e);
        }

    }

    public SocketChannel getClientSocketChannel(){
        return _client_socket_channel;
    }


    @Override
    public void run() {
        //start reading from both channels with the Select
        try {
            while (true) {
                //perform the select to see which Channels have events.
                if (_selector.select() == 0) {
                    continue;
                }

                //see the Channels that fired events and traverse them fullfiling
                //the requests of whoever is on the other side
                Iterator<SelectionKey> selected_key_iterator = _selector.selectedKeys().iterator();

                while (selected_key_iterator.hasNext()) {
                    //get the selected channel
                    SelectionKey selected_channel = selected_key_iterator.next();

                    //remove this key from the set of ready keys in order for this channel
                    //to be out of the selector and therefore it will not fire an event
                    //that already happened
                    selected_key_iterator.remove();

                    //figure out what type of channel fired, the pipe or the socket

                    //it is a Pipe so something came from another Thread
                    if (((Client.TYPE_OF_CHANNEL) selected_channel.attachment()) == Client.TYPE_OF_CHANNEL.PIPE) {
                        //read the message to see what kind of command we received
                        Message received_message = ServerModel.extractMessageFromChannel(_receive_commands.source());

                        //we must send the alive server list to our client
                        if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.ALIVE_SERVER_LIST){
                            ServerModel.sendMessageViaChannel(received_message, _client_socket_channel);
                        }

                    }
                    //something came from the Client
                    else {
                        //read the client message
                        Message received_message = ServerModel.extractMessageFromChannel(_client_socket_channel);

                        //now see what kind of message it is and process it
                        if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.LOGIN){
                            //print that we received a command
                            System.out.println("Received a Login command!");

                            //send the answer to the Client
                            ServerModel.sendMessageViaChannel(
                                    _server_model.getEventDatabase().login(received_message),
                                    _client_socket_channel
                            );
                        }

                        else if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.GET_PAID_RESERVATION){
                            System.out.println("Received get paid reservation command");


                            ServerModel.sendMessageViaChannel(
                                    _server_model.getEventDatabase().getPaidReservations(received_message),
                                    _client_socket_channel
                            );
                        }
                        else if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.GET_UNPAID_RESERVATION){
                            System.out.println("Received get unpaid reservation command");


                            ServerModel.sendMessageViaChannel(
                                    _server_model.getEventDatabase().getUnpaidReservations(received_message),
                                    _client_socket_channel
                            );
                        }

                        else if(received_message.type_of_message==Message.TYPE_OF_MESSAGE.REGISTER) {
                            System.out.println("Received a Register command");
                            ServerModel.sendMessageViaChannel(
                                    new Message(Message.TYPE_OF_MESSAGE.REGISTER, _server_model.getEventDatabase().register(received_message)),
                                    _client_socket_channel
                            );
                        }
                        else if(received_message.type_of_message==Message.TYPE_OF_MESSAGE.EDIT_USER_DATA) {
                            System.out.println("Received a Change User Data command");
                            ServerModel.sendMessageViaChannel(
                                    new Message(Message.TYPE_OF_MESSAGE.EDIT_USER_DATA,_server_model.getEventDatabase().changeUserData(received_message)),
                                    _client_socket_channel
                            );
                        }
                        else if(received_message.type_of_message==Message.TYPE_OF_MESSAGE.GET_SHOWS_FILTERED) {
                            System.out.println("Received a filter shows command");
                            ServerModel.sendMessageViaChannel(
                                    new Message(Message.TYPE_OF_MESSAGE.GET_SHOWS_FILTERED,_server_model.getEventDatabase().getShowsFromFilter(received_message)),
                                    _client_socket_channel
                            );
                        }
                        //a client wishes to exit and as such we must terminate
                        else if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.CLIENT_EXIT){
                            //throw an exception to go into the catch clause
                            throw new IOException();
                        }

                    }
                }
            }
            //this client disconnected so we must take him out
            }catch(IOException | SQLException e){
                //print a status information
                System.out.println("The client closed the socket so we must remove him");

                //close the client socket
                try {
                    _client_socket_channel.close();
                    _receive_commands.source().close();
                    _receive_commands.sink().close();
                } catch (IOException exception) {
                    System.out.println(exception);
                }

                //decrement the Server workload
                _server_model.lostTcpConnection();

                //remove ourselves from the collection of ServiceClientThreads
                _server_model.removeServiceClientThread(this);

                //send the updated server list to the clients since our workload changed
                _server_model.sendUpdatedServerListToClients(true);

                try {
                    //send a HeartBeat because our state changed
                    _server_model.sendHeartBeat();
                } catch (IOException ex) {
                    System.out.println(ex);
                }

                //terminate our execution
                return;
            }
    }


    public Pipe.SinkChannel getReceiveCommandsSinkChannel(){
        return _receive_commands.sink();
    }
}
