package server.Threads;

import server.ServerClasses.ServerHeartBeatInfo;
import server.ServerClasses.ServerModel;
import shared.Message;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

/**
 * ClientUdpListenerThread is the Thread that listens to client requests for connections in a
 * UDP port.
 */
public class ClientUdpListenerThread extends Thread {
    private ServerModel _server_model;

    public ClientUdpListenerThread(ServerModel server_model){
        _server_model = server_model;
    }

    @Override
    public void run() {
        //create the DatagramSocket used to read Client messages using the port contained in the
        //server model
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(_server_model.getUdpPort());
        } catch (SocketException e) {
            System.out.println(e);
        }

        //loop until we are stopped
        while(true){
            try {
                //read a message from the client
                DatagramPacket datagram_packet = readUdpMessage(socket);

                //send to it the list of updated Servers
                System.out.println("Sent to a client the updated list of servers!");
                ArrayList<ServerHeartBeatInfo> alive_server_list = null;
                synchronized (_server_model.getAliveServerList()) {
                    //create an alive server list that is ordered by workload and dont forget that
                    //it must include ourselves
                    alive_server_list = new ArrayList<>(_server_model.getAliveServerList());
                }
                alive_server_list.add(_server_model.getHeartbeat_info());
                alive_server_list.sort(ServerHeartBeatInfo::compareTo);

                //send this list to the Client
                sendUdpMessage(
                        new Message(Message.TYPE_OF_MESSAGE.ALIVE_SERVER_LIST, alive_server_list),
                        datagram_packet.getAddress(), datagram_packet.getPort()
                );
            }catch (IOException | ClassNotFoundException e){
                System.out.println(e);
            }
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


    private DatagramPacket readUdpMessage(DatagramSocket socket) throws IOException, ClassNotFoundException{
        //create the Datagram packet used to receive the message
        DatagramPacket received_message = new DatagramPacket(new byte[256], 256);

        //read the message from the socket
        socket.receive(received_message);

        //extract the message from it
        return received_message;
    }

    private Message extractMessageFromDatagramPacket(DatagramPacket datagram_packet) throws IOException, ClassNotFoundException {
        return (Message) new ObjectInputStream(new ByteArrayInputStream(datagram_packet.getData())).readObject();
    }
}
