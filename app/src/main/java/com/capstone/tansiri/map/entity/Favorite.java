package com.capstone.tansiri.map.entity;

    public class Favorite {
        private Long id;
        private String startName;         // 출발지 이름
        private String endName;           // 목적지 이름
        private String startLat;          // 출발지 위도
        private String startLon;          // 출발지 경도
        private String endLat;            // 목적지 위도
        private String endLon;            // 목적지 경도
        private String response;           // 길찾기 API의 응답
        private String userId;            // 사용자 ID
        private boolean isFirstClick = false;

        public Favorite(String startName, String endName, String startLat, String startLon,
                        String endLat, String endLon, String response, String userId) {
            this.startName = startName;
            this.endName = endName;
            this.startLat = startLat;
            this.startLon = startLon;
            this.endLat = endLat;
            this.endLon = endLon;
            this.response = response;
            this.userId = userId;
        }

        public Long getId() {
            return id;
        }

        public String getEndName() {
            return endName;
        }

        public boolean isFirstClick() {
            return isFirstClick;
        }

        public void setFirstClick(boolean isFirstClick) {
            this.isFirstClick = isFirstClick;
        }

    }
