package shared;

import java.io.Serializable;
import java.util.List;

public class ReservationViewmodel implements Serializable {

    public ReservationViewmodel(EventInfo event, Reservation reservation, List<SeatPrice> reserved_seats, Error_Messages error_message) {
        this.event = event;
        this.reservation = reservation;
        this.reserved_seats = reserved_seats;
        this.error_message = error_message;
    }
    public EventInfo event;
    public Reservation reservation;
    public List<SeatPrice> reserved_seats;
    public Error_Messages error_message;
}
