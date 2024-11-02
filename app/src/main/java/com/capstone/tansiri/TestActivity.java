package com.capstone.tansiri;

import android.location.Location; // Location 클래스를 가져옵니다.
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.skt.tmap.TMapGpsManager;
import com.skt.tmap.TMapView;

public class TestActivity extends AppCompatActivity implements TMapGpsManager.OnLocationChangedListener {

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
    public void onLocationChange(Location location) {
        // 위치 변경 시 호출되는 메서드
        if (location != null && tMapView != null) { // tMapView null 체크 추가
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            Toast.makeText(this, "위치 변경: " + latitude + ", " + longitude, Toast.LENGTH_SHORT).show();

            // 추가로 위치를 지도에 업데이트할 수 있음
            tMapView.setCenterPoint(longitude, latitude); // 새로운 위치로 지도 중심 이동
        }
    }




}
