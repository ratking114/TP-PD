package shared;

import java.io.Serializable;
import java.util.List;

/**
 * ClientActivity is a simple class that serves to store the activity of a Client and the Client
 * itself. This class is intended to be used when a client looses the connection that it had to its
 * server and needs to connect to a new one, naturally the new Server does not know who the client is, what were
 * and what were they doing
 */
public class ClientActivity implements Serializable {

    public ClientActivity(User user, int show_id_of_seats_being_reserved, boolean is_looking_for_seats) {
        this.user = user;
        this.show_id_of_seats_being_reserved = show_id_of_seats_being_reserved;
        this.is_looking_for_seats = is_looking_for_seats;
    }

    /**
     * The user whom this ClientActivity says respect to
     */
    public User user;

    /**
     * The id of the show whose seats being reserved belong to
     */
    public int show_id_of_seats_being_reserved;

    /**
     * Indicates that this user was looking for seats
     */
    public boolean is_looking_for_seats;
}
