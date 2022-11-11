package server.ServerClasses;

import server.Threads.*;
import server.database.EventDatabase;
import shared.Message;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Semaphore;

public class ServerModel {
    /**
     * The address of the cluster group. This is a multicast address used to communicate with our
     * peer servers.
     */
    public static final String CLUSTER_GROUP_ADDRESS = "239.39.39.39";

    /**
     * The port of our group, which is the port on wich we will listen for multicasts.
     */
    private static final int GROUP_PORT = 4004;

    /**
     * The IP address of our cluster group.
     */
    private InetAddress cluster_group;


    /**
     * The ServerSocket used to accept connections. It is trough this ServerSocket that we will accept clients
     * and other servers.
     */
    private final ServerSocket tcp_socket;

    /**
     * The socket used for Multicasting purposes.
     */
    private final MulticastSocket multicast_socket;


    /**
     * The list of Servers that are alive. This list is composed of ServerHeartBeatInfo's and it contains all
     * information that we need to know about a server.
     */
    private final ArrayList<ServerHeartBeatInfo> alive_server_list = new ArrayList<>();

    /**
     * Our HeartBeatInfo to send to the other servers. The thread that sends HeartBeats shall send this
     * data member by multicast to the other servers.
     */
    private ServerHeartBeatInfo our_heartbeat_info;

    /**
     * The Path to the server.database directory wich is choosen by the user.
     */
    private final Path database_directory;

    /**
     * The path to the server.database file name .
     */
    private final Path database_file_name;

    /**
     * The path to the original server.database file name which is the empty server.database file from wich one can
     * construct an empty server.database by copying this file's contents.
     */
    private final Path original_database_file_name;

    /**
     * The name of the file where the server.database version is
     */
    private final Path database_version_file_name;
    /**
     * The ProducerConsumerBuffer used to communicate prepares to the TransactionHandlerThread
     */
    private final ProducerConsumerBuffer _producerConsumerBuffer;

    /**
     * The Semaphore that controls Database accesses, this Semaphore is a binary one that shall serve
     * as a simple mutex.
     */
    private final Semaphore database_lock;

    /**
     * The server.database that contains the events.
     */
    private EventDatabase event_database;

    /**
     * The UDP port used to read Client requests for connections.
     */
    private int udp_port;

    /**
     * The Threads that Service clients are stored here in order to access the client sockets to inject
     * messages there.
     */
    private ArrayList<ServiceClientThread> _service_clients_threads;

    public ServerModel(String database_directory, int udp_port) throws IOException {
        //create the MulticastSocket and join the group of Servers
        multicast_socket = new MulticastSocket(GROUP_PORT);
        joinServerClusterGroup();

        //the UDP port used to read Client requests for connections.
        this.udp_port = udp_port;

        //create the ServerSocket used to accept clients and servers
        tcp_socket = new ServerSocket(0);
        System.out.println("server.ServerClasses.Server created on port: " + tcp_socket.getLocalPort());


        //create our HeartBeatInfo
        our_heartbeat_info = new ServerHeartBeatInfo(
                tcp_socket.getLocalPort(), 0, 0, false, InetAddress.getLocalHost()
        );

        //save the server.database directory which was supplied by the user
        this.database_directory = Path.of(database_directory);

        //save the server.database file name
        this.database_file_name = Path.of(database_directory + File.separator + "PD-2022-23-TP.db");

        //save the original server.database file name
        original_database_file_name = Path.of("./Database/default/PD-2022-23-TP.db");

        //save the name of the file where the server.database version is
        database_version_file_name = Path.of(database_directory + File.separator + "version.txt");

        //setup the server.database which involves creating the file our fetching it from another peer.
        setupDatabase();

        //create the server.database lock
        this.database_lock = new Semaphore(1);

        //create the ProducerConsumerBuffer and the Thread that takes requests from it
        _producerConsumerBuffer = new ProducerConsumerBuffer();
        new TransactionHandler(this).start();

        //create the Thread that reads commands from the keyboard
        new AdminThread(this).start();

        //start the Thread that listens for incoming client connections
        new ClientUdpListenerThread(this).start();

        //create the Database
        try {
            this.event_database = new EventDatabase(this.database_file_name);
        }catch (SQLException ignored){}
    }



    public int getUdpPort(){
        return udp_port;
    }

    public Path getDatabaseFileName(){
        return this.database_file_name;
    }

