package com.capstone.tansiri.map;

import com.capstone.tansiri.map.entity.Favorite;
import com.capstone.tansiri.map.entity.Poi;
import com.capstone.tansiri.map.entity.Start;
import com.capstone.tansiri.map.entity.UserState;
import com.capstone.tansiri.map.entity.WalkRoute;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

import java.util.List;

public interface ApiService {

    @POST("/start")
    Call<Start> createStart(@Body Start start);

    @POST("/endSearch")
    Call<Poi> searchEndPoi(@Body Poi.SearchRequest request);
    // 경로 찾기 API 호출
    @POST("/findWalkRoute")
    Call<Void> findRoute(@Body WalkRoute walkRoute);

    @GET("/getWalkRoute/{userID}")
    Call<WalkRoute> getWalkRoute(@Path("userID") String userID);

    @POST("/userstate")
    Call<UserState> createUserState(@Body UserState userState);

    @POST("/favorite/save")
    Call<Favorite> saveFavorite(@Body Favorite favorite);

    // 중복 확인 메서드
    @POST("/favorite/check-duplicate")
    Call<Boolean> checkDuplicateFavorite(@Body Favorite favorite);

    @GET("/favorite/{userId}")
    Call<List<Favorite>> getFavoritesByUserId(@Path("userId") String userId);

    @DELETE("favorite/{id}")
    Call<Void> deleteFavorite(@Path("id") long id); // ID로 즐겨찾기 삭제
}
