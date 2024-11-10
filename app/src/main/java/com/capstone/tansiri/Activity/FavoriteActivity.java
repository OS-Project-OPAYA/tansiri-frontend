package com.capstone.tansiri.Activity;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.capstone.tansiri.map.FavoriteAdapter;
import com.capstone.tansiri.R;
import com.capstone.tansiri.map.Util;
import com.capstone.tansiri.map.RetrofitClient;
import com.capstone.tansiri.map.ApiService;
import com.capstone.tansiri.map.entity.Favorite;
import com.capstone.tansiri.map.entity.Poi;
import com.capstone.tansiri.map.entity.Start;
import com.capstone.tansiri.map.entity.WalkRoute;
import android.speech.tts.TextToSpeech;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.widget.Button;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FavoriteActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FavoriteAdapter favoriteAdapter;
    private String userID; // 사용자 ID를 할당하세요.
    private TextToSpeech tts;


    private String currentLatitude;  // 현재 위도
    private String currentLongitude; // 현재 경도
    private String currentAddress;   // 현재 주소
    private ApiService apiService;

    //클릭상태를 저장하는 변수,
    private Map<Integer, Boolean> clickStates = new HashMap<>();
    private Handler handler = new Handler();
    private Map<Integer, Runnable> resetClickStateRunnables = new HashMap<>();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.KOREA);
                    if (result == TextToSpeech.ERROR) {
                        // 언어 설정 실패
                    }
                }
            }
        });

        apiService = RetrofitClient.getClient().create(ApiService.class);


        Button addFavoriteButton = findViewById(R.id.btn_add_favorite);
        addFavoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // SearchActivity로 이동
                Intent intent = new Intent(FavoriteActivity.this, SearchActivity.class);
                startActivity(intent);
            }
        });

        userID = Util.getDeviceID(getApplicationContext());
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), LinearLayoutManager.VERTICAL);
        dividerItemDecoration.setDrawable(ContextCompat.getDrawable(this, R.drawable.divider));
        recyclerView.addItemDecoration(dividerItemDecoration);



        requestLocationPermission();
        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                tts.setLanguage(Locale.KOREAN);
            }
        });
        initialize();

        // API를 통해 즐겨찾기 목록 가져오기
        loadFavorites();
    }
    private void initialize() {
        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                tts.setLanguage(Locale.KOREAN);
            }
        });

        apiService = RetrofitClient.getClient().create(ApiService.class);
        userID = Util.getDeviceID(getApplicationContext());

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), LinearLayoutManager.VERTICAL);
        dividerItemDecoration.setDrawable(ContextCompat.getDrawable(this, R.drawable.divider));
        recyclerView.addItemDecoration(dividerItemDecoration);

        requestLocationPermission();
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                Geocoder geocoder = new Geocoder(FavoriteActivity.this, Locale.getDefault());
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

    // 클릭 상태와 타이머를 위한 변수 선언
    // 클릭 상태와 타이머를 위한 변수 선언
    boolean isFirstClick = false;
