package user;

public class Customer extends User{
    private double balance;

    public Customer(int id, String email, String password, String name, String type, double balance) {
        super(id, email, password, name, type);
        this.balance = balance;
    }

    public double getBalance() {
        return balance;
    }
    
}
