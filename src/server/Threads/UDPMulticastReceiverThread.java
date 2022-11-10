package server.Threads;

import server.ServerClasses.Prepare;
import server.ServerClasses.ServerHeartBeatInfo;
import server.ServerClasses.ServerModel;
import shared.Message;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Date;

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
                DatagramPacket rcv_packet = new DatagramPacket(new byte[1024], 1024);
                serverModel.getMulticastSocket().receive(rcv_packet);

                ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(rcv_packet.getData(), 0,
                        rcv_packet.getLength()));
                Message received_message = (Message)in.readObject();

                if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.PREPARE){
                    System.out.println("We got a Prepare, hu-ray!");
                }

                if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.HEARTBEAT){
                    ServerHeartBeatInfo received_heartbeat = (ServerHeartBeatInfo) received_message.attachment;
                    //if the HeartBeat is not ours
                    if(!received_heartbeat.equals(serverModel.getHeartbeat_info())) {
                        //add it to the alive server list
                        synchronized (this.serverModel.getAliveServerList()) {
                            if (!this.serverModel.getAliveServerList().contains(received_heartbeat)) {
                                System.out.println("added one! " + received_heartbeat.toString());
                                this.serverModel.getAliveServerList().add(received_heartbeat);
                            }
                            //update the received HeartBeat time
                            this.serverModel.getAliveServerList().get(
                                    this.serverModel.getAliveServerList().indexOf(received_heartbeat)
                            ).setHeartbeatTime(new Date());
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

                        //send an HeartBeat with our available state
                        serverModel.sendHeartBeat();
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
                        serverModel.getDatabaseLock().acquire();
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
                    //set our prepare to null to indicate that we are not processing any PREPARE
                    current_prepare = null;

                    //free the server.database lock as we already updated it
                    serverModel.getDatabaseLock().release();

                    System.out.println("COMMIT received!");

                }

                else if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.ABORT){
                    //set our prepare to null to indicate that we are not processing any PREPARE
                    current_prepare = null;

                    //free the server.database lock as we already updated it
                    serverModel.getDatabaseLock().release();
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