    /**
     *
     * @throws IOException in case an IO error ocurs
     */
    public void setupDatabase() throws IOException {
        //start the thread that listens to heartbeats and wait 30 seconds for heartbeats
        UDPMulticastReceiverThread heartBeatListenerThread = new UDPMulticastReceiverThread(this);
        heartBeatListenerThread.start();
        try {
            System.out.println("Waiting for other Server HeartBeat's");
            Thread.sleep(10 * 1000);
        } catch (InterruptedException ignored) {}
        //now that we waited 30seconds we might have received server HeartBeats and as such we must see
        //if another server has a server.database version superior to ours or if we need to create our own server.database
        //file

        //if the directory does not exists then we must create it and the file with the server.database
        //version too
        if(!Files.exists(database_directory)){
            //create the server.database directory that the user specified
            Files.createDirectory(this.database_directory);

            //create the file that contains the server.database version
            Files.createFile(database_version_file_name);
            setDatabaseVersionToFile();
        }
        //the directory exists and as such we can set our server.database version from the file
        else{
            our_heartbeat_info.setDatabaseVersion(getDatabaseVersionFromFile());
        }

        //if we are the only active server and the server.database directory does not exist then we shall
        //create our own directory and place our server.database file there by copying from the root server.database file
        if(alive_server_list.isEmpty()) {
            try {
                //create the server.database file in the directory just created and copy the base
                //server.database file to it so we have an empty functional server.database
                System.out.println( "Original directory: "+original_database_file_name);
                System.out.println("Copy directory "+database_file_name);
                Files.copy(original_database_file_name, database_file_name, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        //if we have fellow servers, and they have superior server.database versions lets fetch a server.database from one of them
        else{

            System.out.println("Server list not empty");
            getUpdatedDatabaseFromPeer();
        }

        this.our_heartbeat_info.setAvailability(true);

        //now that we have a valid server.database we can start sending HeartBeats
        new HeartbeatThread(this).start();
        new MaintainServerListThread(this).start();

        System.out.println("We got a valid server.database");
    }

    /**
     * Fetches the updated server.database from the server to which the supplied ServerHeartBeatInfo
     * says respect to.
     */
    public void getUpdatedDatabaseFromPeer() throws IOException{
        System.out.println("Fetching server.database from peer");

        //get the servers with the highest server.database version and from those servers get the server that
        //has the least workload
        List<ServerHeartBeatInfo> servers_with_highest_database_version = alive_server_list.stream().filter(server->server.getDatabaseVersion()==alive_server_list.stream().max(
                Comparator.comparingInt(ServerHeartBeatInfo::getDatabaseVersion)).get().getDatabaseVersion()
        ).toList();
        ServerHeartBeatInfo server_to_fetch_database = servers_with_highest_database_version.stream().min(Comparator.comparingInt(ServerHeartBeatInfo::getWorkLoad)).get();

        //see if our server.database version is inferior to this server's and if it is lets fetch it
        if(this.our_heartbeat_info.getDatabaseVersion() > server_to_fetch_database.getDatabaseVersion()) {
            return;
        }

        //establish a TCP connection with that server to get the updated server.database
        Socket socket = new Socket(server_to_fetch_database.getIpAddress(), server_to_fetch_database.getPort());


        //send it a message indicating that we are a server
        serializeMessage(new Message(Message.TYPE_OF_MESSAGE.HELLO_I_AM_SERVER, null), socket.getOutputStream());

        //Stop heartbeat thread by signaling on our HeartBeatInfo that we are inactive
        this.our_heartbeat_info.setAvailability(false);

        //read the message from the peer Server
        Message received_message = null;
        try {
            received_message = deserializeMessage(socket.getInputStream());
        }catch(ClassNotFoundException ignored){}

        //write the server.database contents to our server.database file
        //transferto internaly decides the cycle and does the transfer internally
        Files.createDirectories(this.database_directory);

        //if the file already exists this throws an exception and we can ignore it
        try {
            Files.createFile(database_version_file_name);
        }catch (IOException ignored){}

        if(!Files.exists(this.database_file_name)) {
            Files.createFile(this.database_file_name);
        }
        //if the file does exist then we must clear it by deleting it and creating it again
        else{
            Files.delete(this.database_file_name);
            Files.createFile(this.database_file_name);
        }

        //read the file sent over the network to our local server.database file and save the server.database
        //version
        new ByteArrayInputStream(((byte[])received_message.attachment)).transferTo(
                new FileOutputStream(this.database_file_name.toFile())
        );
        our_heartbeat_info.setDatabaseVersion(server_to_fetch_database.getDatabaseVersion());
        setDatabaseVersionToFile();

        //close the socket as the transfer is done
        socket.close();

        //we now have a valid server.database so we can set our availiability to true
        this.our_heartbeat_info.setAvailability(true);
        System.out.println("Transfer Done");

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
        return GROUP_PORT;
    }
    public ServerHeartBeatInfo getHeartbeat_info() {
        return our_heartbeat_info;
    }

    public MulticastSocket getMulticastSocket() {
        return multicast_socket;
    }

    public int getTcpConnections() {
        return our_heartbeat_info.getWorkLoad();
    }

    synchronized public void newTcpConnection() {
        this.our_heartbeat_info.addWorkLoad();
    }

    synchronized public void lostTcpConnection() {
        this.our_heartbeat_info.reduceWorkLoad();
    }

    public EventDatabase getEventDatabase(){
        return this.event_database;
    }

    public ArrayList<ServerHeartBeatInfo> getAliveServerList(){
        return this.alive_server_list;
    }

    public int getAssignedTcpPort() {
        return our_heartbeat_info.getPort();
    }

    public void setAssignedTcpPort(int assigned_tcp_port) {
        this.our_heartbeat_info.setPort(assigned_tcp_port);
    }

    public ArrayList<ServiceClientThread> getServiceClientThreads(){
        return _service_clients_threads;
    }

    /**
     * Removes a ServiceClientThread from the list of ServiceClientThreads that the Server holds.
     * This method uses the client socket of the Thread to perform the removal.
     * @param to_remove the ServiceClientThread that is to be removed.
     */
    public void removeServiceClientThread(ServiceClientThread to_remove){
        synchronized (_service_clients_threads){
            _service_clients_threads.removeIf(
                    (ServiceClientThread thread) -> {
                        if(to_remove.getClientSocket() == thread.getClientSocket()){
                            System.out.println("Removed a ServiceClientThread");
                            return true;
                        }
                        else{
                            return false;
                        }
                    }
            );
        }

        //now that a service clients Thread was removed we must send to all the clients the updated list
        //of servers


    }

    /**
     * Sends the updated server list to all the clients that are connected
     */
    public void sendUpdatedServerListToClients(){

    }


    public ServerSocket getTcpSocket() {
        return tcp_socket;
    }

    public void closeTcpSocket() throws IOException {
        tcp_socket.close();
    }

    public int generateNumberFromIPAndPort(){
        return getHeartbeat_info().getPort() * getHeartbeat_info().getIpAddress().hashCode();
    }

    private int getDatabaseVersionFromFile() throws FileNotFoundException {
        try(Scanner file_scanner = new Scanner(new FileInputStream(database_version_file_name.toFile())) ){
            return file_scanner.nextInt();
        }
        catch (NoSuchElementException|IllegalStateException e)
        {
            return -1;
        }
    }

    private void setDatabaseVersionToFile() throws IOException {
        try(PrintWriter to_write = new PrintWriter(database_version_file_name.toFile())){
            to_write.write( Integer.toString(this.our_heartbeat_info.getDatabaseVersion()) );
        }
    }

    public void joinServerClusterGroup() {
        try {
            this.cluster_group= InetAddress.getByName(CLUSTER_GROUP_ADDRESS);
            NetworkInterface network_interface;
            for (Iterator<NetworkInterface> it = NetworkInterface.getNetworkInterfaces().asIterator(); it.hasNext(); ) {
                network_interface = it.next();

                if(network_interface.supportsMulticast()){

                    multicast_socket.joinGroup(new InetSocketAddress(CLUSTER_GROUP_ADDRESS,GROUP_PORT),network_interface);
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


    public void sendHeartBeat() throws IOException {
        sendUDPBroadcast(new Message(Message.TYPE_OF_MESSAGE.HEARTBEAT, this.getHeartbeat_info()));
    }

    public void sendMessage(Message message_to_send) throws IOException {
        System.out.println("About to send prepare");
        sendUDPBroadcast(message_to_send);
        System.out.println("Message sent\nola ola");
    }
    private void sendUDPBroadcast(Message message_to_send) throws IOException
    {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bOut);
        out.writeObject(message_to_send);

        DatagramPacket packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), this.getClusterGroup(), GROUP_PORT);
        this.getMulticastSocket().send(packet);
    }


    public InetAddress getClusterGroup() {
        return cluster_group;
    }

    public ProducerConsumerBuffer getProducerConsumerBuffer() {
        return _producerConsumerBuffer;
    }

    public Semaphore getDatabaseLock(){
        return this.database_lock;
    }
}