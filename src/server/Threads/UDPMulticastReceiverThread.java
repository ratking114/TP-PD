package server.Threads;

import server.ServerClasses.Prepare;
import server.ServerClasses.ServerHeartBeatInfo;
import server.ServerClasses.ServerModel;
import shared.Message;
import shared.SeatPrice;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Date;
import java.util.List;

public class UDPMulticastReceiverThread extends Thread {
    private final ServerModel serverModel;

    private Prepare current_prepare = null;

    public UDPMulticastReceiverThread(ServerModel serverModel) {
        this.serverModel = serverModel;
    }
    @Override
    public void run() {
        try {
            while (true) {
                DatagramPacket rcv_packet = new DatagramPacket(new byte[12000], 12000);
                serverModel.getMulticastSocket().receive(rcv_packet);

                ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(rcv_packet.getData(), 0,
                        rcv_packet.getLength()));
                Message received_message = (Message)in.readObject();


                if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.HEARTBEAT){
                    ServerHeartBeatInfo received_heartbeat = (ServerHeartBeatInfo) received_message.attachment;
                    //if the HeartBeat is not ours
                    if(!received_heartbeat.equals(serverModel.getHeartbeat_info())) {
                        //add it to the alive server list
                        synchronized (this.serverModel.getAliveServerList()) {
                            //we received a HeartBeat from a new Server
                            if (!this.serverModel.getAliveServerList().contains(received_heartbeat)) {
                                System.out.println("added one! " + received_heartbeat.toString());
                                this.serverModel.getAliveServerList().add(received_heartbeat);
                            }
                            //update the received HeartBeat time and the HeartBeat itself
                            received_heartbeat.setHeartbeatTime(new Date());
                            this.serverModel.getAliveServerList().set(
                                    this.serverModel.getAliveServerList().indexOf(received_heartbeat),
                                    received_heartbeat
                            );
                            //send a message to all the clients to tell that a new HeartBeat was received
                            this.serverModel.sendUpdatedServerListToClients(true);
                        }
                    }
                    //see if the HeartBeat we received contains a server.database version superior to ours
                    if( serverModel.getHeartbeat_info().isAvailable() && received_heartbeat.getDatabaseVersion() > serverModel.getHeartbeat_info().getDatabaseVersion()){
                        System.out.println("Outdated server.database version");

                        //set our state as unavailable
                        serverModel.getHeartbeat_info().setAvailability(false);

                        //send a heartbeat to the other servers with our unavailable state
                        serverModel.sendHeartBeat();

                        //get the updated server.database
                        serverModel.getUpdatedDatabaseFromPeer();
                    }
                }
                else if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.PREPARE){
                    System.out.println("We received a PREPARE!");

                    if(((Prepare) received_message.attachment).prepare_number == serverModel.generateNumberFromIPAndPort()){
                        System.out.println("Prepare sent by us, Ignore it!");
                        continue;
                    }


                    //if we are already processing a PREPARE then we shall ignore a different PREPARE
                    if(current_prepare != null && current_prepare.prepare_number != ((Prepare) received_message.attachment).prepare_number ){
                        continue;
                    }

                    //if we are not processing a PREPARE then start processing this one
                    else if(current_prepare == null){
                        System.out.println("First Time receiving it!");


                        //save the prepare we received
                        current_prepare = (Prepare) received_message.attachment;

                        //since an update in a server is happening and it can change our server.database too we must
                        //lock the server.database
                        serverModel.lockDatabase();
                    }

                    //send a confirmation to the server that sent the PREPARE in the port specified
                    //in the message

                    //create the DatagramSocket to send the confirmation
                    DatagramSocket send_confirmation = new DatagramSocket();

                    //serialize the message and send it
                    ByteArrayOutputStream bOut = new ByteArrayOutputStream();
                    ObjectOutputStream out = new ObjectOutputStream(bOut);
                    out.writeObject(
                            new Message(Message.TYPE_OF_MESSAGE.CONFIRMATION, current_prepare.prepare_number)
                    );
                    DatagramPacket to_send = new DatagramPacket(
                            bOut.toByteArray(), 0, bOut.size(), rcv_packet.getAddress(),
                            current_prepare.answer_udp_port
                    );
                    send_confirmation.send(to_send);

                    System.out.println("Sent the confirmation to port: " + current_prepare.answer_udp_port);

                }
                else if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.COMMIT){
                    //if this commit is from a PREPARE sent by us we must not perform any update as who sent
                    //the PREPARE is responsible for the update
                    if(!((Integer)received_message.attachment).equals(this.serverModel.generateNumberFromIPAndPort())) {
                        //update the database or in other words, execute the query received
                        this.serverModel.getEventDatabase().executeQuery(current_prepare.query);
                        this.serverModel.incrementDatabaseVersion();

                        //see if this PREPARE contained a notification
                        if(current_prepare.type_of_message != null){
                            System.out.println("Seats changed!");

                            List<SeatPrice> available_seats = this.serverModel.getEventDatabase().get_Available_Seats_No_lock(
                                    current_prepare.show_id
                            );

                            //send the update to all clients that are reserving seats from this show
                            synchronized (this.serverModel.getServiceClientThreads()){
                                for(ServiceClientThread service_client_thread : this.serverModel.getServiceClientThreads()){
                                    if(service_client_thread.get_show_Id_OF_Seat_Been_Chosen() == current_prepare.show_id && service_client_thread.is_Looking_For_Seats()){
                                        ServerModel.sendMessageViaChannel(
                                                new Message(Message.TYPE_OF_MESSAGE.SEATS_UPDATED, available_seats),
                                                service_client_thread.getReceiveCommandsSinkChannel()
                                        );
                                    }
                                }
                            }
                        }





                        //free the server.database lock as we already updated it
                        serverModel.unlockDatabase();
                    }

                    //set our prepare to null to indicate that we are not processing any PREPARE
                    current_prepare = null;

                    System.out.println("COMMIT received! Database was updated");

                }

                else if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.ABORT){
                    //set our prepare to null to indicate that we are not processing any PREPARE
                    current_prepare = null;

                    //free the server.database lock as we already updated it
                    serverModel.unlockDatabase();
                    System.out.println("THe transaction was aborted, git gud bozo");

                }
            }
        } catch (IOException | ClassNotFoundException | InterruptedException e) {
            System.out.println(e);
        }
    }

    private void processHeartBeat(Message received_message){

    }

}
