package shared;

import java.io.Serializable;

public class ReservationViewmodel implements Serializable {

    public ReservationViewmodel(EventInfo event, Reservation reservation, Error_Messages error_message){
        this.event = event;
        this.reservation = reservation;
        this.error_message = error_message;
    }

    public EventInfo event;
    public Reservation reservation;
    public Error_Messages error_message;
}
