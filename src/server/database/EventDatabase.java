package server.database;

import shared.Error_Messages;
import shared.Message;
import shared.User;
import shared.UserViewModel;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * EventDatabase is the server.database that stores the events and all information
 * associated with them.
 */
public class EventDatabase {
    /**
     * The connection to the server.database.
     */
    private final Connection database_connection;

    public EventDatabase(Path database_file_name) throws SQLException{
        //create the DatabaseConnection
        this.database_connection = DriverManager.getConnection(
                "jdbc:sqlite:" + database_file_name
        );
    }

    public Error_Messages login(Message received_login){
        UserViewModel user_viewmodel = new UserViewModel();
        User login_user = (User) received_login.attachment;

        //TODO(see if the username and password match the ones in the DB i have no idea hhow plase help :( )

        return Error_Messages.SUCESS;

    }

    /**
     *This function will try to register a new user with the info inputed on the client
     * @param received_register protocol message sent by the client containing the type of request and corresponding attachment, a user in this instance
     * @return Error_Messages which contains the result of the command
     */
    public Error_Messages Register(Message received_register)
    {
        User user_To_Register=(User) received_register.attachment;
        //We will only allway names between 2 and 128 characters
        if(user_To_Register.name.length()<2||user_To_Register.name.length()>128)
            return Error_Messages.INVALID_USERNAME;

        //The password must contain more than 6 characters
        if(user_To_Register.password.length()<6)
            return Error_Messages.INVALID_PASSWORD;
        //TODO()
        return Error_Messages.SUCESS;
    }
}
