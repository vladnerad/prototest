package com.dst.users;

import java.util.ArrayList;

public class UserStorage {

    private static final ArrayList<User> users = new ArrayList<>();

    public static ArrayList<User> getUsers() {
        return users;
    }

    static {
        users.add(new UserDispatcher("disp1", "123"));
        users.add(new UserDispatcher("disp2", "123"));
        users.add(new UserDispatcher("disp3", "123"));
        users.add(new UserDispatcher("disp4", "123"));
        users.add(new UserDispatcher("disp5", "123"));
        users.add(new UserDriver("dr1", "123"));
        users.add(new UserDriver("dr2", "123"));
        users.add(new UserDriver("dr3", "123"));
        users.add(new UserDriver("dr4", "123"));
        users.add(new UserDriver("dr5", "123"));
    }
}
