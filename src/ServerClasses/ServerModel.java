package ServerClasses;

import Threads.AdminThread;
import Threads.HeartBeatListenerThread;
import Threads.HeartbeatThread;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;

public class ServerModel {


    public static final String CLUSTER_GROUP_ADDRESS = "239.39.39.39";
    private static final int group_port =4004;

    private InetAddress cluster_group;
    private final ServerSocket tcp_socket;
    private final MulticastSocket multicast_socket;


    private final ArrayList<ServerModel> alive_server_list = new ArrayList<>();
    private final ServerHeartBeatInfo heartbeat_info;



    public ServerModel() throws IOException {
        multicast_socket = new MulticastSocket(group_port);
        joinServerClusterGroup();
        tcp_socket = new ServerSocket(0);
        System.out.println("ServerClasses.Server created on port: " + tcp_socket.getLocalPort());
        heartbeat_info  = new ServerHeartBeatInfo(tcp_socket.getLocalPort(),0,0,false);

        HeartbeatThread heartbeatThread = new HeartbeatThread(this);
        HeartBeatListenerThread heartBeatListenerThread = new HeartBeatListenerThread(this);

        heartbeatThread.start();
        heartBeatListenerThread.start();

        AdminThread adminThread = new AdminThread();
        adminThread.start();

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

    public ArrayList<ServerModel> getAliveServerList() {
        return alive_server_list;
    }

    public void addServerToAliveList(ServerModel server){
        alive_server_list.add(server); // check if server already in list ?
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
