package com.dst.users;

public class UserDispatcher extends User {
    public UserDispatcher(String userName, String password) {
        super(userName, password);
    }

    @Override
    public Role getRole() {
        return Role.DISPATCHER;
    }
}
