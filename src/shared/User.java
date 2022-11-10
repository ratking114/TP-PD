package shared;

import java.io.Serializable;

public class User implements Serializable {
    public int id;
    public String username;
    public String name;
    public String password;
    public boolean administrator;
    public boolean authenticated;
    public User(String username,String name,String password){
        this.password=password;
        this.name=name;
        this.username=username;
        this.administrator=false;
        this.authenticated=false;
    }
    public User(){}
}
