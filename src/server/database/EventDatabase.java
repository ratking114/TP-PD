package server.database;

import server.ServerClasses.Prepare;
import server.ServerClasses.ServerModel;
import shared.*;

import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


/**
 * EventDatabase is the server.database that stores the events and all information
 * associated with them.
 */
public class EventDatabase {
    /**
     * The connection to the server.database.
     */
    private  Connection database_connection;

    private final ServerModel server_model;

    private Path database_file_name;

    public EventDatabase(Path database_file_name, ServerModel server_model) throws SQLException{
        //save the database file name
        this.database_file_name = database_file_name;

        //set the database connection to null
        this.database_connection = null;

        //save the server model
        this.server_model = server_model;
    }

    /**
     * This function receives a ResultSet from a database query
     * and uses it to build and return a Reservation object
     */
    private Reservation buildReservationFromTable(ResultSet table) throws SQLException{
        return new Reservation(
                table.getInt("id"),
                table.getString("data_hora"),
                table.getBoolean("pago"),
                table.getInt("id_utilizador"),
                table.getInt("id_espetaculo")
        );
    }

    /**
     * This function receives a ResultSet from a database query
     * and uses it to build and return a EventInfo object
     */
    private EventInfo buildEventInfoFromTable(ResultSet table) throws SQLException{
        return new EventInfo(
                table.getInt("id"),
                table.getString("descricao"),
                table.getString("tipo"),
                table.getString("data_hora"),
                table.getInt("duracao"),
                table.getString("local"),
                table.getString("localidade"),
                table.getString("pais"),
                table.getString("classificacao_etaria"),
                buildSeatPriceListFromEventTable(table)
        );
    }

    /**
     * This function receives a ResultSet from a database query
     * and uses it to build and return a List of seat prices object
     * WARNING: USE ONLY IF THE ResultSet IS AN EVENT
     */
    private List<SeatPrice> buildSeatPriceListFromEventTable(ResultSet table) throws SQLException{
        String query = "select * from lugar where espetaculo_id=?";

        PreparedStatement statement = database_connection.prepareStatement(query);

        statement.setInt(1,table.getInt("id"));

        ResultSet seat_list = statement.executeQuery();

        if(seat_list.isBeforeFirst()){
            ArrayList<SeatPrice> seat_prices = new ArrayList<>();
            while (seat_list.next()){
                SeatPrice seat_price = new SeatPrice(
                        seat_list.getInt("id"),
                        seat_list.getString("fila").charAt(0),
                        seat_list.getInt("assento"),
                        seat_list.getDouble("preco"),
                        seat_list.getInt("espetaculo_id"));

                seat_prices.add(seat_price);

            }

            return seat_prices;
        }
        return new ArrayList<>();
    }


