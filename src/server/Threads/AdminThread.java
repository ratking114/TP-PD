package server.Threads;

import shared.EventInfo;
import server.ServerClasses.Prepare;
import shared.SeatPrice;
import server.ServerClasses.ServerModel;
import shared.Message;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;


public class AdminThread extends Thread{
    private final ServerModel _server_model;



    public AdminThread(ServerModel server_model){
        _server_model = server_model;
    }


    @Override
    public void run() {
        try {
            Scanner stdin = new Scanner(System.in);
            while (true) {
                //print a prompt
                System.out.print("Admin> ");

                //read a command from the keyboard
                String command = stdin.nextLine();
                //terminate the execution of the Server
                if (command.equals("terminate")) {
                    //tell to the other servers that we are no longer available
                    this._server_model.getHeartbeat_info().setAvailability(false);
                    this._server_model.sendHeartBeat();


                    //get the database lock to ensure that no one is there
                    try {
                        _server_model.lockDatabase();
                        this._server_model.getEventDatabase().closeDatabaseConnection();
                    } catch (InterruptedException e) {
                        System.out.println(e);
                        e.printStackTrace();
                    }

                    //send the updated server list to the clients
                    this._server_model.sendUpdatedServerListToClients(false);

                    //now tell to all threads that are attending clients that we are no longer available
                    //and this is done by interrupting the threads
                    synchronized (this._server_model.getServiceClientThreads()){
                        for(ServiceClientThread service_client_thread : this._server_model.getServiceClientThreads()){
                            service_client_thread.interrupt();
                            service_client_thread.join();
                        }
                    }

                    this._server_model.getEventDatabase().closeDatabaseConnection();

                    //very ugly, we should have saved all the threads that we have and interrupt them but
                    //this suffices
                    System.exit(0);
                }

            }
        }catch (Exception e){
            System.out.println(e);
            e.printStackTrace();
        }
    }

}
