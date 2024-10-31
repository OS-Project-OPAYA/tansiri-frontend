package com.capstone.tansiri.map.entity;

public class WalkRoute {
    private Long id; // 기본 키
    private String startX;
    private String startY;
    private String endX;
    private String endY;
    private String startName;
    private String endName;
    private String response;
    private String userID;

    // 모든 필드를 포함하는 생성자
    public WalkRoute(String startX, String startY, String endX, String endY, String startName, String endName, String userID) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.startName = startName;
        this.endName = endName;
        this.userID = userID;
    }


    public String getStartX() {
        return startX;
    }

    public String getStartY() {
        return startY;
    }

    public String getEndX() {
        return endX;
    }

    public String getEndY() {
        return endY;
    }

    public String getResponse() {
        return response;
    }

    public String getStartName() {
        return startName;
    }

    public String getEndName() {
        return endName;
    }

    public String getUserID() {
        return userID;
    }
}
