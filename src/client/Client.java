package client;

import server.ServerClasses.ServerHeartBeatInfo;
import shared.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 *
 */
public class Client {

    /**
     * The user instance that represents us.
     */
    private User _user;

    private Socket _serverCommunicationSocket;

    /**
     * The list of Servers that are alive, or in other words, this is the list of Servers to wich
     * we can connect to.
     */
    private ArrayList<ServerHeartBeatInfo> _alive_server_list;

    public static void main(String[] args) {
        try {
            //create a Client and start it passint it the server ip and the server udp port
            Client client = new Client();
            client.start(InetAddress.getByName(args[0]), Integer.valueOf(args[1]));
        }catch (Exception ignored){}
    }

    /**
     * Starts the Client. when this function is called the client effectivly connects to a Server and
     * starts reading commands from the keyboard.
     */
    public void start(InetAddress server_ip, int server_udp_port) throws IOException, ClassNotFoundException {
        //try to connect to a Server this is done by sending a message to the Server udp port
        //we received
        DatagramSocket socket_to_send = sendUdpMessage(
                new Message(Message.TYPE_OF_MESSAGE.SEND_SERVER_LIST, null), server_ip, server_udp_port
        );

        //read the message from the Server which contains the updated ServerList
        Message received_message = readUdpMessage(socket_to_send);

        //now store the alive server list that we received
        _alive_server_list = (ArrayList<ServerHeartBeatInfo>) received_message.attachment;

        //now try to connect to a server from this List
        boolean found_server = false;
        for(ServerHeartBeatInfo info : _alive_server_list){
            try {
                //try to connect to this Server
                _serverCommunicationSocket = new Socket(info.getIpAddress(), info.getPort());

                //if we arrived here everything went well and we can exit the loop
                found_server = true;
                break;

            //we were not able to connect to this Server and as such we must pass to the next one
            //until we reached the maximum number of servers
            }catch (IOException e){
                continue;
            }
        }

        //now that we established the connection to the Server we can start reading commands
        //from the keyboard and from the TCP socket that we created when we established a
        //connection with the server
        System.out.println("Established a connection with a Server");



        //if we didn't found a Server we throw the appropriate exception
        if(!found_server){
            throw new IOException("No server is availiable for connection");
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

    private Message readUdpMessage(DatagramSocket socket) throws IOException, ClassNotFoundException{
        //create the Datagram packet used to receive the message
        DatagramPacket received_message = new DatagramPacket(new byte[256], 256);

        //read the message from the socket
        socket.receive(received_message);

        //extract the message from it
        return (Message) new ObjectInputStream(new ByteArrayInputStream(received_message.getData())).readObject();
    }

    public void sendTCPMessage(Message message_to_send){
        try {
            OutputStream output_stream = _serverCommunicationSocket.getOutputStream();
            ObjectOutputStream object_output_stream = new ObjectOutputStream(output_stream);
            object_output_stream.writeObject(message_to_send);


        }catch (IOException e){

        }
    }

    public Message readTCPMessage(){
        try {
            InputStream input_stream = _serverCommunicationSocket.getInputStream();
            ObjectInputStream object_input_stream = new ObjectInputStream(input_stream);
            return (Message) object_input_stream.readObject();

        }catch (ClassNotFoundException | IOException ignored){
            return null;
        }

    }



    public String login(String username, String password){
        User user_to_send = new User();
        user_to_send.username = username;

        //hash password using sha-256 algorithm
        user_to_send.password = Utils.hashString(password);

        Message message_to_send = new Message(Message.TYPE_OF_MESSAGE.LOGIN,user_to_send);

        //send the serialized user object to the server
        sendTCPMessage(message_to_send);

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





    public String Register(String username,String name,String password){
        //sha-256

        User new_user=new User(username,name,password);
        OutputStream outputStream= null;
        try {
            outputStream = _serverCommunicationSocket.getOutputStream();
            ObjectOutputStream objectOutputStream= new ObjectOutputStream(outputStream);
            Message register_Message= new Message(Message.TYPE_OF_MESSAGE.REGISTER,username);
            objectOutputStream.writeObject(register_Message);
            InputStream inputStream= _serverCommunicationSocket.getInputStream();
            ObjectInputStream objectInputStream= new ObjectInputStream(inputStream);
            Message register_Response= (Message) objectInputStream.readObject();
            if(register_Response.type_of_message.equals(Message.TYPE_OF_MESSAGE.REGISTER))
            switch ((Error_Messages)register_Response.attachment){
                case SUCESS -> {
                    this._user=new_user;
                    this._user.authenticated=true;
                    return Error_Messages.SUCESS.toString();
                }
                case INVALID_PASSWORD -> {return Error_Messages.INVALID_PASSWORD.toString();}
                case INVALID_USERNAME -> {return Error_Messages.INVALID_USERNAME.toString();}
                //default -> {return "Unknown error ocurred";}
            }
        } catch (IOException|ClassNotFoundException e) {
            throw new RuntimeException(e);

        }

    return "Unknown error ocurred";
    }


}
