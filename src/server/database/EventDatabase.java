package server.database;

import shared.Error_Messages;
import shared.Message;
import shared.User;
import shared.UserViewModel;

import java.nio.file.Path;
import java.sql.*;

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

    /**
     * Receives a message with a UserViewModel in the attachment field.
     * <p>
     * This function queries the database for the username field contained inside the UserViewModel which contains a User,
     * if the username exists it will compare the hashed passwords.
     * <p>
     * Returns the user data in case of a sucessfull login (name,password,administrator,password etc).
     * In case of a failed login due to password mismatch returns null along with an Error_Messages.INVALID_PASSWORD
     * If it doesn't find the username in the Database it will return null along with an Error_Messages.USERNAME_DOESNT_EXIST
     */
    public Message login(Message received_login) throws SQLException {
        User login_user = (User) received_login.attachment;

        String query = "select * from utilizador where username=?";

        String update_query = "update utilizador set autenticado=? where username=?";

        //create a prepared statement and add the received username to it
        PreparedStatement statement = database_connection.prepareStatement(query);
        statement.setString(1,login_user.username);

        PreparedStatement update_statement = database_connection.prepareStatement(update_query);

        //execute the query and save the results in a ResultSet
        ResultSet query_result = statement.executeQuery();


        //if there were results check if the passwords match and if they do fill a User object with the data
        //stored in the database
        if(query_result.isBeforeFirst()){
            query_result.next();
            if(query_result.getString("password").equals(login_user.password)){
                User user_data = new User();
                user_data.id = query_result.getInt("id");
                user_data.username = query_result.getString("username");
                user_data.name = query_result.getString("nome");
                user_data.password = query_result.getString("password");
                user_data.administrator = query_result.getBoolean("administrador");


                update_statement.setBoolean(1,true);
                update_statement.setString(2,user_data.username);
                update_statement.executeUpdate();



                user_data.authenticated = true;

                return new Message(Message.TYPE_OF_MESSAGE.LOGIN,new UserViewModel(user_data,Error_Messages.SUCESS));
            }

            return new Message(Message.TYPE_OF_MESSAGE.LOGIN, new UserViewModel(null,Error_Messages.INVALID_PASSWORD));


        }

        return new Message(Message.TYPE_OF_MESSAGE.LOGIN, new UserViewModel(null,Error_Messages.USERNAME_DOESNT_EXIST));



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
