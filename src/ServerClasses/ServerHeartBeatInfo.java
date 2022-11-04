package ServerClasses;

import java.io.Serializable;

public class ServerHeartBeatInfo implements Serializable, Comparable<ServerHeartBeatInfo>{
    private int port;
    private int database_version;
    private int work_load;
    private boolean availability;

    public ServerHeartBeatInfo(int port, int database_version, int work_load, boolean availability) {
        this.port = port;
        this.database_version = database_version;
        this.work_load = work_load;
        this.availability = availability;
    }


    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

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
        return String.format("Porto: %d\tDatabase version:%d\tWorkLoad:%dAvailability:%b",port,database_version,work_load,availability);
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
        if(obj instanceof ServerHeartBeatInfo)
            return false;

        //compare the port numbers
        return port == ((ServerHeartBeatInfo) obj).port;
    }
}
