import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;

public class ServerModel {

    private final ServerSocket tcp_socket;
    private int tcp_connections;

    private final ArrayList<ServerModel> alive_server_list = new ArrayList<>();

    private int assigned_tcp_port;

    public ServerModel() throws IOException {
        tcp_socket = new ServerSocket(0);
        assigned_tcp_port = tcp_socket.getLocalPort();
        System.out.println("Server created on port: " + assigned_tcp_port);
    }





    public int getTcpConnections() {
        return tcp_connections;
    }

    public void newTcpConnection() {
        this.tcp_connections++;
    }

    public ArrayList<ServerModel> getAliveServerList() {
        return alive_server_list;
    }

    public void addServerToAliveList(ServerModel server){
        alive_server_list.add(server); // check if server already in list ?
    }

    public int getAssignedTcpPort() {
        return assigned_tcp_port;
    }

    public void setAssignedTcpPort(int assigned_tcp_port) {
        this.assigned_tcp_port = assigned_tcp_port;
    }

    public ServerSocket getTcpSocket() {
        return tcp_socket;
    }

    public void closeTcpSocket() throws IOException {
        tcp_socket.close();
    }

}
