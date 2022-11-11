package shared;

public class Reservation {



    public int id;
    public String date_hour;
    public boolean paid_for;
    public int user_id;
    public int event_id;

    public Reservation(int id, String date_hour, boolean paid_for, int user_id, int event_id) {
        this.id = id;
        this.date_hour = date_hour;
        this.paid_for = paid_for;
        this.user_id = user_id;
        this.event_id = event_id;
    }

    @Override
    public String toString() {
        return String.format("Event: %d\tDate:%s",event_id,date_hour);
    }
}
