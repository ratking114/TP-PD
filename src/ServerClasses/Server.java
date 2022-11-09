package ServerClasses;

import java.io.*;
import Threads.TcpListeningThread;



public class Server {
    public static void main(String[] args) throws IOException {
        //create the ServerModel passing it our command line arguments
        ServerModel server_model = new ServerModel(args[0]);


        //create the TCPListeningThread which is the Thread that listens to incoming TCP connections
        TcpListeningThread accept_clients_thread = new TcpListeningThread(server_model);
        accept_clients_thread.start();
    }
}