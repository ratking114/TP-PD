package shared;

import java.io.Serializable;

public class Message implements Serializable {
    /**
     * Each enum constant is a type of message that can be exchanged between clients/servers.
     * */
    public enum TYPE_OF_MESSAGE{
        /**
         * Type of message to indicate that the sender is a server
         */
        HELLO_I_AM_SERVER,

        /**
         * Type of message to indicate that the sender is a client
         */
        HELLO_I_AM_CLIENT,

        /**
         * Type of message to indicate to a peer server that the sender is requesting its server.database.
         * The attachment associated with this message is an array of bytes that represents the server.database
         * contents.
         */
        TRANSFER_DATABASE,

        LOGIN,

        REGISTER,

        EDIT_USER_DATA,

        HEARTBEAT,

        PREPARE,

        COMMIT,

        CONFIRMATION,

        ABORT,

        /**
         * Sent from the client to the server to ask for the list of servers that are alive.
         * The attachment of this Message is an ArrayList<ServerHeartBeatInfo>.
         */
        SEND_SERVER_LIST,

        /**
         * The list of servers that are alive and therefore that a client can connect to.
         */
        ALIVE_SERVER_LIST,

        GET_PAID_RESERVATION,

        GET_UNPAID_RESERVATION,

        GET_SHOWS_FILTERED,

        /**
         * Sent by a Client to the Server to tell to it that it is exiting
         */
        CLIENT_EXIT,

        GET_LIST_OF_ALL_SHOWS,
        DELETE_SHOW,
        LOGOUT,
        INSERT_SHOW_BY_FILE,
        MAKE_VISIBLE,
        /**
         * Message sent by a Server to its clients to signal that it went unavailable and the clients must try
         * to connect to another server. The attachment of this message is a list of all the available servers
         * ordered by their workload
         */
        UNAVAILABLE_SERVER
    }

    public Message(TYPE_OF_MESSAGE type_of_message, Object attachment){
        this.type_of_message = type_of_message;
        this.attachment = attachment;
    }

    /**
     * The type of this message.
     */
    public TYPE_OF_MESSAGE type_of_message;

    /**
     * The attachment of a Message is an additional object that contains more information
     * about the message itself.
     */
    public Object attachment;
}
