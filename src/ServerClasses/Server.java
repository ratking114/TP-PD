package ServerClasses;

import java.io.*;
import Threads.TcpListeningThread;




public class Server {
    public static void main(String[] args) throws IOException {
        ServerModel server_model = new ServerModel(args[0]);


        TcpListeningThread accept_clients_thread = new TcpListeningThread(server_model);
        //accept_clients_thread.setDaemon(true);
        accept_clients_thread.start();
    }
}