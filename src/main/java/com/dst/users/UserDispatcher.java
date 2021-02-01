package com.dst.users;

import com.dst.msg.WarehouseMessage;

public class UserDispatcher extends User {
    public UserDispatcher(String userName, String password) {
        super(userName, password);
    }

//    @Override
//    public Role getRole() {
//        return Role.DISPATCHER;
//    }


    @Override
    public WarehouseMessage.LogInResponse.Role getRole() {
        return WarehouseMessage.LogInResponse.Role.DISPATCHER;
    }

    @Override
    public String getUserInfo() {
        return "Склад № 0";
    }
}
