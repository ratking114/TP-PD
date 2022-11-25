package server.Threads;

import client.Client;
import server.ServerClasses.ServerModel;
import shared.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * ServiceClientThread is the Thread that services a client.
 */
public class ServiceClientThread extends Thread {

    /**
     * The ClientActivity of the client that is being serviced
     */
    private ClientActivity _client_activity;

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

    private static int RESERVATION_EXPIRE_TIME = 120 * 1000;

    public ServiceClientThread(ServerModel server_model, SocketChannel client_socket_channel, ClientActivity client_activity){
        //save the ServerModel
        _server_model = server_model;

        //save the client socket channel
        _client_socket_channel = client_socket_channel;

        //store the client activity
        _client_activity = client_activity;

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

        }

        //if the client activity is not null then this client was connect to another server and the client
        //might have reservations that were not paid and as such we must initiate a timer for each reservation
        //that is not payed yet
        if(_client_activity != null) {
            List<ReservationViewmodel> client_unpaid_reservations = null;
            List<java.util.Date> unpaid_reservation_dates = null;
            try {
                client_unpaid_reservations = (List<ReservationViewmodel>) _server_model.getEventDatabase().getUnpaidReservations(
                        new Message(Message.TYPE_OF_MESSAGE.GET_UNPAID_RESERVATION, _client_activity.user)
                ).attachment;

                //get the dates of the unpaid reservations
                unpaid_reservation_dates = _server_model.getEventDatabase().getDatesOfReservation(
                        client_unpaid_reservations
                );
            } catch (Exception e) {

            }

            //now for each reservation get its date and start a timer with the remaining time
            for (int i = 0; i != client_unpaid_reservations.size(); ++i) {
                //get the milliseconds that must elapse till this reservation expires
                long miliseconds_till_reservation_expires =  RESERVATION_EXPIRE_TIME - (new Date().getTime() - unpaid_reservation_dates.get(i).getTime());


                //create a timer for this reservation
                startTimer(Math.max((int) miliseconds_till_reservation_expires, 0), client_unpaid_reservations.get(i).reservation.id);
            }
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

                    //figure out what type of channel fired, the pipe or the socket or the timer
                    try {
                        Client.TYPE_OF_CHANNEL fired_channel = ((Client.TYPE_OF_CHANNEL) selected_channel.attachment());
                    }catch (ClassCastException e){
                        //the timer fired
                        //remove the client reservations and tell them they no longer have the reservations
                        TimerThread timer_thread = (TimerThread)selected_channel.attachment();
                        Error_Messages reservation_deleted_successfully = _server_model.getEventDatabase().removeClientReservation(
                                timer_thread.getTimerId()
                        );

                        //notify the client that his reservation expired
                        if(reservation_deleted_successfully == Error_Messages.SUCESS){
                            ServerModel.sendMessageViaChannel(
                                    new Message(Message.TYPE_OF_MESSAGE.RESERVATION_EXPIRED, timer_thread.getTimerId()),
                                    _client_socket_channel
                            );

                            //update the data members with the client activity as the client is no longer
                            //booking seats
                            _client_activity.is_looking_for_seats = false;
                            _client_activity.show_id_of_seats_being_reserved = -1;

                            ServerModel.sendMessageViaChannel(
                                    new Message(Message.TYPE_OF_MESSAGE.CLIENT_ACTIVITY_UPDATED, _client_activity)
                                    ,_client_socket_channel
                            );
                        }
                        //the cluster could not update the "whole" database, so we must try again
                        else{
                            startTimer(10 * 1000, ((TimerThread)selected_channel.attachment()).getTimerId());
                        }

                        //remove the timer from the selector
                        selected_channel.cancel();

                        //contiue the select
                        continue;
                    }


                    //it is a Pipe so something came from another Thread
                    if (((Client.TYPE_OF_CHANNEL) selected_channel.attachment()) == Client.TYPE_OF_CHANNEL.PIPE) {
                        //read the message to see what kind of command we received
                        Message received_message = ServerModel.extractMessageFromChannel(_receive_commands.source());

                        //we must send the alive server list to our client
                        if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.ALIVE_SERVER_LIST){
                            ServerModel.sendMessageViaChannel(received_message, _client_socket_channel);
                        }
                        else if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.SEATS_UPDATED){
                            ServerModel.sendMessageViaChannel(received_message, _client_socket_channel);
                        }

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

                            //see if the client logged in successfully and if it dit save its state
                            Message login_success = _server_model.getEventDatabase().login(received_message);
                            if(((UserViewModel)login_success.attachment).error_message == Error_Messages.SUCESS){
                                _client_activity  = new ClientActivity (
                                        ((UserViewModel)login_success.attachment).user,  -1, false
                                );
                            }

