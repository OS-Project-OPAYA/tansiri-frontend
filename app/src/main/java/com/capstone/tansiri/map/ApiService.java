package com.capstone.tansiri.map;

import com.capstone.tansiri.map.entity.Poi;
import com.capstone.tansiri.map.entity.Start;
import com.capstone.tansiri.map.entity.WalkRoute;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {

    @POST("/start")
    Call<Start> createStart(@Body Start start);

    @POST("/endSearch")
    Call<Poi> searchEndPoi(@Body String endName);

    // 경로 찾기 API 호출
    @POST("/api/route")
    Call<Void> findRoute(@Body WalkRoute walkRoute);

    //@GET("/api/getRoute")
    //Call<WalkRoute> getLatestWalkRoute();

    @GET("/api/getLatestWalkRoute")
    Call<WalkRoute> getLatestWalkRoute();



}
