package ServerClasses;

import Threads.HeartBeatListenerThread;
import shared.Message;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ServerModel {
    public static final String CLUSTER_GROUP_ADDRESS = "239.39.39.39";
    private static final int group_port = 4004;

    private InetAddress cluster_group;
    private final ServerSocket tcp_socket;
    private final MulticastSocket multicast_socket;


    private final ArrayList<ServerHeartBeatInfo> alive_server_list = new ArrayList<>();


    private final ServerHeartBeatInfo heartbeat_info;

    /**
     * The Path to the database directory
     */
    private final Path database_directory;

    /**
     * The path to the database file name.
     */
    private final Path database_file_name;

    /**
     * The path to the original database file name which is the empty database file from wich one can
     * construct an empty database by copying this file's contents.
     */
    private final Path original_database_file_name;


    public ServerModel(String database_directory) throws IOException {
        multicast_socket = new MulticastSocket(group_port);
        joinServerClusterGroup();
        tcp_socket = new ServerSocket(0);
        System.out.println("ServerClasses.Server created on port: " + tcp_socket.getLocalPort());
        heartbeat_info  = new ServerHeartBeatInfo(tcp_socket.getLocalPort(),0,0,false);

        //save the database directory
        this.database_directory = Path.of(database_directory);

        //save the database file name
        this.database_file_name = Path.of(database_directory + File.separator + "PD-2022-23-TP.db");

        //save the original database file name
        original_database_file_name = Path.of("../Database/default/PD-2022-23-TP.db");

        setupDatabase();
    }

    public void setupDatabase() throws IOException {
        //start the thread that listens to heartbeats and wait 30 seconds for heartbeats
        HeartBeatListenerThread heartBeatListenerThread = new HeartBeatListenerThread(this);
        heartBeatListenerThread.start();
        try {
            Thread.sleep(30 * 1000);
        } catch (InterruptedException ignored) {}

        //now that we have(or can have) servers on our list lets process the information


        if(alive_server_list.isEmpty() && !Files.exists(database_directory)) {
            try {
                //we must create the database file in the directory that the user specified
                Files.createDirectory(this.database_directory);

                //create the database file in the directory just created and copy the base
                //database file to it
                Files.copy(original_database_file_name, database_file_name);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else if(!alive_server_list.isEmpty()){
            //get the servers with the highest database version and from those servers get the server that
            //has the least workload
            List<ServerHeartBeatInfo> servers_with_highest_database_version = alive_server_list.stream().filter(server->server.getDatabaseVersion()==alive_server_list.stream().max(
                    Comparator.comparingInt(ServerHeartBeatInfo::getDatabaseVersion)).get().getDatabaseVersion()
            ).toList();
            ServerHeartBeatInfo server_to_fetch_database = servers_with_highest_database_version.stream().min(Comparator.comparingInt(ServerHeartBeatInfo::getWorkLoad)).get();

            //get the updated database from the server
            getUpdatedDatabaseFromPeer(server_to_fetch_database);
        }
    }

    /**
     * Fetches the updated database from the server to which the supplied ServerHeartBeatInfo
     * says respect to.
     * @param server_to_fetch_database the ServerHeartBeatInfo of the Server from which the
     * database will be fetched.
     */
    public void getUpdatedDatabaseFromPeer( ServerHeartBeatInfo server_to_fetch_database) throws IOException{
        //establish a TCP connection with that server to get the updated database
        Socket socket = new Socket("localhost", server_to_fetch_database.getPort());

        //send it a message indicating that we are a server
        serializeMessage(new Message(Message.TYPE_OF_MESSAGE.HELLO_I_AM_SERVER, null), socket.getOutputStream());

        //Stop heartbeat thread by signaling on our HeartBeatInfo that we are inactive
        this.heartbeat_info.setAvailability(false);

        //send the message to the peer server with our request
        serializeMessage(new Message(Message.TYPE_OF_MESSAGE.TRANSFER_DATABASE, null), socket.getOutputStream());

        //read the message from the peer Server
        Message received_message = null;
        try {
            received_message = deserializeMessage(socket.getInputStream());
        }catch(ClassNotFoundException ignored){}

        //write the database contents to our database file
        new ByteArrayInputStream( ((byte[])received_message.attachment)).transferTo(
                new FileOutputStream(this.database_file_name.toFile())
        );

        //close the socket as the transfer is done
        socket.close();
    }

    /**
     * Serializes and writes the serialized Message to the supplied OutputStream.
     * @param message_to_serialize the Message to serialize.
     * @param to_write the OutputStream to which the serialized message shall be written
     */
    static public void serializeMessage(Message message_to_serialize, OutputStream to_write) throws IOException{
        new ObjectOutputStream(to_write).writeObject(message_to_serialize);
    }

    /**
     * Deserializes a Message from an InputStream and returns such Message.
     * @param message_to_deserialize_stream the InputStream where the Message is written.
     * @return the deserialized Message
     * @throws IOException
     * @throws ClassNotFoundException
     */
    static public Message deserializeMessage(InputStream message_to_deserialize_stream) throws IOException, ClassNotFoundException{
        return (Message) new ObjectInputStream(message_to_deserialize_stream).readObject();
    }

    public static int getGroupPort() {
        return group_port;
    }
    public ServerHeartBeatInfo getHeartbeat_info() {
        return heartbeat_info;
    }

    public MulticastSocket getMulticastSocket() {
        return multicast_socket;
    }

    public int getTcpConnections() {
        return heartbeat_info.getWorkLoad();
    }

    public void newTcpConnection() {
        this.heartbeat_info.addWorkLoad();
    }

    public ArrayList<ServerHeartBeatInfo> getAliveServerList(){
        return this.alive_server_list;
    }

    public int getAssignedTcpPort() {
        return heartbeat_info.getPort();
    }

    public void setAssignedTcpPort(int assigned_tcp_port) {
        this.heartbeat_info.setPort(assigned_tcp_port);
    }

    public ServerSocket getTcpSocket() {
        return tcp_socket;
    }

    public void closeTcpSocket() throws IOException {
        tcp_socket.close();
    }

    public void joinServerClusterGroup() {
        try {
            this.cluster_group= InetAddress.getByName(CLUSTER_GROUP_ADDRESS);
            NetworkInterface network_interface;
            for (Iterator<NetworkInterface> it = NetworkInterface.getNetworkInterfaces().asIterator(); it.hasNext(); ) {
                network_interface = it.next();

                if(network_interface.supportsMulticast()){

                    multicast_socket.joinGroup(new InetSocketAddress(CLUSTER_GROUP_ADDRESS,group_port),network_interface);
                    break;
                }


            }

            //Thread.sleep(3000);

            //debugSendMulticast(server.getMulticastSocket(),cluster_group);


            //debugReceiveMulticast(server.getMulticastSocket(),cluster_group);


        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    public InetAddress getClusterGroup() {
        return cluster_group;
    }
}
