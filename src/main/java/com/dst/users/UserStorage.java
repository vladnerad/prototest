package com.dst.users;

import com.dst.msg.WarehouseMessage;

import java.util.ArrayList;

public class UserStorage {

    private static final ArrayList<User> users = new ArrayList<>();

    public static ArrayList<User> getUsers() {
        return users;
    }

    static {
        users.add(new UserDispatcher("SVC-1", "123", "СВЦ-1"));
        users.add(new UserDispatcher("CMO-1", "123", "ЦМО-1"));
        users.add(new UserDispatcher("disp3", "123"));
        users.add(new UserDispatcher("disp4", "123"));
        users.add(new UserDispatcher("disp5", "123"));
        users.add(new UserDriver("dr1-1500", "123", WarehouseMessage.NewTask.Weight.KG_1500));
        users.add(new UserDriver("dr2-1750", "123", WarehouseMessage.NewTask.Weight.KG_1750));
        users.add(new UserDriver("dr3-1750", "123", WarehouseMessage.NewTask.Weight.KG_1750));
        users.add(new UserDriver("dr4-2000", "123", WarehouseMessage.NewTask.Weight.KG_2000));
        users.add(new UserDriver("dr5-3000", "123", WarehouseMessage.NewTask.Weight.KG_3000));
        users.add(new UserDriver("dr6-5000", "123", WarehouseMessage.NewTask.Weight.KG_5000));
    }
}
