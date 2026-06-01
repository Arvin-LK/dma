package com.example;

import java.sql.Connection;
import java.sql.Statement;

public class UserService {
    public void findUsers() {
        String sql = "SELECT IFNULL(name, 'N/A') FROM users LIMIT 10, 20";
        System.out.println(sql);
    }

    public void insertUser() {
        String sql = "INSERT INTO t (id) VALUES (UUID())";
        System.out.println(sql);
    }
}
