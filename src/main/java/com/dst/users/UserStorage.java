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

        users.add(new UserDispatcher("grischenko", "123", "Грищенко"));

        users.add(new UserDispatcher("mika", "123", "СВЦ-2 (Микаелян)"));
        users.add(new UserDispatcher("kargapolov", "123", "СВЦ-2,-3 (Каргаполов)"));

        users.add(new UserDispatcher("bakin", "123", "СВЦ-1,-4 (Бакин)"));

        users.add(new UserDispatcher("gerasimov", "123", "ЦМО-1 (Герасимов)"));

        users.add(new UserDispatcher("raskatov", "123", "Малярка, сборка (Раскатов)"));
        users.add(new UserDispatcher("sidorkin", "123", "Малярка, сборка (Сидоркин)"));
        users.add(new UserDispatcher("mamonov", "123", "Малярка, сборка (Мамонов)"));
        users.add(new UserDispatcher("kozlov", "123", "Малярка, сборка (Козлов)"));
        users.add(new UserDispatcher("shariy", "123", "Малярка, сборка (Шарый)"));
        users.add(new UserDispatcher("kostylev", "123", "Малярка, сборка (Костылев)"));
        users.add(new UserDispatcher("lykov", "123", "Малярка, сборка (Лыков)"));
        users.add(new UserDispatcher("ivanov", "123", "Малярка, сборка (Иванов)"));
        users.add(new UserDispatcher("gagarin", "123", "Малярка, сборка (Гагарин)"));

        users.add(new UserDispatcher("nezamud_n", "123", "Склад №1 (Незамутдинова Н.)"));
        users.add(new UserDispatcher("nezamud_t", "123", "Склад №2 (Незамутдинова Т.)"));
        users.add(new UserDispatcher("kudryavtseva", "123", "Склад №2 (Кудрявцева)"));

        users.add(new UserDispatcher("melnikov", "123", "СБЦ-1 (Мельников)"));
        users.add(new UserDispatcher("murygin", "123", "СБЦ-1 (Мурыгин)"));

        users.add(new UserDispatcher("nizamov", "123", "ЗЦ-1,-2 (Низамов)"));
        users.add(new UserDispatcher("nemich", "123", "ЗЦ-1,-2 (Немич)"));
        users.add(new UserDispatcher("savinyh", "123", "ЗЦ-1,-2 (Савиных)"));

        users.add(new UserDispatcher("nosenko", "123", "СДМ (Носенко)"));

        users.add(new UserDriver("dr1-1500", "123", WarehouseMessage.NewTask.Weight.KG_5000));
        users.add(new UserDriver("dr2-1750", "123", WarehouseMessage.NewTask.Weight.KG_3000));
        users.add(new UserDriver("dr3-1750", "123", WarehouseMessage.NewTask.Weight.KG_1750));
        users.add(new UserDriver("dr4-2000", "123", WarehouseMessage.NewTask.Weight.KG_1500));
        users.add(new UserDriver("dr5-3000", "123", WarehouseMessage.NewTask.Weight.KG_2000));
        users.add(new UserDriver("dr6-5000", "123", WarehouseMessage.NewTask.Weight.KG_5000));
    }
}
