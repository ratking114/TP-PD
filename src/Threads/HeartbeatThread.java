package Threads;

import ServerClasses.ServerModel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;

public class HeartbeatThread extends Thread{

    private final ServerModel serverModel;

    public HeartbeatThread(ServerModel serverHeartBeatInfo) {
        this.serverModel = serverHeartBeatInfo;
    }


    public void run(){
        while (true) {
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            ObjectOutputStream out;
            try {
                out = new ObjectOutputStream(bOut);
                out.writeObject(serverModel.getHeartbeat_info());
                out.flush();
                //System.out.println( serverModel.getClusterGroup() );

                DatagramPacket packet = new DatagramPacket(bOut.toByteArray(), bOut.size(), serverModel.getClusterGroup()   , 4004);
                serverModel.getMulticastSocket().send(packet);
                Thread.sleep(3000);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
    }
}
