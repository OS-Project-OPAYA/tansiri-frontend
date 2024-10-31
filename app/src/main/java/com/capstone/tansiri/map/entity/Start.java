package com.capstone.tansiri.map.entity;

public class Start {
    private String startName;
    private String startLat;
    private String startLon;
    private String userID;

    public Start(String startName, String startLat, String startLon, String userID) {
        this.startName = startName;
        this.startLat = startLat;
        this.startLon = startLon;
        this.userID = userID;
    }

    public String getUserID() {
        return userID;
    }
}
