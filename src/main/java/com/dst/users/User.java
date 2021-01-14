package com.dst.users;

public class User implements Roleable {
    private final String userName;
    private final String password;
//    private Role role;

    protected User(String userName, String password/*, Role role*/) {
        this.userName = userName;
        this.password = password;
//        this.role = role;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return Role.UNKNOWN;
    }
}