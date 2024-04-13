package user;

public abstract class User {
    private int id;
    private String password;
    private String email;
    private String name;
    private String type;

    public User(int id, String email, String password, String name, String type) {
        this.id = id;
        this.password = password;
        this.email = email;
        this.name = name;
        this.type = type;
    }

    public int getID() {
        return this.id;
    }

    public String getEmail() {
        return this.email;
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

}
