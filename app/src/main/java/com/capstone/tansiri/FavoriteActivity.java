package com.capstone.tansiri;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.capstone.tansiri.map.RetrofitClient;
import com.capstone.tansiri.map.ApiService;
import com.capstone.tansiri.map.entity.Favorite;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.widget.Button;


import java.util.List;

public class FavoriteActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FavoriteAdapter favoriteAdapter;
    private String userId; // 사용자 ID를 할당하세요.

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        Button addFavoriteButton = findViewById(R.id.btn_add_favorite);
        addFavoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // SearchActivity로 이동
                Intent intent = new Intent(FavoriteActivity.this, SearchActivity.class);
                startActivity(intent);
                finish();
            }
        });



        userId = Util.getDeviceID(getApplicationContext());
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // API를 통해 즐겨찾기 목록 가져오기
        loadFavorites();
    }

    private void loadFavorites() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<List<Favorite>> call = apiService.getFavoritesByUserId(userId);

        call.enqueue(new Callback<List<Favorite>>() {
            @Override
            public void onResponse(Call<List<Favorite>> call, Response<List<Favorite>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Favorite> favorites = response.body();
                    favoriteAdapter = new FavoriteAdapter(favorites, favorite -> {
                        // 삭제 버튼 클릭 시 호출되는 메서드
                        deleteFavorite(favorite.getId()); // favorite.getId()는 삭제할 즐겨찾기의 ID입니다.
                    });
                    recyclerView.setAdapter(favoriteAdapter);
                } else {
                    Log.e("API_ERROR", "Response Code: " + response.code());
                    Toast.makeText(FavoriteActivity.this, "즐겨찾기 로드 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Favorite>> call, Throwable t) {
                Log.e("API_ERROR", "Communication Error: " + t.getMessage());
                Toast.makeText(FavoriteActivity.this, "통신 오류 발생", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteFavorite(long favoriteId) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<Void> call = apiService.deleteFavorite(favoriteId); // 즐겨찾기 삭제 API 호출

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(FavoriteActivity.this, "즐겨찾기가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    loadFavorites(); // 삭제 후 즐겨찾기 목록 새로고침
                } else {
                    Log.e("API_ERROR", "Failed to delete favorite: " + response.code());
                    Toast.makeText(FavoriteActivity.this, "즐겨찾기 삭제 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("API_ERROR", "Communication Error: " + t.getMessage());
                Toast.makeText(FavoriteActivity.this, "통신 오류 발생", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
