package client;

import jdk.jfr.Event;
import server.ServerClasses.ServerHeartBeatInfo;
import shared.*;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class Client {
    /**
     * The user instance that represents us.
     */
    private User _user;

    /**
     * The SocketChannel used to communicate with the Server
     */
    private SocketChannel _serverCommunicationSocket;

    /**
     * The SelectionKey of the socket used to remove it from the select
     */
    private SelectionKey _server_communication_socket_selection_key;

    /**
     * The list of Servers that are alive, or in other words, this is the list of Servers to wich
     * we can connect to.
     */
    private ArrayList<ServerHeartBeatInfo> _alive_server_list;


    /**
     * The Selector that will allow us to select from a Pipe that contains the information that
     * comes from the keyboard and from the Server socket
     */
    private Selector _selector;

    /**
     * The Pipe that contains the information that comes from the Keyboard.
     */
    private Pipe _keyboard_pipe;

    /**
     * The SelectionKey of the keyboard used to remove it from the select
     */
    private SelectionKey _keyboard_pipe_selection_key;

    /**
     * enum type used to figure out the type of a channel
     */
    public enum TYPE_OF_CHANNEL{PIPE, SOCKET}

    public static void main(String[] args) {
        try {
            //create a Client and start it passing it the server ip and the server udp port
            Client client = new Client();
            client.establishFirstConnection(InetAddress.getByName(args[0]), Integer.valueOf(args[1]));


            //now start reading commands
            client.start();

        //there is no Server availiable to us and as such we must exit
        }catch (Exception e){
            System.out.println(e);

            //sadly we must kill the keyboard thread too
            System.exit(0);
        }
    }

    /**
     * Starts the Client. when this function is called the client effectivly connects to a Server and
     * starts reading commands from the keyboard.
     */
    public void establishFirstConnection(InetAddress server_ip, int server_udp_port) throws IOException, ClassNotFoundException {
        //try to connect to a Server this is done by sending a message to the Server udp port
        //we received
        DatagramSocket socket_to_send = sendUdpMessage(
                new Message(Message.TYPE_OF_MESSAGE.SEND_SERVER_LIST, null), server_ip, server_udp_port
        );

        //read the message from the Server which contains the updated ServerList
        Message received_message = readUdpMessage(socket_to_send, server_ip, server_udp_port);

        //now store the alive server list that we received
        _alive_server_list = (ArrayList<ServerHeartBeatInfo>) received_message.attachment;

        //now try to connect to a Server
        tryToConnectToServer();
    }

    /**
     * Tries to connect to a Server that is on the alive Server list and this method throws an
     * Exception if it cannot.
     */
    void tryToConnectToServer(){
        //now try to connect to a server from this List
        boolean found_server = false;
        for(ServerHeartBeatInfo info : _alive_server_list){
            try {
                //try to connect to this Server
                _serverCommunicationSocket = SocketChannel.open(
                        new InetSocketAddress(info.getIpAddress(), info.getPort())
                );

                //if we arrived here everything went well and we can exit the loop
                found_server = true;
                break;

                //we were not able to connect to this Server and as such we must pass to the next one
                //until we reached the maximum number of servers
            }catch (IOException e){
                continue;
            }
        }


        //if we didn't found a Server we throw the appropriate exception
        if(!found_server){
            throw new RuntimeException("No server is availiable for connection");
        }

        //now that we established the connection to the Server we can start reading commands
        //from the keyboard and from the TCP socket that we created when we established a
        //connection with the server
        System.out.println("Established a connection with a Server");

        //greet the Server to tell that we are a client so the Server can Service us
        sendTCPMessage(new Message(Message.TYPE_OF_MESSAGE.HELLO_I_AM_CLIENT, null));
    }

    /**
     * Start reading commands from the KeyBoard and from the Server.
     */
    void start() throws IOException {
        //open the Selector
        _selector = Selector.open();

        //create the pipe that contains information that comes from the Keyboard
        _keyboard_pipe = Pipe.open();

        //configure the pipe to be read in non blocking mode and register it in the selector
        _keyboard_pipe.source().configureBlocking(false);
        _keyboard_pipe_selection_key = _keyboard_pipe.source().register(
                _selector, SelectionKey.OP_READ, TYPE_OF_CHANNEL.PIPE
        );

        //configure the socket to be read in non blocking mode and register it in the selector
        _serverCommunicationSocket.configureBlocking(false);
        _server_communication_socket_selection_key = _serverCommunicationSocket.register(
                _selector, SelectionKey.OP_READ, TYPE_OF_CHANNEL.SOCKET
        );


        //start the Thread that reads from the Keyboard
        new ReadFromKeyboardThread(this).start();

        //start reading from both channels with the Select
        while(true){
            //perform the select to see which Channels have events.
            if(_selector.select()==0){
                continue;
            }

            //see the Channels that fired events and traverse them fullfiling
            //the requests of whoever is on the other side
            Iterator<SelectionKey> selected_key_iterator = _selector.selectedKeys().iterator();

            while(selected_key_iterator.hasNext()) {
                //get the selected channel
                SelectionKey selected_channel = selected_key_iterator.next();

                //remove this key from the set of ready keys in order for this channel
                //to be out of the selector and therefore it will not fire an event
                //that already happened
                selected_key_iterator.remove();

                //figure out what type of channel fired, the keyboard or the socket

                //it is a Pipe so something came from the keyboard
                if( ((TYPE_OF_CHANNEL)selected_channel.attachment()) == TYPE_OF_CHANNEL.PIPE){
                    //read the message from the keyboard and see what command did the user type
                    String user_command = extractStringFromKeyboard();

                    //if its the login fetch the required data from the user
                    if(user_command.equals("login")){
                        System.out.println(executeLogin());
                    }
                    else if(user_command.equals("exit")){
                        executeExit();
                    }
                    else{
                        System.out.printf("Unrecognized Command: '%s'", user_command);
                    }
                }
                //something came from the Server
                else{
                    try {
                        //read the message
                        Message received_message = extractMessageFromSocketChannel(
                                (SocketChannel) selected_channel.channel()
                        );

                        //print the Message received from the Server
                        System.out.println("Received from Server: " + received_message.attachment);

                        //the Server closed the socket so we must try to connect to a new one
                    }catch (IOException e){
                        //try to connect to a new Server
                        System.out.println("The Server closed the Socket, lets try to connect to a new one!");
                        tryToConnectToServer();

                        //if we did it then we are here and we must remove the old socket and add
                        //a new one

                        //close and remove from the selector the old socket
                        selected_channel.channel().close();
                        selected_channel.cancel();

                        //add the new socket to the Selector
                        _serverCommunicationSocket.configureBlocking(false);
                        _server_communication_socket_selection_key = _serverCommunicationSocket.register(
                                _selector, SelectionKey.OP_READ, TYPE_OF_CHANNEL.SOCKET
                        );
                    }
                }

            }
        }
    }




    public Pipe.SinkChannel getPipeSinkChannelToWriteKeyboardInformation(){
        return _keyboard_pipe.sink();
    }


    private Message extractMessageFromSocketChannel(SocketChannel socket_channel) throws IOException {
        //read the message from the channel
        ByteBuffer message_from_channel = ByteBuffer.allocate(4096);
        socket_channel.read(message_from_channel);

        //deserialize the Message from the Channel and return it
        ObjectInputStream object_input_stream = new ObjectInputStream(
                new ByteArrayInputStream(message_from_channel.array())
        );
        try {
            return (Message) object_input_stream.readObject();
        }catch (ClassNotFoundException ignored){
            return null;
        }
    }

    private DatagramSocket sendUdpMessage(Message message_to_send, InetAddress ip_address, int udp_port) throws IOException {
        //serialize the message
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bOut);
        out.writeObject(message_to_send);

        //create the DatagramPacket used to send the message
        DatagramPacket packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), ip_address, udp_port);

        //send the message
        DatagramSocket socket = new DatagramSocket();
        socket.send(packet);

        //return the socket used to send the message so the user can read the answer
        return socket;
    }

    private Message readUdpMessage(DatagramSocket socket, InetAddress ip_address, int udp_port) throws IOException, ClassNotFoundException{
        //create the Datagram packet used to receive the message
        DatagramPacket received_message = new DatagramPacket(
                new byte[4096], 4096, ip_address, udp_port
        );

        //read the message from the socket
        socket.receive(received_message);


        //extract the message from it
        return (Message) new ObjectInputStream(new ByteArrayInputStream(received_message.getData())).readObject();
    }

    public void sendTCPMessage(Message message_to_send){
        try {
            //serialize the Message
            ByteArrayOutputStream serialized_message = new ByteArrayOutputStream();
            ObjectOutputStream object_output_stream = new ObjectOutputStream(serialized_message);
            object_output_stream.writeObject(message_to_send);

            //write the message to the channel which sends it to the other side of the TCP
            //connection
            _serverCommunicationSocket.write(ByteBuffer.wrap(serialized_message.toByteArray()));
        }catch (IOException e){
            System.out.println(e);
            e.printStackTrace();
        }
    }

    public Message readTCPMessage(){
        try {
            //read the message from the channel into a byte array
            ByteBuffer serialized_message = ByteBuffer.allocate(4096);
            _serverCommunicationSocket.read(serialized_message);

            //extract the message from it
            ObjectInputStream object_input_stream = new ObjectInputStream(
                    new ByteArrayInputStream(serialized_message.array())
            );
            return (Message) object_input_stream.readObject();
        }catch (ClassNotFoundException | IOException e){
            System.out.println(e);
            e.printStackTrace();
            return null;
        }

    }

    private String extractStringFromKeyboard() throws IOException {
        //read the message from the keyboard
        ByteBuffer message_from_keyboard = ByteBuffer.allocate(256);
        int bytes_read = _keyboard_pipe.source().read(message_from_keyboard);

        //return the message that the user sent
        return new String(message_from_keyboard.array(), 0, bytes_read);
    }

    /**
     * Fetches the login data from the user and execute the login function
     */
    private String executeLogin() throws IOException {
        //get the required login data from the user
        System.out.print("Type your username and password\n>");

        //get the username
        System.out.print(">");
        String username = extractStringFromKeyboard();

        //get the password
        System.out.print(">");
        String password = extractStringFromKeyboard();


        return login(username, password);
    }

    private void executeExit(){
        //send the exit message to the Server
        sendTCPMessage(new Message(Message.TYPE_OF_MESSAGE.CLIENT_EXIT, null));

        //effectively terminate the client application
        System.out.println("Goodbye");
        System.exit(0);
    }

    public String getShowsFromFilter(int id, String designation, String type, String date, int duration, String location, String county, String country, String age_Restriction){
        EventInfo event_info = new EventInfo(id,designation,type,date,duration,location,county,country,age_Restriction,null);
        Message message_to_send = new Message(Message.TYPE_OF_MESSAGE.GET_SHOWS_FILTERED, event_info);

        sendTCPMessage(message_to_send);

        Message received_message = readTCPMessage();

        return null;
    }

    public String getUnpaidReservations(){
        Message message_to_send = new Message(Message.TYPE_OF_MESSAGE.GET_UNPAID_RESERVATION, _user);

        sendTCPMessage(message_to_send);

        Message received_message = readTCPMessage();

        List<ReservationViewmodel> unpaid_for_reservation_list = (List<ReservationViewmodel>) received_message.attachment;

        StringBuilder sb = new StringBuilder();
        sb.append("Currently unpaid reservations:\n");
        for(ReservationViewmodel reservation : unpaid_for_reservation_list){
            sb.append(reservation.event).append("\n");
        }

        return sb.toString();

    }

    public String getPaidReservations(){
        Message message_to_send = new Message(Message.TYPE_OF_MESSAGE.GET_PAID_RESERVATION, _user);

        sendTCPMessage(message_to_send);

        Message received_message = readTCPMessage();

        List<ReservationViewmodel> paid_for_reservation_list = (List<ReservationViewmodel>) received_message.attachment;

        StringBuilder sb = new StringBuilder();
        sb.append("Currently paid reservations:\n");
        for(ReservationViewmodel reservation : paid_for_reservation_list){
            sb.append(reservation.event).append("\n");
        }

        return sb.toString();

    }

    public String login(String username, String password){
        User user_to_send = new User();
        user_to_send.username = username;

        //hash password using sha-256 algorithm
        user_to_send.password = Utils.hashString(password);

        Message message_to_send = new Message(Message.TYPE_OF_MESSAGE.LOGIN, user_to_send);

        //send the serialized user object to the server
        sendTCPMessage(message_to_send);

        try {
            //wait for an answer from the Server
            waitForServerAnswer();
        } catch (IOException e) {
            System.out.println(e);
            e.printStackTrace();
        }

        //read the message the server sent back about our login
        Message received_message = readTCPMessage();

        UserViewModel user_viewmodel = (UserViewModel) received_message.attachment;
        //if the server doesn't return a sucess error then it failed
        switch (user_viewmodel.error_message){
            case USERNAME_DOESNT_EXIST -> {
                return Error_Messages.USERNAME_DOESNT_EXIST.toString();
            }
            case INVALID_PASSWORD -> {
                return Error_Messages.INVALID_PASSWORD.toString();
            }
            default ->{
                //if sucess set the user equal to the user data sent by the server
                _user = user_viewmodel.user;
                return "Sucessfully logged in";
            }

        }

    }

    /**
     *This function will try to register the user in the servers database and subsequently log them
     * @param username User's chosen username to register
     * @param name user's given name
     * @param password user's password
     * @return a feedback message to let the user know of the result of the registration
     */

    public String Register(String username,String name,String password){

        User new_user=new User(username,name,password);
        try {
            Message register_Message = new Message(Message.TYPE_OF_MESSAGE.REGISTER,username);
            sendTCPMessage(register_Message);

            Message register_Response = readTCPMessage();

            if(register_Response.type_of_message.equals(Message.TYPE_OF_MESSAGE.REGISTER)) {
                switch ((Error_Messages) register_Response.attachment) {
                    case SUCESS -> {
                        this._user = new_user;
                        this._user.authenticated = true;
                        return Error_Messages.SUCESS.toString();
                    }
                    case INVALID_PASSWORD -> {
                        return Error_Messages.INVALID_PASSWORD.toString();
                    }
                    case INVALID_USERNAME -> {
                        return Error_Messages.INVALID_USERNAME.toString();
                    }
                    case USERNAME_ALREADY_EXISTS -> {
                        return Error_Messages.USERNAME_ALREADY_EXISTS.toString();
                    }
                    case SQL_ERROR -> {
                        return Error_Messages.SQL_ERROR.toString();
                    }
                    //default -> {return "Unknown error ocurred";}
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);

        }

    return "Unknown error ocurred";
    }

    /**
     *this function will try to edit the user information on the server database
     * @param user_viewModel a class containing both the data the user choose to change and unchanged data to send to the server to replace the current one
     * @param password the password the user input to check  agains his real password to allow the modifications
     * @return Confirmation/error message with the result of the change
     */
    public Error_Messages EditUser(User user_viewModel,String password)
    {

        if(!password.equals(user_viewModel.password)) {
            return Error_Messages.INVALID_CONFIRMATION_PASSWORD;

        }
        user_viewModel.old_Username=_user.username;
        sendTCPMessage(new Message(Message.TYPE_OF_MESSAGE.EDIT_USER_DATA,user_viewModel));
        Message server_Response = readTCPMessage();
        if(server_Response.type_of_message.equals(Message.TYPE_OF_MESSAGE.EDIT_USER_DATA))
        {

            Error_Messages edit_Feedback=(Error_Messages) server_Response.attachment;
            if(edit_Feedback.equals(Error_Messages.SUCESS))
                _user=user_viewModel;
            return edit_Feedback;
        }
        return Error_Messages.SUCESS;
    }



    /**
     * Upon return of this method one can read a message from the Server.
     */
    public void waitForServerAnswer() throws IOException {
        //wait for the socket to be ready for read and as such we must take out the keyboard pipe from the
        //Selector and only wait for the socket and when we have information on the Socket we shall put the
        //keyboard pipe back in
        _keyboard_pipe_selection_key.cancel();

        //perform the select only on the Socket
        _selector.select();

        //get an iterator to the selected keys
        Iterator<SelectionKey> selected_key_iterator = _selector.selectedKeys().iterator();

        //advance it to put it on the socket
        selected_key_iterator.next();

        //remove this key as we will read from it
        selected_key_iterator.remove();

        //now we can put the pipe back in
        _keyboard_pipe_selection_key = _keyboard_pipe.source().register(
                _selector, SelectionKey.OP_READ, TYPE_OF_CHANNEL.PIPE
        );
    }


    /**
     * Upon completion of this method one can read information from the keyboard without blocking
     * @throws IOException in case an IO error occurs
     */
    public void waitForKeyboardInformation() throws IOException {
        //wait for the keyboard to be ready for read and as such we must take out the socket from the
        //Selector and only wait for the keyboard and when we have information on the keyboard we shall put the
        //socket back in
        _server_communication_socket_selection_key.cancel();

        //perform the select only on the keyboard
        _selector.select();

        //get an iterator to the selected keys
        Iterator<SelectionKey> selected_key_iterator = _selector.selectedKeys().iterator();

        //advance it to put it on the keyboard pipe
        selected_key_iterator.next();

        //remove this key as we will read from it
        selected_key_iterator.remove();

        //now we can put the socket back in
        _serverCommunicationSocket.configureBlocking(false);
        _server_communication_socket_selection_key = _serverCommunicationSocket.register(
                _selector, SelectionKey.OP_READ, TYPE_OF_CHANNEL.SOCKET
        );
    }


}
