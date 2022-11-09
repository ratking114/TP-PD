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
            try {
                serverModel.sendHeartBeat();
                Thread.sleep(3000);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
    }
}
