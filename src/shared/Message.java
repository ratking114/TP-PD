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
         * Type of message to indicate to a peer server that the sender is requesting its database.
         * The attachment associated with this message is an array of bytes that represents the database
         * contents.
         */
        TRANSFER_DATABASE

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
