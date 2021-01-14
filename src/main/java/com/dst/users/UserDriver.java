package com.dst.users;

public class UserDriver extends User {
    public UserDriver(String userName, String password) {
        super(userName, password);
    }

    @Override
    public Role getRole() {
        return Role.DRIVER;
    }
}