    public Message getShowsFromFilter(Message received_event) throws SQLException {
        String query = "select * from espetaculo";

//and tipo=? and data_hora=? and duracao=? and local=? and" +
//                " localidade=? and pais=? and classificacao_etaria=?";
        PreparedStatement statement = database_connection.prepareStatement(query);

        EventFilter filters = (EventFilter) received_event.attachment;

        //statement.setString(1,filters.designation);

        int numOfvalues = 0;
        boolean firstParameter = true;
        if(filters.designation != null){
            if(firstParameter){
                query += " where descricao LIKE ?";
            } else {
                firstParameter = false;
                query += " and descricao LIKE ?";
            }
            statement = database_connection.prepareStatement(query);
            //statement.setString(2,filters.type);
        }

        if(filters.type != null){
            if(firstParameter){
                query += " where tipo=?";
            } else {
                firstParameter = false;
                query += " and tipo=?";
            }
            statement = database_connection.prepareStatement(query);
            //statement.setString(2,filters.type);
        }

        if(filters.date != null) {
            if(firstParameter){
                query += " where data_hora=?";
            } else {
                firstParameter = false;
                query += " and data_hora=?";
            }
            statement = database_connection.prepareStatement(query);
            //statement.setString(3, filters.date);
        }

        if(filters.duration != null) {
            if(firstParameter){
                query += " where duracao=?";
            } else {
                firstParameter = false;
                query += " and duracao=?";
            }
            statement = database_connection.prepareStatement(query);
            //statement.setInt(4, filters.duration);
        }

        if(filters.location != null) {
            if(firstParameter){
                query += " where local=?";
            } else {
                firstParameter = false;
                query += " and local=?";
            }
            statement = database_connection.prepareStatement(query);
            // statement.setString(5, filters.location);
        }

        if(filters.county != null) {
            if(firstParameter){
                query += " where localidade=?";
            } else {
                firstParameter = false;
                query += " and localidade=?";
            }
            statement = database_connection.prepareStatement(query);
            //statement.setString(6, filters.county);
        }

        if(filters.country != null) {
            if(firstParameter){
                query += " where pais=?";
            } else {
                firstParameter = false;
                query += " and pais=?";
            }
            statement = database_connection.prepareStatement(query);
            //statement.setString(7, filters.country);
        }

        if(filters.age_Restriction != null) {
            if(firstParameter){
                query += " where classificacao_etaria=?";
            } else {
                firstParameter = false;
                query += " and classificacao_etaria=?";
            }
            statement = database_connection.prepareStatement(query);
            //statement.setString(8, filters.age_Restriction);
        }


        if(filters.designation != null){
            //query += " and tipo=?";
            //statement = database_connection.prepareStatement(query);
            numOfvalues++;
            statement.setString(numOfvalues,"%"+filters.designation+"%");
        }

        if(filters.type != null){
            //query += " and tipo=?";
            //statement = database_connection.prepareStatement(query);
            numOfvalues++;
            statement.setString(numOfvalues,filters.type);
        }

        if(filters.date != null) {
            //query += " and data_hora=?";
            //statement = database_connection.prepareStatement(query);
            numOfvalues++;
            statement.setString(numOfvalues, filters.date);
        }

        if(filters.duration != null) {
            //query += " and duracao=?";
            //statement = database_connection.prepareStatement(query);
            numOfvalues++;
            statement.setInt(numOfvalues, filters.duration);
        }

        if(filters.location != null) {
            //query += " and local=?";
            //statement = database_connection.prepareStatement(query);
            numOfvalues++;
            statement.setString(numOfvalues, filters.location);
        }

        if(filters.county != null) {
            //query += " and localidade=?";
            //statement = database_connection.prepareStatement(query);
            numOfvalues++;
            statement.setString(numOfvalues, filters.county);
        }

        if(filters.country != null) {
            //query += " and pais=?";
            //statement = database_connection.prepareStatement(query);
            numOfvalues++;
            statement.setString(numOfvalues, filters.country);
        }

        if(filters.age_Restriction != null) {
            //statement = database_connection.prepareStatement(query);
            numOfvalues++;
            statement.setString(numOfvalues, filters.age_Restriction);
        }


        ResultSet filtered_events = statement.executeQuery();

        if(filtered_events.isBeforeFirst()){
            ArrayList<EventInfo> event_list = new ArrayList<EventInfo>();
            while(filtered_events.next()){

                EventInfo event_info = buildEventInfoFromTable(filtered_events);

                event_list.add(event_info);

            }
            return new Message(Message.TYPE_OF_MESSAGE.GET_SHOWS_FILTERED,event_list);
        }
        return new Message(Message.TYPE_OF_MESSAGE.GET_SHOWS_FILTERED,new ArrayList<EventInfo>());
    }

    /**
     * This will delete a show and all its dependencies from the database. Fails if the show has paid reservations
     * @param showToCancel contains the id of the show to be deleted
     * @return Message with the result of the query
     */
    public synchronized Error_Messages deleteShow(Message showToCancel)
    {
        Integer show_ID=(Integer) showToCancel.attachment;
        try {
            String query="select pago from reserva inner join espetaculo on(espetaculo.id=reserva.id_espetaculo) where pago=1 and espetaculo.id=?";
            PreparedStatement statement = database_connection.prepareStatement(query);
            statement.setString(1,show_ID.toString());
            System.out.println(statement);
            ResultSet query_result = statement.executeQuery();
            if(query_result.isBeforeFirst())
                return Error_Messages.SHOW_HAS_PAID_RESERVATIONS;
            query="delete from reserva where id_espetaculo=?; "
                    +
                    "delete from lugar where espetaculo_id=?;"
                    +
                    "delete from espetaculo where id=?";
            statement=database_connection.prepareStatement(query);
            statement.setString(1,show_ID.toString());
            statement.setString(2,show_ID.toString());
            statement.setString(3,show_ID.toString());
            query=String.format("delete from reserva where id_espetaculo=?;" +
                            "delete from lugar where espetaculo_id=?;" +
                            "delete from espetaculo where id=?",
                    show_ID.toString(),show_ID.toString(),show_ID.toString());
            Prepare delete_Show_Prepare= new Prepare(query,server_model.generateNumberFromIPAndPort());
            Message.TYPE_OF_MESSAGE response=server_model.getProducerConsumerBuffer().placeRequestAndWaitForAnswer(delete_Show_Prepare);
            if(response== Message.TYPE_OF_MESSAGE.COMMIT) {
                statement.execute();
                return Error_Messages.SUCESS;
            }
            else {
                return Error_Messages.SQL_ERROR;
            }

        }
        catch (SQLException sqlException)
        {
            sqlException.printStackTrace();
            return Error_Messages.INVALID_EMAIL;
        }


    }

