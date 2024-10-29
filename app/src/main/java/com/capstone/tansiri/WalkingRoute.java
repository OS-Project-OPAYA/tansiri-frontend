package com.capstone.tansiri;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.skt.tmap.TMapGpsManager;
import com.skt.tmap.TMapPoint;
import com.skt.tmap.TMapView;
import com.skt.tmap.overlay.TMapMarkerItem;

public class WalkingRoute extends AppCompatActivity implements TMapGpsManager.OnLocationChangedListener {

    private TMapView tMapView;
    private TMapGpsManager tMapGpsManager;

    // 위치 권한 요청을 위한 ActivityResultLauncher 선언
    private final ActivityResultLauncher<String[]> locationPermissionRequest = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                Boolean fineLocationGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                Boolean coarseLocationGranted = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);

                if (fineLocationGranted != null && fineLocationGranted) {
                    // 정확한 위치 접근 권한이 허용된 경우
                    startLocationUpdates();
                } else if (coarseLocationGranted != null && coarseLocationGranted) {
                    // 대략적인 위치 접근 권한이 허용된 경우
                    startLocationUpdates();
                } else {
                    // 위치 권한이 거부된 경우
                    Toast.makeText(this,
                            "위치 권한이 거부되었습니다.",
                            Toast.LENGTH_SHORT
                    ).show();
                    finish();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walking_route); // XML 파일 설정

        // TMapView를 추가할 LinearLayout 참조
        LinearLayout linearLayoutTmap = findViewById(R.id.linearLayoutTmap);

        // TMapView 생성
        tMapView = new TMapView(this);
        tMapView.onResume();


        // 발급받은 API 키 설정
        tMapView.setSKTMapApiKey("EHDhTt6iqk7HwqS2AirSY71g65xVG8Rp3LtZaIIx");

        // TMapView를 LinearLayout에 추가
        linearLayoutTmap.addView(tMapView);

        // TMapView의 크기 설정 (예: MATCH_PARENT)
        tMapView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        // TMapGpsManager 설정
        tMapGpsManager = new TMapGpsManager(this);
        tMapGpsManager.setMinTime(1000); // 위치 업데이트 최소 시간 (1초)
        tMapGpsManager.setMinDistance(5); // 위치 업데이트 최소 거리 (5미터)
        tMapGpsManager.setProvider(TMapGpsManager.PROVIDER_GPS); // GPS 기반 위치 탐색

        // 위치 권한 요청 시작
        requestLocationPermissions();




    }

    // 위치 권한 요청 메서드
    private void requestLocationPermissions() {
        locationPermissionRequest.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    // 위치 업데이트 시작 메서드
    private void startLocationUpdates() {
        tMapGpsManager.openGps(); // GPS 탐색 시작
    }

    @Override
    public void onLocationChange(Location location) {
        // 현재 위치가 변경될 때 호출됩니다.
        double lat = location.getLatitude(); // 위도
        double lon = location.getLongitude(); // 경도
        TMapPoint point = new TMapPoint(lat, lon);

        // 현재 위치로 TMapView 카메라 이동
        tMapView.setCenterPoint(lon, lat);
        tMapView.setZoomLevel(15); // 적절한 줌 레벨 설정

    }
}
