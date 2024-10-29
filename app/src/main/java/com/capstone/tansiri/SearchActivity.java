package com.capstone.tansiri;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.capstone.tansiri.map.ApiService;
import com.capstone.tansiri.map.RetrofitClient;
import com.capstone.tansiri.map.entity.Poi;
import com.capstone.tansiri.map.entity.Start;
import com.capstone.tansiri.map.entity.WalkRoute;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {

    private ApiService apiService;
    private EditText searchInput; // 검색어 입력 필드
    private String currentLatitude;  // 현재 위도
    private String currentLongitude; // 현재 경도
    private String currentAddress;   // 현재 주소

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Retrofit 인스턴스 생성 및 ApiService 설정
        apiService = RetrofitClient.getClient().create(ApiService.class);

        // UI 요소 초기화
        searchInput = findViewById(R.id.destinationEditText);
        // 검색 버튼
        Button searchButton = findViewById(R.id.searchButton);

        // 위치 권한 요청
        requestLocationPermission();

        // 검색 버튼 클릭 시
        searchButton.setOnClickListener(v -> {
            if (currentLatitude != null && currentLongitude != null) {
                String endName = searchInput.getText().toString();
                if (!endName.isEmpty()) {

                    // 현재 위치와 검색 장소를 서버로 전송
                    //sendLocationToServer(currentAddress, currentLatitude, currentLongitude, endName);
                    sendLocationToServer("현재위치", "36.6256013", "127.4542717", endName);

                } else {
                    Toast.makeText(SearchActivity.this, "목적지를 입력하세요.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(SearchActivity.this, "위치 정보를 가져오는 중입니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            initializeLocationManager();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeLocationManager();
            } else {
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initializeLocationManager() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // GPS와 네트워크 제공자로 위치 업데이트 요청
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 5, locationListener);
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                // 주소 정보 가져오기
                Geocoder geocoder = new Geocoder(SearchActivity.this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        currentAddress = address.getAddressLine(0); // 현재 주소 저장
                        currentLatitude = String.valueOf(latitude); // 현재 위도 저장
                        currentLongitude = String.valueOf(longitude); // 현재 경도 저장

                        Log.d("LOCATION", "Latitude: " + latitude + ", Longitude: " + longitude);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) { }

        @Override
        public void onProviderEnabled(String provider) { }

        @Override
        public void onProviderDisabled(String provider) { }
    };

    private void sendLocationToServer(String addressName, String latitude, String longitude, String endName) {
        // Start 객체 생성
        Start start = new Start(addressName, latitude, longitude);

        // 서버로 POST 요청
        Call<Start> call = apiService.createStart(start);
        call.enqueue(new Callback<Start>() {
            @Override
            public void onResponse(Call<Start> call, Response<Start> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SearchActivity.this, "위치 정보 전송 성공!", Toast.LENGTH_SHORT).show();
                    // 목적지 검색 요청 전송
                    sendSearchToServer(endName);
                } else {
                    Toast.makeText(SearchActivity.this, "위치 정보 전송 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Start> call, Throwable t) {
                Toast.makeText(SearchActivity.this, "서버 통신 실패: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.d("SearchActivity", "서버 통신 실패: " + t.getMessage());

            }
        });
    }

    private void sendSearchToServer(String endName) {
        Call<Poi> call = apiService.searchEndPoi(endName);

        call.enqueue(new Callback<Poi>() {
            @Override
            public void onResponse(Call<Poi> call, Response<Poi> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Poi poi = response.body(); // POI 객체를 가져옴
                    String name = poi.getName();
                    String latitude = poi.getFrontLat();
                    String longitude = poi.getFrontLon();

                    Log.d("API_RESPONSE", "Name: " + name + ", Latitude: " + latitude + ", Longitude: " + longitude);
                    Toast.makeText(SearchActivity.this, "목적지 검색 성공!", Toast.LENGTH_SHORT).show();

                    // WalkingRoute 객체 생성
                    WalkRoute walkRoute = new WalkRoute(currentLongitude, currentLatitude, longitude, latitude, currentAddress, name);

                    // 경로 찾기 요청 전송 및 경로 정보 가져오기
                    findAndRetrieveRouteOnServer(walkRoute);

                } else {
                    Log.e("API_ERROR", "Error: " + response.errorBody());
                    Toast.makeText(SearchActivity.this, "목적지 검색 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Poi> call, Throwable t) {
                Log.e("API_ERROR", "Failed: " + t.getMessage());
                Toast.makeText(SearchActivity.this, "목적지 검색 서버 통신 실패: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // 경로 찾기 요청과 동시에 경로 정보를 가져오는 메서드
    private void findAndRetrieveRouteOnServer(WalkRoute walkRoute) {
        Call<Void> call = apiService.findRoute(walkRoute);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SearchActivity.this, "경로 찾기 성공!", Toast.LENGTH_SHORT).show();

                    // 서버에서 최신 경로 정보 가져오기
                    getLatestRouteFromServer();

                } else {
                    Toast.makeText(SearchActivity.this, "경로 찾기 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(SearchActivity.this, "경로 찾기 서버 통신 실패: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // 서버에서 최신 경로 정보 가져오기
    private void getLatestRouteFromServer() {
        Call<WalkRoute> call = apiService.getLatestWalkRoute();
        call.enqueue(new Callback<WalkRoute>() {
            @Override
            public void onResponse(Call<WalkRoute> call, Response<WalkRoute> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WalkRoute walkRoute = response.body();
                    // 경로 정보를 사용하여 필요한 작업 수행


                    Toast.makeText(SearchActivity.this, "경로 정보 가져오기 성공: " + walkRoute.getStartName(), Toast.LENGTH_SHORT).show();

                    // startX, startY, endX, endY 추출
                    String startName = walkRoute.getStartName();
                    String endName = walkRoute.getEndName();
                    String startX = walkRoute.getStartX();
                    String startY = walkRoute.getStartY();
                    String endX = walkRoute.getEndX();
                    String endY = walkRoute.getEndY();
                    String res = walkRoute.getResponse();

                    goToWalkingRouteActivity(startName, endName, startX, startY, endX, endY, res);
                } else {
                    Toast.makeText(SearchActivity.this, "경로 정보 가져오기 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WalkRoute> call, Throwable t) {
                Toast.makeText(SearchActivity.this, "서버 통신 실패: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // WalkingRoute 화면으로 이동하는 메서드
    private void goToWalkingRouteActivity(String currentAddress, String destinationAddress, String startX, String startY, String endX, String endY, String response) {
        Intent intent = new Intent(SearchActivity.this, WalkingRouteActivity.class);
        intent.putExtra("currentAddress", currentAddress);
        intent.putExtra("destinationAddress", destinationAddress);
        intent.putExtra("startX", startX);
        intent.putExtra("startY", startY);
        intent.putExtra("endX", endX);
        intent.putExtra("endY", endY);
        intent.putExtra("response", response);
        startActivity(intent);
    }
}