    /**
     * This will return a list of all the shows in the database
     * @return
     * @throws SQLException
     */
    public synchronized List<EventInfo> getListOfAllShows() throws SQLException {
        List<EventInfo> list_Of_Events= new ArrayList<>();
        String query= "select * from espetaculo";
        PreparedStatement statement=database_connection.prepareStatement(query);
        ResultSet list_Of_Events_Set=statement.executeQuery();
        if(!list_Of_Events_Set.isBeforeFirst()) {
            System.out.println("No Shows Found");
            return null;
        }
        System.out.println("Theres some shows available");
        while (!list_Of_Events_Set.isAfterLast())
        {
            list_Of_Events_Set.next();
            EventInfo show= new EventInfo();
            show.id=list_Of_Events_Set.getInt(1);
            show.designation=list_Of_Events_Set.getString(2);
            show.type=list_Of_Events_Set.getString(3);
            show.date=list_Of_Events_Set.getString(4);
            show.duration=list_Of_Events_Set.getInt(5);
            show.location=list_Of_Events_Set.getString(6);
            show.county=list_Of_Events_Set.getString(7);
            show.country=list_Of_Events_Set.getString(8);
            show.age_Restriction=list_Of_Events_Set.getString(9);
            list_Of_Events.add(show);
        }
        return list_Of_Events;
    }



    /**
     * This function uses the id present in the User object to query the database
     * and determine which reservations they haven't paid for.
     * It compiles the results into a List of ReservationViewmodel that it then returns.
     * This viewmodel will contain information about the reservation and the event it relates to
     */
    public Message getUnpaidReservations(Message received_user) throws SQLException{
        User user = (User) received_user.attachment;

        String query = "select * from reserva where id_utilizador=? and pago=?";

        PreparedStatement statement = database_connection.prepareStatement(query);

        statement.setInt(1,user.id);
        statement.setBoolean(2,false);

        ResultSet unpaid_reservation_results = statement.executeQuery();

        if(unpaid_reservation_results.isBeforeFirst()){
            ArrayList<ReservationViewmodel> paid_reservation_list = new ArrayList<>();
            while(unpaid_reservation_results.next()){

                Reservation reservation = buildReservationFromTable(unpaid_reservation_results);


                String event_query = "select * from espetaculo where id=?";

                PreparedStatement event_statement = database_connection.prepareStatement(event_query);

                event_statement.setInt(1,reservation.event_id);

                ResultSet event_result = event_statement.executeQuery();
                event_result.next();

                EventInfo event_info = buildEventInfoFromTable(event_result);

                paid_reservation_list.add(new ReservationViewmodel(event_info,reservation,Error_Messages.SUCESS));

            }

            return new Message(Message.TYPE_OF_MESSAGE.GET_UNPAID_RESERVATION,paid_reservation_list);

        }

        return new Message(Message.TYPE_OF_MESSAGE.GET_UNPAID_RESERVATION,new ArrayList<ReservationViewmodel>());
    }


