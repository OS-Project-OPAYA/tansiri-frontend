package com.capstone.tansiri.map.entity;

public class UserState {
    private double userLat;
    private double userLon;
    private double userDir;
    private String userID;

    public UserState(double userLat, double userLon, double userDir, String userID) {
        this.userLat = userLat;
        this.userLon = userLon;
        this.userDir = userDir;
        this.userID = userID;
    }


    public double getUserLat() {
        return userLat;
    }

    public double getUserLon() {
        return userLon;
    }

    public double getUserDir() {
        return userDir;
    }

    public String getUserID() {
        return userID;
    }
}