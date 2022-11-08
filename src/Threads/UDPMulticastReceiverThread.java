package Threads;

import ServerClasses.Prepare;
import ServerClasses.ServerHeartBeatInfo;
import ServerClasses.ServerModel;
import shared.Message;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPMulticastReceiverThread extends Thread {

    public enum MODE{
        /**
         * Base mode, in this mode we are only listening and processing HeartBeats.
         */
        HEARTBEAT_LISTENER,

        /**
         * We sent a Prepare to the network and as such we must ignore other Server Prepares.
         */
        PREPARE_SENT,

        /**
         * We received a Prepare and as such we are now waiting for a COMMMIT or ABORT.
         */
        PREPARE_RECEIVED
    }

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



                switch (serverModel.getUDPMulticastReceiverThreadMode()){
                    case HEARTBEAT_LISTENER -> processHeartBeat(received_message);


                }

                //System.out.println(rcv_packet.getAddress());
                //System.out.println(InetAddress.getLoopbackAddress());
                //System.out.println(InetAddress.getLoopbackAddress().equals(rcv_packet.getAddress()));


                if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.HEARTBEAT){
                    ServerHeartBeatInfo received_heartbeat = (ServerHeartBeatInfo) received_message.attachment;
                    //if the HeartBeat is not ours
                    if(received_heartbeat.getPort() != serverModel.getAssignedTcpPort()) {
                        //add it to the alive server list
                        if(!this.serverModel.getAliveServerList().contains(received_heartbeat)) {
                            System.out.println("added one! "+ received_heartbeat.toString());
                            this.serverModel.getAliveServerList().add(received_heartbeat);
                        }
                    }

                    //see if the HeartBeat we received contains a database version superior to ours
                    if( serverModel.getHeartbeat_info().isAvailable() && received_heartbeat.getDatabaseVersion() > serverModel.getHeartbeat_info().getDatabaseVersion()){
                        System.out.println("Outdated database version");

                        //set our state as unavailable
                        serverModel.getHeartbeat_info().setAvailability(false);

                        //send a heartbeat to the other servers with our unavailable state
                        serverModel.sendHeartBeat();

                        //get the updated database
                        serverModel.getUpdatedDatabaseFromPeer();

                        //send an HeartBeat with our available state
                        serverModel.sendHeartBeat();
                    }
                }


                else if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.PREPARE){
                    current_prepare = (Prepare) received_message.attachment;


                    //if we are in the mode PREPARE_SENT then we sent a prepare and we must ignore other
                    //servers prepares
                    if(serverModel.getUDPMulticastReceiverThreadMode() == MODE.PREPARE_SENT){}

                    else if(serverModel.getUDPMulticastReceiverThreadMode() == MODE.HEARTBEAT_LISTENER){
                        //change our mode to the PREPARE_RECEIVED mode
                        synchronized (serverModel){
                            serverModel.setUDPMulticastReceiverThreadMode(MODE.PREPARE_RECEIVED);
                        }

                        //send a confirmation to the server that sent the PREPARE in the port specified
                        //in the message
                        
                        //create the DatagramSocket to send the confirmation
                        DatagramSocket send_confirmation = new DatagramSocket();

                        //serialize the message and send it
                        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
                        ObjectOutputStream out = new ObjectOutputStream(bOut);
                        out.writeObject(
                                new Message(Message.TYPE_OF_MESSAGE.CONFIRMATION, 10)
                        );
                        DatagramPacket to_send = new DatagramPacket(
                                bOut.toByteArray(), 0, bOut.size(), rcv_packet.getAddress(),
                                current_prepare.answer_udp_port
                        );
                        send_confirmation.send(to_send);
                    }



                    //2º Prepare || COMMIT || ABORT || HeartBeat
                }

                else if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.COMMIT){
                    //update the database with the saved prepare instance
                    current_prepare=null;

                    synchronized (serverModel){
                        serverModel.setUDPMulticastReceiverThreadMode(MODE.HEARTBEAT_LISTENER);
                    }
                }

                else if(received_message.type_of_message == Message.TYPE_OF_MESSAGE.ABORT){
                    //if the ABORT says respect to the PREPARE we are processing
                    if(((Prepare) received_message.attachment).prepare_number == current_prepare.prepare_number ) {
                        //then we reset the current prepare as the operation was cancelled
                        current_prepare = null;

                        synchronized (serverModel){
                            serverModel.setUDPMulticastReceiverThreadMode(MODE.HEARTBEAT_LISTENER);
                        }
                    }
                }

            }
        } catch (IOException | ClassNotFoundException e) {
            //catch this nuts
        }
    }

    private void processHeartBeat(Message received_message){

    }

}
