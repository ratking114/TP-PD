package shared;

public enum Error_Messages {


    INVALID_EMAIL,
    USERNAME_ALREADY_EXISTS("The username is already in use"),
    INVALID_USERNAME("The username must have between 2 and 128"),
    USERNAME_DOESNT_EXIST("Username doesn't exist"),

    SHOW_ALREADY_EXISTS("The show already exists"),

    INVALID_PASSWORD("Invalid Password"),
    INVALID_CONFIRMATION_PASSWORD("The password introduced does not match"),

    INVALID_NAME("Name must have between 2 and 128 characters"),
    SQL_ERROR("An error ocurred with the database"),

    SHOW_HAS_PAID_RESERVATIONS("The show can't be removed due to paid reservations"),

    SUCESS("Registered successfuly"),

    USER_NOT_LOGGED_IN("You must log in in order to use the system"),

    NOT_ADMIN("Only the administrator can perform that command"),

    ALREADY_VISIBLE("The show with that id is already visible"),

    EVENT_DOESNT_EXIT("The event with that id doesn't exist"),

    ERROR_IN_FILE("File has a formatting error, please check and try again"),
    SEAT_BOOKED_SUCCESSFULY("THe seat was booked successfully"),
    SEAT_BOOKING_INFO_INVALID("The seat doesn't exist"),
    SEAT_ALEADY_BOOKED("The chosen seat is already booked");


    private String _return_Message;
    Error_Messages(String return_Message){
        this._return_Message=return_Message;
    }
    Error_Messages(){}

    @Override
    public String toString() {
        return _return_Message==""?super.toString():_return_Message;
    }
}


