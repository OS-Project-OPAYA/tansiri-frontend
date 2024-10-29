package com.capstone.tansiri;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.skt.tmap.TMapView;

public class test extends AppCompatActivity {

    private TMapView tMapView; // TMapView 객체 생성

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test); // activity_test.xml 레이아웃 사용

        // TMapView 초기화
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey("EHDhTt6iqk7HwqS2AirSY71g65xVG8Rp3LtZaIIx"); // 발급받은 API 키 설정

        // TMapView 리스너 설정
        tMapView.setOnMapReadyListener(new TMapView.OnMapReadyListener() {
            @Override
            public void onMapReady() {

                // 지도 로딩 완료 후 구현할 코드
                tMapView.setCenterPoint(36.6256021, 127.4542816);
                tMapView.setZoomLevel(18); // 기본 줌 레벨 설정
                tMapView.setCompassMode(true);
                tMapView.setTrackingMode(true);



            }
        });

        // TMapView를 레이아웃에 추가
        LinearLayout container = findViewById(R.id.mapContainer); // 지도 컨테이너
        container.addView(tMapView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tMapView != null) {
            tMapView = null; // TMapView 메모리 해제
        }
    }
}
