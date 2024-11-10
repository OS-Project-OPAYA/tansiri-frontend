package com.capstone.tansiri.map.entity;


public class Poi {
    private String id;
    private String name;
    private String frontLat;
    private String frontLon;
    public String userID;


    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getFrontLat() {
        return frontLat;
    }

    public String getFrontLon() {
        return frontLon;
    }

    public static class SearchRequest {
        private String keyword;
        private String userID;

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public void setUserID(String userID) {
            this.userID = userID;
        }
    }
}