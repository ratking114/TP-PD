package server.Threads;

import server.ServerClasses.ServerModel;

import java.util.Date;

/**
 * MaintainServerListThread is the Thread that removes the inactive servers from the alive server list.
 */
public class MaintainServerListThread extends Thread {
    private final ServerModel _server_model;

    public MaintainServerListThread(ServerModel server_model){
        _server_model = server_model;
    }

    @Override
    public void run() {
        while(true){
            try {
                //sleep for 1 second
                Thread.sleep(1000);

                //remove the Servers that haven't sent a HeartBeat for 35 seconds
                synchronized (_server_model.getAliveServerList()) {
                    _server_model.getAliveServerList().removeIf(
                            server_info -> {
                                long current_time_miliseconds = new Date().getTime();
                                long received_heartbeat_time_mil = server_info.getHeartbeatTime().getTime();

                                System.out.printf("Time: %d\n", current_time_miliseconds - received_heartbeat_time_mil );
                                if(current_time_miliseconds - received_heartbeat_time_mil >= 10 * 1000){
                                    System.out.println("Removed: " + server_info);
                                    return true;
                                }
                                else{
                                    return false;
                                }
                            }
                    );
                }
            } catch (InterruptedException ignored) {}

        }
    }
}
