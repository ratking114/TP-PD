package server.ServerClasses;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Date;

public class ServerHeartBeatInfo implements Serializable, Comparable<ServerHeartBeatInfo>{
    private int port;
    private int database_version;
    private int work_load;
    private boolean availability;
    private Date heartbeatTime;

    private InetAddress ip_address;

    public ServerHeartBeatInfo(int port, int database_version, int work_load, boolean availability,InetAddress ip_address, Date heartbeatTime) {
        this.port = port;
        this.database_version = database_version;
        this.work_load = work_load;
        this.availability = availability;
        this.heartbeatTime = heartbeatTime;
        this.ip_address=ip_address;
    }
    public ServerHeartBeatInfo(int port, int database_version, int work_load, boolean availability, InetAddress ip_address) {
        this.port = port;
        this.database_version = database_version;
        this.work_load = work_load;
        this.availability = availability;
        this.ip_address = ip_address;
    }


    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public InetAddress getIpAddress(){return ip_address;}

    public int getDatabaseVersion() {
        return database_version;
    }

    public void setDatabaseVersion(int database_version) {
        this.database_version = database_version;
    }

    public int getWorkLoad() {
        return work_load;
    }

    public void addWorkLoad() {
        this.work_load++;
    }
    public void reduceWorkLoad() {
        this.work_load--;
    }

    public boolean isAvailable() {
        return availability;
    }

    public void setAvailability(boolean availability) {
        this.availability = availability;
    }


    @Override
    public String toString() {
        return String.format("Porto: %d\tIP address: %s\tDatabase version:%d\tWorkLoad:%d\tAvailability:%b", port, ip_address, database_version,work_load,availability);
    }

    public Date getHeartbeatTime() {
        return heartbeatTime;
    }

    public void setHeartbeatTime(Date heartbeatTime) {
        this.heartbeatTime = heartbeatTime;
    }


    @Override
    public int compareTo(ServerHeartBeatInfo o) {
        return getWorkLoad() - o.getWorkLoad();
    }

    @Override
    public boolean equals(Object obj) {
        //compare the TCP Port number
        if(!(obj instanceof ServerHeartBeatInfo))
            return false;

        //compare the port numbers
        return port == ((ServerHeartBeatInfo) obj).port && ip_address.equals(((ServerHeartBeatInfo)obj).getIpAddress());
    }
}
