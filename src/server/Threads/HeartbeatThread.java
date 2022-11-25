package server.Threads;

import server.ServerClasses.ServerModel;

import java.io.IOException;

public class HeartbeatThread extends Thread{

    private final ServerModel serverModel;

    public HeartbeatThread(ServerModel serverHeartBeatInfo) {
        this.serverModel = serverHeartBeatInfo;
    }

    public void run(){
        while (true) {
            try {
                serverModel.sendHeartBeat();
                Thread.sleep(3 * 1000);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
    }
}
