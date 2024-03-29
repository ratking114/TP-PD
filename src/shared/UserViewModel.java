package shared;

import java.io.Serializable;

public class UserViewModel implements Serializable {
    public UserViewModel(){}

    public UserViewModel(User user, Error_Messages error_message) {
        this.user = user;
        this.error_message = error_message;
    }

    public User user;
    public Error_Messages error_message;
}
