package com.dst.users;

import java.util.ArrayList;

public class UserStorage {

    private static ArrayList<User> users = new ArrayList<>();

    public static ArrayList<User> getUsers() {
        return users;
    }

    static {
        users.add(new User("disp1", "1356", Role.DISPATCHER));
        users.add(new User("driver1", "111665", Role.DRIVER));
    }
}
