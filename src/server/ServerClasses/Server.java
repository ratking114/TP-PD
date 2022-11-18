package server.ServerClasses;

import java.io.*;
import java.nio.file.Path;
import java.sql.SQLException;

import server.Threads.TcpListeningThread;
import server.database.EventDatabase;


public class Server {
    public static void main(String[] args) throws IOException {
        //create the ServerModel passing it our command line arguments that are the database file name
        //and the udp port where the Server has to wait for client requests to connect
        ServerModel server_model = new ServerModel(args[0], Integer.valueOf(args[1]));


        //create the TCPListeningThread which is the Thread that listens to incoming TCP connections
        TcpListeningThread accept_clients_thread = new TcpListeningThread(server_model);
        accept_clients_thread.start();
    }
}