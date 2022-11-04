package ServerClasses;

import Threads.AdminThread;
import Threads.HeartBeatListenerThread;
import Threads.HeartbeatThread;

import java.io.File;
import java.io.IOException;
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

    private final String database_file_name = "PD-2022-23-TP";

    private final ArrayList<ServerHeartBeatInfo> alive_server_list = new ArrayList<>();


    private final ServerHeartBeatInfo heartbeat_info;

    private final Path database_directory;


    public ServerModel(String database_directory) throws IOException {
        multicast_socket = new MulticastSocket(group_port);
        joinServerClusterGroup();
        tcp_socket = new ServerSocket(0);
        System.out.println("ServerClasses.Server created on port: " + tcp_socket.getLocalPort());
        heartbeat_info  = new ServerHeartBeatInfo(tcp_socket.getLocalPort(),0,0,false);
        this.database_directory = Path.of(database_directory);

        setupDatabase();
    }

    public void setupDatabase() {
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
                Files.copy(
                        Path.of(database_file_name),
                        Path.of(this.database_directory.getFileName() + File.separator + database_file_name)
                );
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
            ServerHeartBeatInfo server_to_fetch_database=servers_with_highest_database_version.stream().min(Comparator.comparingInt(ServerHeartBeatInfo::getWorkLoad)).get();

            //establish a TCP connection with that server to get the updated database


        }
    }//sexy nanda - King

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
