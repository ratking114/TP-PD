package server.Threads;

import client.Client;
import server.ServerClasses.ServerModel;
import shared.*;

import java.io.IOException;
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

    /* True if the user asked list of shows to book
     * False if he already choose a show and is looking for seats or otherwise is not looking for shows
     */
    private boolean _is_Looking_For_Shows;

    /* True if the user asked list of seats available for given show
     * False otherwise
     */
    private boolean _is_Looking_For_Seats;

    /**
     * the id of the show the user is currently booking seat for
     * 0 if the user is not looking for seats currently
     */
    private int _show_Id_OF_Seat_Been_Chosen;

    private final ServerModel _server_model;

    /**
     * The SocketChannel used to communicate with the client
     */
    private final SocketChannel _client_socket_channel;

    /**
     * The user that is being serviced
     */
    private User _user_being_serviced;


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
            while (!isInterrupted()) {
                //perform the select to see which Channels have events.
                //if the select returns 0 then we know that we were interrupted and as such we throw an
                //exception to go right to the handling catch clause
                if (_selector.select() == 0) {
                    System.out.println("I was Interrupted!");
                    throw new InterruptedException();
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

                        //




                    }
                    //something came from the Client
                    else if(((Client.TYPE_OF_CHANNEL) selected_channel.attachment()) == Client.TYPE_OF_CHANNEL.SOCKET){
                        //read the client message
                        Message received_message = ServerModel.extractMessageFromChannel(_client_socket_channel);

                        //see if the user is logged in and if it isnt send to it a message
                        if(isUserLoggedIn() == false && (received_message.type_of_message != Message.TYPE_OF_MESSAGE.REGISTER && received_message.type_of_message != Message.TYPE_OF_MESSAGE.LOGIN && received_message.type_of_message != Message.TYPE_OF_MESSAGE.CLIENT_EXIT)){
                            ServerModel.sendMessageViaChannel(
                                    new Message(Message.TYPE_OF_MESSAGE.LOGIN, new UserViewModel(null, Error_Messages.USER_NOT_LOGGED_IN)),
                                    _client_socket_channel
                            );
                        }

                        //now see what kind of message it is and process it
                        if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.LOGIN){
                            //print that we received a command
                            System.out.println("Received a Login command!");

                            //see if the client logged in successfully and if it dit save its state
                            Message login_success = _server_model.getEventDatabase().login(received_message);
                            if(((UserViewModel)login_success.attachment).error_message == Error_Messages.SUCESS){
                                _user_being_serviced = ((UserViewModel)login_success.attachment).user;
                            }

                            //send the answer to the Client
                            ServerModel.sendMessageViaChannel(login_success, _client_socket_channel);
                        }

                        else if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.GET_PAID_RESERVATION){
                            System.out.println("Received get paid reservation command");


                            ServerModel.sendMessageViaChannel(
                                    _server_model.getEventDatabase().getPaidReservations(received_message),
                                    _client_socket_channel
                            );
                        }
                        else if(received_message.type_of_message==Message.TYPE_OF_MESSAGE.LOGOUT) {
                            System.out.println("Received a logout command");
                            ServerModel.sendMessageViaChannel(
                                    _server_model.getEventDatabase().logout(received_message),
                                    _client_socket_channel
                            );

                            //set the current user being serviced as null to indicate that we are no longer
                            //servicing anybody
                            _user_being_serviced = null;
                        }
                        else if(received_message.type_of_message==Message.TYPE_OF_MESSAGE.INSERT_SHOW_BY_FILE) {
                            System.out.println("Received a insert show command");
                            ServerModel.sendMessageViaChannel(
                                    _server_model.getEventDatabase().insertShow(received_message),
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
                            if(!((EventFilter) received_message.attachment).visibility && isUserAdmin()) {
                                ServerModel.sendMessageViaChannel(
                                        _server_model.getEventDatabase().getShowsFromFilter(received_message),
                                        _client_socket_channel
                                );
                            } else if(((EventFilter) received_message.attachment).visibility){
                                ServerModel.sendMessageViaChannel(
                                        _server_model.getEventDatabase().getShowsFromFilter(received_message),
                                        _client_socket_channel
                                );
                            } else{
                                ServerModel.sendMessageViaChannel(
                                        new Message(Message.TYPE_OF_MESSAGE.GET_SHOWS_FILTERED,new EventFilterViewModel(null,Error_Messages.NOT_ADMIN)),
                                        _client_socket_channel
                                );
                            }
                        }
                        else if(received_message.type_of_message==Message.TYPE_OF_MESSAGE.GET_LIST_OF_ALL_SHOWS) {
                            Boolean booked = (Boolean)received_message.attachment;
                            if(booked == null)
                                booked = false;
                            System.out.println("Received a request list of shows command");
                            ServerModel.sendMessageViaChannel(
                                    new Message(Message.TYPE_OF_MESSAGE.GET_LIST_OF_ALL_SHOWS,_server_model.getEventDatabase().getListOfAllShows(booked,_user_being_serviced.administrator)),
                                    _client_socket_channel
                            );
                            _is_Looking_For_Shows=true;
                        }

                        else if(received_message.type_of_message==Message.TYPE_OF_MESSAGE.MAKE_VISIBLE) {
                            System.out.println("Received a make visible command");
                            if(isUserAdmin()) {
                                ServerModel.sendMessageViaChannel(
                                        _server_model.getEventDatabase().makeVisible(received_message),
                                        _client_socket_channel
                                );
                            } else{
                                ServerModel.sendMessageViaChannel(
                                        new Message(Message.TYPE_OF_MESSAGE.MAKE_VISIBLE, Error_Messages.NOT_ADMIN),
                                        _client_socket_channel
                                );
                            }
                        }

                        else if(received_message.type_of_message==Message.TYPE_OF_MESSAGE.DELETE_SHOW) {
                            System.out.println("Received a delete show command");
                            ServerModel.sendMessageViaChannel(
                                    new Message(Message.TYPE_OF_MESSAGE.DELETE_SHOW,_server_model.getEventDatabase().deleteShow(received_message)),
                                    _client_socket_channel
                            );
                        }

                        //a client wishes to exit and as such we must terminate
                        else if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.CLIENT_EXIT){
                            //throw an exception to go into the catch clause
                            throw new IOException();
                        }


                    }
                    //the timer fired
                    else{
                        //do what must be done when the timer fires
                        System.out.println("The timer fired!");


                        //remove the timer from the selector
                        selected_channel.cancel();
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
                    exception.printStackTrace();
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
                    ex.printStackTrace();
                }

                //terminate our execution
                return;

            //we were interrupted which means that we must close the client socket
            } catch (InterruptedException e) {
            try {
                System.out.println("ServiceClientThread interrupted!");
                _client_socket_channel.close();
            } catch (IOException ex) {
                System.out.println(ex);
                ex.printStackTrace();
            }
        }finally {
            try {
                System.out.println("ServiceClientThread terminating!");
                _client_socket_channel.close();
            } catch (IOException e) {
                System.out.println(e);
                e.printStackTrace();
            }
        }
    }

    public Pipe.SinkChannel getReceiveCommandsSinkChannel(){
        return _receive_commands.sink();
    }

    /**
     * Starts a timer and this thread shall be notified when the supplied time has elapsed
     * @param timer_duration the timer duration in miliseconds
     */
    public void startTimer(int timer_duration){
        try {
            //create a Pipe to let this Thread signal us and configure and place it on the selector
            Pipe notification_pipe = Pipe.open();
            notification_pipe.source().configureBlocking(false);
            notification_pipe.source().register(_selector, SelectionKey.OP_READ, Client.TYPE_OF_CHANNEL.TIMER);


            //now start the timer Thread
            new TimerThread(timer_duration, notification_pipe).start();

        } catch (Exception ignored) {}
    }

    boolean isUserLoggedIn(){
        return _user_being_serviced != null;
    }

    boolean isUserAdmin(){
        if(isUserLoggedIn())
            return _user_being_serviced.administrator;

        return false;
    }
}
