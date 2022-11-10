package shared;

public class User {
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
