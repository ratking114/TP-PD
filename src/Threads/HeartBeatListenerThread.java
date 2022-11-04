package Threads;

import ServerClasses.ServerHeartBeatInfo;
import ServerClasses.ServerModel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;

public class HeartBeatListenerThread extends Thread {

    private final ServerModel serverModel;

    public HeartBeatListenerThread(ServerModel serverModel) {
        this.serverModel = serverModel;
    }
    @Override
    public void run() {
        try {
            while (true) {
                DatagramPacket rcv_packet = new DatagramPacket(new byte[1024], 1024);
                serverModel.getMulticastSocket().receive(rcv_packet);

                //System.out.println(rcv_packet.getAddress());
                //System.out.println(InetAddress.getLoopbackAddress());
                //System.out.println(InetAddress.getLoopbackAddress().equals(rcv_packet.getAddress()));

                ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(rcv_packet.getData(), 0,
                        rcv_packet.getLength()));
                ServerHeartBeatInfo heartbeatInfo = (ServerHeartBeatInfo) in.readObject();


                if(heartbeatInfo.getPort() != serverModel.getAssignedTcpPort()) {




                    //print its contents
                    System.out.println(heartbeatInfo);
                }

            }
        } catch (IOException | ClassNotFoundException e) {
            //catch this nuts
        }
    }

}
