package com.dst.users;

import com.dst.msg.WarehouseMessage;

public class User implements Roleable, UserInfo {
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

    @Override
    public WarehouseMessage.LogInResponse.Role getRole() {
        return null;
    }

    @Override
    public String getUserInfo() {
        return "null";
    }

//    public Role getRole() {
//        return Role.UNKNOWN;
//    }

}