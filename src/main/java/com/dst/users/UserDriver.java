package com.dst.users;

import com.dst.msg.WarehouseMessage;

public class UserDriver extends User {
    private DriverStatus status;
    private WarehouseMessage.NewTask.Weight weightClass;

    public UserDriver(String userName, String password) {
        super(userName, password);
        weightClass = WarehouseMessage.NewTask.Weight.KG_1500;
    }

    public UserDriver(String userName, String password, WarehouseMessage.NewTask.Weight weight) {
        super(userName, password);
        weightClass = weight;
    }

    @Override
    public WarehouseMessage.LogInResponse.Role getRole() {
        return WarehouseMessage.LogInResponse.Role.DRIVER;
    }

    public DriverStatus getStatus() {
        return status;
    }

    public void setStatus(DriverStatus status) {
        this.status = status;
    }

    @Override
    public String getUserInfo() {
        return weightClass.name();
    }
}