//    Handler handler = new Handler();
//    Runnable resetClickStateRunnable; // Runnable 변수 미리 선언

    private void loadFavorites() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<List<Favorite>> call = apiService.getFavoritesByUserId(userID);

        call.enqueue(new Callback<List<Favorite>>() {
            @Override
            public void onResponse(Call<List<Favorite>> call, Response<List<Favorite>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Favorite> favorites = response.body();
                    favoriteAdapter = new FavoriteAdapter(favorites,
                            favorite -> {
                                // 삭제 버튼 클릭 시 확인 대화 상자 표시
                                showDeleteConfirmationDialog(favorite);
                            },
                            favorite -> {
                                int favoriteId = Math.toIntExact(favorite.getId()); // 각 즐겨찾기의 고유 ID

                                // 해당 항목의 상태를 가져오고 없으면 초기화
                                boolean isFirstClick = clickStates.getOrDefault(favoriteId, false);

                                if (isFirstClick) { // 두 번째 클릭 시 실행
                                    tts.speak("경로를 탐색중입니다", TextToSpeech.QUEUE_FLUSH, null, null);
                                    sendLocationToServer(currentAddress, currentLatitude, currentLongitude, favorite.getEndName(), userID);

                                    // 상태 초기화
                                    clickStates.put(favoriteId, false);
                                    handler.removeCallbacks(resetClickStateRunnables.get(favoriteId));

                                } else { // 첫 번째 클릭 시 TTS 안내
                                    tts.speak(favorite.getEndName(), TextToSpeech.QUEUE_FLUSH, null, null);
                                    clickStates.put(favoriteId, true);

                                    // 클릭 상태 초기화: 2초 안에 두 번 클릭해야 함
                                    Runnable resetRunnable = () -> clickStates.put(favoriteId, false);
                                    resetClickStateRunnables.put(favoriteId, resetRunnable);
                                    handler.postDelayed(resetRunnable, 2000); // 2초 후 상태 초기화
                                }
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



    private void sendLocationToServer(String addressName, String latitude, String longitude, String endName, String userID) {
        // Start 객체 생성
        Start start = new Start(addressName, latitude, longitude, userID);

        // 서버로 POST 요청
        Call<Start> call = apiService.createStart(start);
        call.enqueue(new Callback<Start>() {
            @Override
            public void onResponse(Call<Start> call, Response<Start> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(FavoriteActivity.this, "현위치 전송 성공", Toast.LENGTH_SHORT).show();
                    // 목적지 검색 요청 전송
                    sendSearchToServer(endName, userID);
                } else {
                    Toast.makeText(FavoriteActivity.this, "현위치 전송 실패", Toast.LENGTH_SHORT).show();
                    Log.e("API_ERROR", "Error: " + response.errorBody());
                }
            }

            @Override
            public void onFailure(Call<Start> call, Throwable t) {
                Toast.makeText(FavoriteActivity.this, "서버 통신 실패: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.d("SearchActivity", "서버 통신 실패: " + t.getMessage());
            }
        });
    }

    private void sendSearchToServer(String endName, String userID) {
        // SearchRequest 객체 생성
        Poi.SearchRequest searchRequest = new Poi.SearchRequest();
        searchRequest.setKeyword(endName); // 검색할 키워드 설정
        searchRequest.setUserID(userID);    // 사용자 ID 설정

        // API 호출
        Call<Poi> call = apiService.searchEndPoi(searchRequest);
        call.enqueue(new Callback<Poi>() {
            @Override
            public void onResponse(Call<Poi> call, Response<Poi> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Poi poi = response.body(); // POI 객체를 가져옴
                    String name = poi.getName();
                    String latitude = poi.getFrontLat();
                    String longitude = poi.getFrontLon();

                    Log.d("API_RESPONSE", "Name: " + name + ", Latitude: " + latitude + ", Longitude: " + longitude);
                    Toast.makeText(FavoriteActivity.this, "목적지 검색 성공", Toast.LENGTH_SHORT).show();

                    // WalkingRoute 객체 생성
                    WalkRoute walkRoute = new WalkRoute(currentLongitude, currentLatitude, longitude, latitude, currentAddress, name, userID);

                    // 경로 찾기 요청 전송 및 경로 정보 가져오기
                    findAndRetrieveRouteOnServer(walkRoute);

                } else {
                    Log.e("API_ERROR", "Error: " + response.errorBody());
                    Toast.makeText(FavoriteActivity.this, "목적지 검색 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Poi> call, Throwable t) {
                Log.e("API_ERROR", "Failed: " + t.getMessage());
                Toast.makeText(FavoriteActivity.this, "서버 통신 실패: " + t.getMessage(), Toast.LENGTH_LONG).show();
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
                    Toast.makeText(FavoriteActivity.this, "경로 생성 성공!", Toast.LENGTH_SHORT).show();
                    getWalkRoute(walkRoute.getUserID());

                } else {
                    Log.e("API_ERROR", "Error: " + response.errorBody());
                    Toast.makeText(FavoriteActivity.this, "경로 생성 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("API_ERROR", "Failed: " + t.getMessage());
                Toast.makeText(FavoriteActivity.this, "서버 통신 실패: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // 서버에서 유저의 가장 최근 도보 경로 불러오기
    private void getWalkRoute(String userID) {
        Call<WalkRoute> call = apiService.getWalkRoute(userID);
        call.enqueue(new Callback<WalkRoute>() {
            @Override
            public void onResponse(Call<WalkRoute> call, Response<WalkRoute> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WalkRoute walkRoute = response.body();
                    // 경로 정보를 사용하여 필요한 작업 수행


                    Toast.makeText(FavoriteActivity.this, "경로 가져오기 성공: " + walkRoute.getEndName(), Toast.LENGTH_SHORT).show();

                    // startX, startY, endX, endY 추출
                    String startName = walkRoute.getStartName();
                    String endName = walkRoute.getEndName();
                    String startX = walkRoute.getStartX();
                    String startY = walkRoute.getStartY();
                    String endX = walkRoute.getEndX();
                    String endY = walkRoute.getEndY();
                    String res = walkRoute.getResponse();
                    String userID = walkRoute.getUserID();

                    goToWalkingRouteActivity(startName, endName, startX, startY, endX, endY, res, userID);
                } else {
                    Toast.makeText(FavoriteActivity.this, "경로 가져오기 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WalkRoute> call, Throwable t) {
                Toast.makeText(FavoriteActivity.this, "서버 통신 실패: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // WalkingRoute 화면으로 이동하는 메서드
    private void goToWalkingRouteActivity(String currentAddress, String destinationAddress, String startX, String startY, String endX, String endY, String response, String userID) {
        Intent intent = new Intent(FavoriteActivity.this, WalkingRouteActivity.class);
        intent.putExtra("currentAddress", currentAddress);
        intent.putExtra("destinationAddress", destinationAddress);
        intent.putExtra("startX", startX);
        intent.putExtra("startY", startY);
        intent.putExtra("endX", endX);
        intent.putExtra("endY", endY);
        intent.putExtra("response", response);
        intent.putExtra("userID", userID);
        startActivity(intent);
        finish();
    }


    private void showDeleteConfirmationDialog(Favorite favorite) {
        new AlertDialog.Builder(this)
                .setTitle("즐겨찾기 삭제")
                .setMessage("정말로 이 즐겨찾기를 삭제하시겠습니까?")
                .setPositiveButton("예", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteFavorite(favorite.getId());
                    }
                })
                .setNegativeButton("아니오", null)
                .show();
    }

    private void deleteFavorite(long favoriteId) {
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

    protected void onPause() {
        super.onPause();
        // 위치 관리자에서 위치 업데이트 해제
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(locationListener);
        }
    }
    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }


}