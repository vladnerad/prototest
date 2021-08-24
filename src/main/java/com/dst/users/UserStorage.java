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
        users.add(new UserDispatcher("sklad-2", "123", "Склад №2"));
        users.add(new UserDispatcher("kargapolov", "123", "Каргаполов"));
        users.add(new UserDispatcher("bakin", "123", "Бакин"));
        users.add(new UserDispatcher("gerasimov", "123", "Герасимов"));
        users.add(new UserDispatcher("grischenko", "123", "Грищенко"));
        users.add(new UserDispatcher("kozlov", "123", "Козлов"));
        users.add(new UserDispatcher("shariy", "123", "Шарый"));
        users.add(new UserDispatcher("kostylev", "123", "Костылев"));
        users.add(new UserDispatcher("lykov", "123", "Лыков"));
        users.add(new UserDispatcher("ivanov", "123", "Иванов"));
        users.add(new UserDispatcher("gagarin", "123", "Гагарин"));
        users.add(new UserDriver("dr1-1500", "123", WarehouseMessage.NewTask.Weight.KG_5000));
        users.add(new UserDriver("dr2-1750", "123", WarehouseMessage.NewTask.Weight.KG_3000));
        users.add(new UserDriver("dr3-1750", "123", WarehouseMessage.NewTask.Weight.KG_1750));
        users.add(new UserDriver("dr4-2000", "123", WarehouseMessage.NewTask.Weight.KG_1500));
        users.add(new UserDriver("dr5-3000", "123", WarehouseMessage.NewTask.Weight.KG_2000));
        users.add(new UserDriver("dr6-5000", "123", WarehouseMessage.NewTask.Weight.KG_5000));
    }
}
