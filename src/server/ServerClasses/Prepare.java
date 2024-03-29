package server.ServerClasses;

import shared.Message;

import java.io.Serializable;

/**
 * Prepare is the message that is sent when a Server needs to update its server.database.
 */
public class Prepare implements Serializable {
    public Prepare(String query, int prepare_number){
        this.query = query;
        this.prepare_number = prepare_number;
    }

    /**
     * The number that identifies this Prepare message
     */
    public int prepare_number;

    /**
     * The query that must be run to update the server.database.
     */
    public String query;

    /**
     * The udp port number where the sender will wait for confirmation messages.
     */
    public int answer_udp_port;

    //SEATS CHANGED
    public Message.TYPE_OF_MESSAGE type_of_message;

    public int show_id;
}
