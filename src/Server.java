import java.io.*;
import java.net.*;
import java.util.Iterator;

class TcpListeningThread extends Thread{
    private final ServerModel server_model;

    TcpListeningThread(ServerModel server_model) {
        this.server_model = server_model;
    }

    public void run() {
        try {
            while (true) {
                Socket connected_client = server_model.getTcpSocket().accept();
                server_model.newTcpConnection();

                System.out.println("New client connected, current number of connections = " + server_model.getTcpConnections());
                // lançar thread para fazer handle do client ?
            }
            //server_model.closeTcpSocket();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}



public class Server {


    public static String CLUSTER_GROUP_ADDRESS = "239.39.39.39";
    public static int CLUSTER_GROUP_IP = 4004;


    public static void debugSendMulticast(MulticastSocket multicast_socket,InetAddress cluster_group) throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bOut);

        out.writeObject("Hello im a new server");
        out.flush();

        DatagramPacket packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), cluster_group, 4004);

        multicast_socket.send(packet);

    }

    public static void debugReceiveMulticast(MulticastSocket multicast_socket,InetAddress cluster_group) throws IOException, ClassNotFoundException {
        DatagramPacket rcv_packet = new DatagramPacket(new byte[1024], 1024);
        multicast_socket.receive(rcv_packet);

        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(rcv_packet.getData(), 0,
                rcv_packet.getLength()));
        String heartbeatInfo = (String) in.readObject();

        System.out.println(heartbeatInfo);
    }
    public static void joinServerClusterGroup(String ip, int port) {
        try {
            MulticastSocket multicast_socket = new MulticastSocket(port);
            InetAddress cluster_group = InetAddress.getByName(ip);
            NetworkInterface network_interface;
            for (Iterator<NetworkInterface> it = NetworkInterface.getNetworkInterfaces().asIterator(); it.hasNext(); ) {
                network_interface = it.next();

                if(network_interface.supportsMulticast()){

                    multicast_socket.joinGroup(new InetSocketAddress(ip,port),network_interface);
                    break;
                }


            }

            Thread.sleep(3000);

            debugSendMulticast(multicast_socket,cluster_group);


            debugReceiveMulticast(multicast_socket,cluster_group);


        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }


    }
    public static void main(String[] args) throws IOException {

        ServerModel server_model = new ServerModel();

        TcpListeningThread accept_clients_thread = new TcpListeningThread(server_model);
        //accept_clients_thread.setDaemon(true);
        accept_clients_thread.start();



        joinServerClusterGroup(CLUSTER_GROUP_ADDRESS,CLUSTER_GROUP_IP);




    }
}