package com.dst.users;

import java.util.ArrayList;

public class UserStorage {

    private static ArrayList<User> users = new ArrayList<>();

    public static ArrayList<User> getUsers() {
        return users;
    }

    static {
        users.add(new UserDispatcher("disp1", "1356"));
        users.add(new UserDriver("driver1", "111665"));
    }
}