    /**
     * This function uses the id present in the User object to query the database
     * and determine which reservations they have paid for.
     * It compiles the results into a List of ReservationViewmodel that it then returns.
     * This viewmodel will contain information about the reservation and the event it relates to
     */
    public Message getPaidReservations(Message received_user) throws SQLException{
        User user = (User) received_user.attachment;

        String query = "select * from reserva where id_utilizador=? and pago=?";

        PreparedStatement statement = database_connection.prepareStatement(query);

        statement.setInt(1, user.id);
        statement.setBoolean(2,true);

        ResultSet paid_for_reservation_results = statement.executeQuery();

        if(paid_for_reservation_results.isBeforeFirst()){
            ArrayList<ReservationViewmodel> paid_reservation_list = new ArrayList<>();
            while(paid_for_reservation_results.next()){

                Reservation reservation = buildReservationFromTable(paid_for_reservation_results);


                String event_query = "select * from espetaculo where id=?";

                PreparedStatement event_statement = database_connection.prepareStatement(event_query);

                event_statement.setInt(1,reservation.event_id);

                ResultSet event_result = event_statement.executeQuery();
                event_result.next();

                EventInfo event_info = buildEventInfoFromTable(event_result);

                paid_reservation_list.add(new ReservationViewmodel(event_info,reservation,Error_Messages.SUCESS));

            }

            return new Message(Message.TYPE_OF_MESSAGE.GET_PAID_RESERVATION,paid_reservation_list);

        }

        return new Message(Message.TYPE_OF_MESSAGE.GET_PAID_RESERVATION,new ArrayList<ReservationViewmodel>());
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
    public synchronized Message login(Message received_login) throws SQLException, InterruptedException {
        try {
            this.server_model.lockDatabase();

            User login_user = (User) received_login.attachment;

            String query = "select * from utilizador where username=?";

            String update_query = "update utilizador set autenticado=? where username=?";

            //create a prepared statement and add the received username to it
            PreparedStatement statement = database_connection.prepareStatement(query);
            statement.setString(1, login_user.username);

            PreparedStatement update_statement = database_connection.prepareStatement(update_query);

            //execute the query and save the results in a ResultSet
            ResultSet query_result = statement.executeQuery();


            //if there were results check if the passwords match and if they do fill a User object with the data
            //stored in the database
            if (query_result.isBeforeFirst()) {
                query_result.next();
                if (query_result.getString("password").equals(login_user.password)) {
                    //construct and fill a User object to send to the client
                    User user_data = new User();
                    user_data.id = query_result.getInt("id");
                    user_data.username = query_result.getString("username");
                    user_data.name = query_result.getString("nome");
                    user_data.password = query_result.getString("password");
                    user_data.administrator = query_result.getBoolean("administrador");
                    user_data.authenticated = true;

                    //substitute in the query the missing fields and construct a valid query
                    update_statement.setBoolean(1, true);
                    update_statement.setString(2, user_data.username);
                    String query_to_send = String.format(
                            "update utilizador set autenticado=%s where username=\"%s\"", true, user_data.username
                    );

                    //now see if we can execute the update
                    Message.TYPE_OF_MESSAGE answer = this.server_model.getProducerConsumerBuffer().placeRequestAndWaitForAnswer(
                            new Prepare(query_to_send, this.server_model.generateNumberFromIPAndPort())
                    );
                    if (answer == Message.TYPE_OF_MESSAGE.COMMIT) {
                        update_statement.executeUpdate();
                        return new Message(Message.TYPE_OF_MESSAGE.LOGIN, new UserViewModel(user_data, Error_Messages.SUCESS));
                    } else {
                        return new Message(Message.TYPE_OF_MESSAGE.LOGIN, new UserViewModel(null, Error_Messages.SQL_ERROR));
                    }
                }

                return new Message(Message.TYPE_OF_MESSAGE.LOGIN, new UserViewModel(null, Error_Messages.INVALID_PASSWORD));
            }

            return new Message(Message.TYPE_OF_MESSAGE.LOGIN, new UserViewModel(null, Error_Messages.USERNAME_DOESNT_EXIST));

        }finally {
            this.server_model.unlockDatabase();
        }
    }


    /**
     *This function will try to register a new user with the info inputed on the client
     * @param received_register protocol message sent by the client containing the type of request and corresponding attachment, a user in this instance
     * @return Error_Messages which contains the result of the command
     */
    public synchronized Error_Messages register(Message received_register) throws SQLException
    {
        User user_To_Register=(User) received_register.attachment;
        String query= "select username from utilizador where upper(username)=upper(?)";
        PreparedStatement statement = database_connection.prepareStatement(query);
        statement.setString(1,user_To_Register.username);
        ResultSet query_result = statement.executeQuery();
        if(query_result.isBeforeFirst())
            return Error_Messages.USERNAME_ALREADY_EXISTS;
        System.out.println("Utilizador já existe?"+ (query_result.isBeforeFirst()?"sim":"não"));
        //We will only allway names between 2 and 128 characters
        if(user_To_Register.name.length()<2||user_To_Register.name.length()>16)
            return Error_Messages.INVALID_USERNAME;

        //The password must contain more than 6 characters
        if(user_To_Register.password.length()<4||user_To_Register.password.length()>12)
            return Error_Messages.INVALID_PASSWORD;
        if(user_To_Register.name.length()<2||user_To_Register.name.length()>128)
            return Error_Messages.INVALID_USERNAME;
        try {
            query = String.format("insert into utilizador (username,nome,password,administrador,autenticado) values(upper(%s),%s,%s,false,true) ", user_To_Register.username,
                    user_To_Register.name, Utils.hashString(user_To_Register.password));
            query="insert into utilizador (username,nome,password,administrador,autenticado) values(upper(?),?,?,false,true)" ;

            statement = database_connection.prepareStatement(query);
            statement.setString(1,user_To_Register.username);
            statement.setString(2,user_To_Register.name);
            statement.setString(3, Utils.hashString(user_To_Register.password));
            System.out.println(statement);
            Prepare registration_Prepare= new Prepare(String.format("insert into utilizador (username,nome,password,administrador,autenticado) values(upper(%s),%s,%s,false,true) ", user_To_Register.username,
                    user_To_Register.name, Utils.hashString(user_To_Register.password)),server_model.generateNumberFromIPAndPort());
            Message.TYPE_OF_MESSAGE response=server_model.getProducerConsumerBuffer().placeRequestAndWaitForAnswer(registration_Prepare);
            if(response==Message.TYPE_OF_MESSAGE.COMMIT)
                statement.execute();
            else
                return Error_Messages.SQL_ERROR;
        }
        catch (SQLException sqlException)
        {
            sqlException.printStackTrace();
            return Error_Messages.SQL_ERROR;
        }


        return Error_Messages.SUCESS;
    }



    public synchronized Error_Messages changeUserData(Message change_data)
    {
        User user_To_Register=(User) change_data.attachment;
        String query= "select id,username from utilizador where upper(username)=upper(?)";
        try {
            PreparedStatement statement = database_connection.prepareStatement(query);
            statement.setString(1,user_To_Register.old_Username);
            ResultSet query_result = statement.executeQuery();
            if(!query_result.isBeforeFirst())
                return Error_Messages.SQL_ERROR;
            query_result.next();
            int id= query_result.getInt("id");
            //We will only allway names between 2 and 128 characters
            if(user_To_Register.username.length()<2||user_To_Register.username.length()>16)
                return Error_Messages.INVALID_USERNAME;

            //The password must contain more than 6 characters
            if(user_To_Register.password.length()<4||user_To_Register.password.length()>12)
                return Error_Messages.INVALID_PASSWORD;
            if(user_To_Register.name.length()<2||user_To_Register.name.length()>128)
                return Error_Messages.INVALID_USERNAME;


            query="update utilizador set username=upper(?),nome=?,password=? where id = ? " ;

            statement = database_connection.prepareStatement(query);
            statement.setString(1,user_To_Register.username);
            statement.setString(2,user_To_Register.name);
            statement.setString(3, Utils.hashString(user_To_Register.password));
            statement.setString(4,Integer.toString(id));
            System.out.println(statement);
            statement = database_connection.prepareStatement(query);
            Prepare registration_Prepare= new Prepare(String.format("update utilizador set username=upper(%s),nome=%s,password=%s where id = %d"
                    ,user_To_Register.username,user_To_Register.name,user_To_Register.password,id),server_model.generateNumberFromIPAndPort());
            Message.TYPE_OF_MESSAGE response=server_model.getProducerConsumerBuffer().placeRequestAndWaitForAnswer(registration_Prepare);
            if(response==Message.TYPE_OF_MESSAGE.COMMIT)
                statement.execute();
            else
                return Error_Messages.SQL_ERROR;
        }
        catch (SQLException sqlException)
        {
            sqlException.printStackTrace();
            return Error_Messages.INVALID_EMAIL;
        }

        return Error_Messages.SUCESS;
    }

    /**
     * Executes the supplied query.
     */
    public void executeQuery(String query){
        try {
            System.out.println("About to execute: " + query);
            this.database_connection.createStatement().executeUpdate(query);
        } catch (SQLException e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    public void closeDatabaseConnection(){
        //the connection is already closed
        if(database_connection == null){
            return;
        }
        try {
            database_connection.close();
            database_connection = null;
        } catch (SQLException e) {
            System.out.println(e);
        }
    }


    public void openDatabaseConnection(){
        //see if the connection is already open
        if(this.database_connection != null){
            return;
        }
        try {
            this.database_connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + database_file_name
            );
        } catch (SQLException e) {
            System.out.println(e);
        }
    }

}