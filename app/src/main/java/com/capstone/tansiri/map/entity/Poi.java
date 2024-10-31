package com.capstone.tansiri.map.entity;


public class Poi {
    private String id;
    private String name;
    private String frontLat;
    private String frontLon;
    public String userID;

    // 기본 생성자
    public Poi() {
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFrontLat() {
        return frontLat;
    }

    public void setFrontLat(String frontLat) {
        this.frontLat = frontLat;
    }

    public String getFrontLon() {
        return frontLon;
    }

    public void setFrontLon(String frontLon) {
        this.frontLon = frontLon;
    }

    public static class SearchRequest {
        private String keyword;
        private String userID; // 사용자 ID 필드 추가

        // Getter 및 Setter 메서드
        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public String getUserID() {
            return userID;
        }

        public void setUserID(String userID) {
            this.userID = userID;
        }
    }
}