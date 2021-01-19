package com.dst.users;

public class UserDriver extends User {
    private DriverStatus status;

    public UserDriver(String userName, String password) {
        super(userName, password);
    }

    @Override
    public Role getRole() {
        return Role.DRIVER;
    }

    public DriverStatus getStatus() {
        return status;
    }

    public void setStatus(DriverStatus status) {
        this.status = status;
    }
}
