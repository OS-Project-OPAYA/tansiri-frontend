package com.capstone.tansiri;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 보행 경로 화면으로 전환하는 버튼
        Button buttonWalkingRoute = findViewById(R.id.btn_open_walking_route);
        buttonWalkingRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, WalkingRouteActivity.class);
                startActivity(intent);
            }
        });

        // 현재 위치와 방위각 화면으로 전환하는 버튼 추가
        Button buttonCurLocation = findViewById(R.id.btn_open_cur_location);
        buttonCurLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                startActivity(intent);
            }
        });

        // 추가 버튼 설정
        Button buttonA = findViewById(R.id.btn_a);
        buttonA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, test.class);
                startActivity(intent);
            }
        });

        Button buttonFavorite = findViewById(R.id.btn_open_favorite);
        buttonFavorite.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FavoriteActivity.class);
                startActivity(intent);
            }
        }));

    }


}
