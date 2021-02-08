package com.dst.users;

import com.dst.msg.WarehouseMessage;

public class UserDispatcher extends User {

    private String dispatcherInfo = "no information";
    public UserDispatcher(String userName, String password) {
        super(userName, password);
    }

    public UserDispatcher(String userName, String password, String dispatcherInfo) {
        super(userName, password);
        this.dispatcherInfo = dispatcherInfo;
    }

    @Override
    public WarehouseMessage.LogInResponse.Role getRole() {
        return WarehouseMessage.LogInResponse.Role.DISPATCHER;
    }

    @Override
    public String getUserInfo() {
        return dispatcherInfo;
    }
}
