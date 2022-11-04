package ServerClasses;

import java.io.*;
import java.net.*;

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
    public static void main(String[] args) throws IOException {


        ServerModel server_model = new ServerModel();










        TcpListeningThread accept_clients_thread = new TcpListeningThread(server_model);
        //accept_clients_thread.setDaemon(true);
        accept_clients_thread.start();
    }
}