                            //send the answer to the Client
                            ServerModel.sendMessageViaChannel(login_success, _client_socket_channel);
                        }

                        else if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.GET_PAID_RESERVATION){


                            ServerModel.sendMessageViaChannel(
                                    _server_model.getEventDatabase().getPaidReservations(received_message),
                                    _client_socket_channel
                            );
                        }
                        else if(received_message.type_of_message==Message.TYPE_OF_MESSAGE.LOGOUT) {
                            ServerModel.sendMessageViaChannel(
                                    _server_model.getEventDatabase().logout(received_message),
                                    _client_socket_channel
                            );

                            //if the client had reservations to pay then delete them all
                            List<ReservationViewmodel> unpaid_reservations = (List<ReservationViewmodel>) this._server_model.getEventDatabase().getUnpaidReservations(
                                    new Message(Message.TYPE_OF_MESSAGE.GET_UNPAID_RESERVATION, _client_activity.user)
                            ).attachment;
                            for(ReservationViewmodel reservation : unpaid_reservations) {
                                this._server_model.getEventDatabase().removeClientReservation(reservation.reservation.id);
                            }

                            //set the current user being serviced as null to indicate that we are no longer
                            //servicing anybody
                            _client_activity = null;
                        }
                        else if(received_message.type_of_message==Message.TYPE_OF_MESSAGE.INSERT_SHOW_BY_FILE) {
                            if(_client_activity.user.administrator) {
                                ServerModel.sendMessageViaChannel(
                                        _server_model.getEventDatabase().insertShow(received_message),
                                        _client_socket_channel
                                );
                            }
                            else{
                                ServerModel.sendMessageViaChannel(
                                        new Message(Message.TYPE_OF_MESSAGE.INSERT_SHOW_BY_FILE, Error_Messages.NOT_ADMIN),
                                        _client_socket_channel
                                );
                            }
                        }
                        else if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.GET_UNPAID_RESERVATION){

                            ServerModel.sendMessageViaChannel(
                                    _server_model.getEventDatabase().getUnpaidReservations(received_message),
                                    _client_socket_channel
                            );
                        }

                        else if(received_message.type_of_message==Message.TYPE_OF_MESSAGE.REGISTER) {

                            //see if the client logged in successfully and if it dit save its state
                            Error_Messages register_success = _server_model.getEventDatabase().register(received_message);
                            if(register_success == Error_Messages.SUCESS){
                                _client_activity = new ClientActivity(
                                        (User)received_message.attachment, -1, false
                                );
                            }

                            //send the answer to the Client
                            ServerModel.sendMessageViaChannel(
                                    new Message(Message.TYPE_OF_MESSAGE.REGISTER, register_success),
                                    _client_socket_channel
                            );
                        }
                        else if(received_message.type_of_message==Message.TYPE_OF_MESSAGE.EDIT_USER_DATA) {
                            ServerModel.sendMessageViaChannel(
                                    new Message(Message.TYPE_OF_MESSAGE.EDIT_USER_DATA,_server_model.getEventDatabase().changeUserData(received_message)),
                                    _client_socket_channel
                            );
                        }
                        else if(received_message.type_of_message==Message.TYPE_OF_MESSAGE.GET_SHOWS_FILTERED) {
                            if(!((EventFilter) received_message.attachment).visibility && isUserAdmin()) {
                               Message message_rcv =  _server_model.getEventDatabase().getShowsFromFilter(received_message);
                               EventFilterViewModel eventFilterViewModel = (EventFilterViewModel) message_rcv.attachment;

                                ServerModel.sendMessageViaChannel(message_rcv, _client_socket_channel);

                            } else if(((EventFilter) received_message.attachment).visibility){
                                Message message_rcv =  _server_model.getEventDatabase().getShowsFromFilter(received_message);
                                EventFilterViewModel eventFilterViewModel = (EventFilterViewModel) message_rcv.attachment;

                                ServerModel.sendMessageViaChannel(message_rcv, _client_socket_channel);
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
                            List<EventInfo> shows = _server_model.getEventDatabase().getListOfAllShows(booked, _client_activity.user.administrator);
                            if(shows==null||shows.size()>0) {
                                ServerModel.sendMessageViaChannel(
                                        new Message(Message.TYPE_OF_MESSAGE.GET_LIST_OF_ALL_SHOWS, shows),
                                        _client_socket_channel
                                );
                            }
                        }
                        else if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.GIVE_UP_BOOKING)
                        {
                            _client_activity.is_looking_for_seats = false;
                            _client_activity.show_id_of_seats_being_reserved = -1;

                            ServerModel.sendMessageViaChannel(
                                    new Message(Message.TYPE_OF_MESSAGE.CLIENT_ACTIVITY_UPDATED, _client_activity),
                                    _client_socket_channel
                            );
                        }
                        else if(received_message.type_of_message==Message.TYPE_OF_MESSAGE.GET_AVAILABLE_SEATS)
                        {
                            _client_activity.show_id_of_seats_being_reserved=(Integer)received_message.attachment;
                            _client_activity.is_looking_for_seats=true;
                            ServerModel.sendMessageViaChannel(
                                    new Message(Message.TYPE_OF_MESSAGE.BOOK_SEAT,_server_model.getEventDatabase().get_Available_Seats(_client_activity.show_id_of_seats_being_reserved))
                                    ,_client_socket_channel
                            );
                            ServerModel.sendMessageViaChannel(
                                    new Message(Message.TYPE_OF_MESSAGE.CLIENT_ACTIVITY_UPDATED, _client_activity),
                                    _client_socket_channel
                            );
                        }
                        else if(received_message.type_of_message==Message.TYPE_OF_MESSAGE.BOOK_SEAT) {
                            //try to reserve these seats
                            Pair<Integer, List<SeatPrice>> reservation_id_and_booked_seats = _server_model.getEventDatabase().book_Seat((List<SeatPrice>)received_message.attachment, _client_activity.user);

                            //see if the reservation was a success and if it was start a timer with the
                            //reservation id
                            if(reservation_id_and_booked_seats.first != -1){
                                startTimer(120 * 1000, reservation_id_and_booked_seats.first);
                            }

                            //send the answer to the client
                            ServerModel.sendMessageViaChannel(
                                    new Message(Message.TYPE_OF_MESSAGE.BOOK_SEAT, reservation_id_and_booked_seats.second)
                                    ,_client_socket_channel
                            );



                            //update the data members with the client activity as the client is no longer
                            //booking seats
                            _client_activity.is_looking_for_seats = false;
                            _client_activity.show_id_of_seats_being_reserved = -1;

                            ServerModel.sendMessageViaChannel(
                                    new Message(Message.TYPE_OF_MESSAGE.CLIENT_ACTIVITY_UPDATED, _client_activity)
                                    ,_client_socket_channel
                            );
                        }

                        else if(received_message.type_of_message==Message.TYPE_OF_MESSAGE.MAKE_VISIBLE) {
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
                            ServerModel.sendMessageViaChannel(
                                    new Message(Message.TYPE_OF_MESSAGE.DELETE_SHOW,_server_model.getEventDatabase().deleteShow(received_message)),
                                    _client_socket_channel
                            );
                        }

                        //a client wishes to exit and as such we must terminate
                        else if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.CLIENT_EXIT){
                            //see if the client is logged in, log them out
                            if(_client_activity != null){
                                this._server_model.getEventDatabase().logout(
                                        new Message(Message.TYPE_OF_MESSAGE.LOGOUT, _client_activity.user)
                                );

                                //if the client had reservations to pay then delete them all
                                List<ReservationViewmodel> unpaid_reservations = (List<ReservationViewmodel>) this._server_model.getEventDatabase().getUnpaidReservations(
                                        new Message(Message.TYPE_OF_MESSAGE.GET_UNPAID_RESERVATION, _client_activity.user)
                                ).attachment;
                                for(ReservationViewmodel reservation : unpaid_reservations) {
                                    this._server_model.getEventDatabase().removeClientReservation(reservation.reservation.id);
                                }
                            }
                            //throw an exception to go into the catch clause
                            throw new IOException();
                        }

                        else if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.PAY_SEATS){
                            Error_Messages answer = _server_model.getEventDatabase().paySeat(
                                    ((int)received_message.attachment)
                            );
                            if(answer == Error_Messages.SUCESS){
                                //interrupt the timer thread and cancel
                                SelectionKey selected_key = _selector.keys().stream().filter(
                                        (SelectionKey key) ->{
                                            try {
                                                return ((TimerThread) key.attachment()).getTimerId() == ((int)received_message.attachment) ;
                                            }catch (ClassCastException e){return false;}
                                        }
                                ).findAny().get();

                                TimerThread associated_timer_thread = (TimerThread) selected_key.attachment();
                                associated_timer_thread.interrupt();
                                selected_key.cancel();
                            }

                            ServerModel.sendMessageViaChannel(
                                    new Message(Message.TYPE_OF_MESSAGE.PAY_SEATS, answer) , _client_socket_channel
                            );
                        }

                    }
                }
            }
            //this client disconnected so we must take him out
            }catch(IOException | SQLException e){
                //close the client socket
                try {
                    _client_socket_channel.close();
                    _receive_commands.source().close();
                    _receive_commands.sink().close();
                } catch (IOException exception) {

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

                }

                //terminate our execution
                return;

            //we were interrupted which means that we must close the client socket
            } catch (InterruptedException e) {
            try {
                _client_socket_channel.close();
            } catch (IOException ex) {

            }
        }finally {
            try {
                _client_socket_channel.close();
            } catch (IOException e) {

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
    public void startTimer(int timer_duration, int timer_id){
        try {
            //create a Pipe to let this Thread signal us and configure and place it on the selector
            Pipe notification_pipe = Pipe.open();
            notification_pipe.source().configureBlocking(false);
            TimerThread timer_thread = new TimerThread(timer_id, timer_duration, notification_pipe);
            notification_pipe.source().register(_selector, SelectionKey.OP_READ, timer_thread);


            //now start the timer Thread
            timer_thread.start();

        } catch (Exception ignored) {}
    }

    boolean isUserLoggedIn(){
        return _client_activity != null;
    }

    boolean isUserAdmin(){
        if(isUserLoggedIn())
            return _client_activity.user.administrator;

        return false;
    }


    public boolean is_Looking_For_Seats() {
        return _client_activity.is_looking_for_seats;
    }

    public int get_show_Id_OF_Seat_Been_Chosen() {
        return _client_activity.show_id_of_seats_being_reserved;
    }
}
