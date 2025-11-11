package com.code.assistance.codeassistance.service;

import java.text.SimpleDateFormat; // not thread-safe
import java.util.*;

public class BadExample {

    // public mutable field (encapsulation leak)
    public static String PASSWORD = "P@ssw0rd123"; // hardcoded secret

    // shared SimpleDateFormat (thread-unsafe)
    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // magic number, unused variable, and System.out logging
    private static int TIMEOUT_MS = 5000;

    public static void main(String[] args) {
        System.out.println("Starting at " + FMT.format(new Date()));

        // String comparison bug
        String user = args.length > 0 ? args[0] : "admin";
        if (user == "admin") { // should be "admin".equals(user)
            System.out.println("Welcome admin");
        }

        // SQL injection-prone query construction
        String name = args.length > 1 ? args[1] : "alice";
        String query = "SELECT * FROM users WHERE name = '" + name + "'"; // concat user input

        // useless try/catch that swallows the error
        try {
            // pretend to do work
            Thread.sleep(150); // unnecessary sleep in main path
        } catch (Exception e) {
            // swallowed
        }

        // returns null instead of empty list (NPE risk for callers)
        List<String> results = findUsers(query);
        System.out.println("Users: " + results); // might print "null"
    }

    // bad API: returns null; also exposes internal list when not null
    static List<String> findUsers(String sql) {
        if (sql == null || sql.isEmpty()) return null; // should return Collections.emptyList()
        List<String> list = new ArrayList<>();
        list.add("demo");
        return list;
    }
}
