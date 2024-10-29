package com.capstone.tansiri.map.entity;

import java.util.List;

public class Response {

    private String type;
    private List<Feature> features;

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Feature> getFeatures() {
        return features;
    }

    public void setFeatures(List<Feature> features) {
        this.features = features;
    }

    // Inner classes for Feature, Geometry, and Properties

    public static class Feature {
        private String type;
        private Geometry geometry;
        private Properties properties;

        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Geometry getGeometry() {
            return geometry;
        }

        public void setGeometry(Geometry geometry) {
            this.geometry = geometry;
        }

        public Properties getProperties() {
            return properties;
        }

        public void setProperties(Properties properties) {
            this.properties = properties;
        }
    }

    public static class Geometry {
        private String type;
        private List<List<Double>> coordinates;

        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<List<Double>> getCoordinates() {
            return coordinates;
        }

        public void setCoordinates(List<List<Double>> coordinates) {
            this.coordinates = coordinates;
        }
    }

    public static class Properties {
        private int index;
        private int lineIndex;
        private String name;
        private String description;
        private int distance;
        private int time;
        private int roadType;
        private int categoryRoadType;
        private String facilityType;
        private String facilityName;

        // Additional properties can be added as needed
        // Getters and Setters

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public int getLineIndex() {
            return lineIndex;
        }

        public void setLineIndex(int lineIndex) {
            this.lineIndex = lineIndex;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public int getDistance() {
            return distance;
        }

        public void setDistance(int distance) {
            this.distance = distance;
        }

        public int getTime() {
            return time;
        }

        public void setTime(int time) {
            this.time = time;
        }

        public int getRoadType() {
            return roadType;
        }

        public void setRoadType(int roadType) {
            this.roadType = roadType;
        }

        public int getCategoryRoadType() {
            return categoryRoadType;
        }

        public void setCategoryRoadType(int categoryRoadType) {
            this.categoryRoadType = categoryRoadType;
        }

        public String getFacilityType() {
            return facilityType;
        }

        public void setFacilityType(String facilityType) {
            this.facilityType = facilityType;
        }

        public String getFacilityName() {
            return facilityName;
        }

        public void setFacilityName(String facilityName) {
            this.facilityName = facilityName;
        }
    }
}
