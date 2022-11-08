package ServerClasses;

/**
 * Prepare is the message that is sent when a Server needs to update its database.
 */
public class Prepare {
    public Prepare(String query, int prepare_number){
        this.query = query;
        this.prepare_number = prepare_number;
    }

    /**
     * The number that identifies this Prepare message
     */
    public int prepare_number;

    /**
     * The query that must be run to update the database.
     */
    public String query;

    /**
     * The udp port number where the sender will wait for confirmation messages.
     */
    public int answer_udp_port;
}
