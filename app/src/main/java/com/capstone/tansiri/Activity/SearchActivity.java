package com.capstone.tansiri.Activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.capstone.tansiri.R;
import com.capstone.tansiri.map.Util;
import com.capstone.tansiri.map.ApiService;
import com.capstone.tansiri.map.RetrofitClient;
import com.capstone.tansiri.map.entity.Poi;
import com.capstone.tansiri.map.entity.Start;
import com.capstone.tansiri.map.entity.WalkRoute;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.speech.tts.TextToSpeech;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_STT = 100;
    private ApiService apiService;
    private EditText searchInput; // 검색어 입력 필드
    private String currentLatitude;  // 현재 위도
    private String currentLongitude; // 현재 경도
    private String currentAddress;   // 현재 주소
    private String userID;
    private ActivityResultLauncher<Intent> sttLauncher;
    private TextToSpeech tts;
    private boolean isFirstClick = false;
    private Handler handler = new Handler();
    private Runnable resetClickStateRunnable;
    private Vibrator vibrator;
    private long lastClickTime = 0;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);



        // 고유 기기 ID 가져오기
        userID = Util.getDeviceID(getApplicationContext());

        // Retrofit 인스턴스 생성 및 ApiService 설정
        apiService = RetrofitClient.getClient().create(ApiService.class);

        // UI 요소 초기화
        searchInput = findViewById(R.id.destinationEditText);
        // 검색 버튼
        Button searchButton = findViewById(R.id.searchButton);
        ImageButton sttButton = findViewById(R.id.sttButton);


        // 위치 권한 요청
        requestLocationPermission();
        //TTS

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.KOREAN); // 한국어로 설정
                tts.speak("목적지 검색 화면입니다.", TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
            }
        });


        // STT (음성 인식)
        sttButton.setOnClickListener(v -> {
            // 진동 추가 (버튼 클릭 시 진동 효과)
            vibrator = (Vibrator) v.getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)); // 0.1초 진동
            }

            // 버튼 클릭 시 효과 추가 (버튼 크기 작아짐)
            v.setScaleX(0.95f);
            v.setScaleY(0.95f);

            // 현재 시간 가져오기
            long currentTime = System.currentTimeMillis();

            // 두 번 클릭 인식 (0.3초 이내에 두 번 클릭 시 음성 인식 실행)
            if (currentTime - lastClickTime < 300) {
                // 진행 중인 TTS 멈춤
                if (tts.isSpeaking()) {
                    tts.stop(); // 현재 진행 중인 TTS를 중지
                }
                startSTT(); // 두 번째 클릭 시 음성 인식 실행
            } else {
                // 첫 번째 클릭 시에는 TTS로 안내 음성 출력
                tts.speak("음성 인식을 하시려면 두 번 클릭하세요", TextToSpeech.QUEUE_FLUSH, null, null);
            }

            // 마지막 클릭 시간 업데이트
            lastClickTime = currentTime;

            // 버튼이 눌린 후 잠시 후에 원래 크기로 복원
            v.postDelayed(() -> {
                v.setScaleX(1.0f);
                v.setScaleY(1.0f);
            }, 200);
        });


        sttLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                Intent data = result.getData();
                if (data != null) {
                    ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (results != null && !results.isEmpty()) {
                        String recognizedText = results.get(0);
                        searchInput.setText(recognizedText); // 인식된 텍스트 입력
                    }
                }
            }
        });


        searchButton.setOnClickListener(v -> {
            vibrator = (Vibrator) v.getContext().getSystemService(Context.VIBRATOR_SERVICE);

            // 버튼이 눌렸을 때 작아지게 설정
            v.setScaleX(0.95f);
            v.setScaleY(0.95f);


            // 진동 추가
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)); // 0.1초 진동
            }

            if (isFirstClick) {
                tts.speak("로딩 중", TextToSpeech.QUEUE_FLUSH, null, null);

                if (currentLatitude != null && currentLongitude != null) {
                    String endName = searchInput.getText().toString();
                    if (!endName.isEmpty()) {
                        sendLocationToServer(currentAddress, currentLatitude, currentLongitude, endName, userID);
                    } else {
                        Toast.makeText(SearchActivity.this, "목적지를 입력하세요.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(SearchActivity.this, "위치 정보를 가져오는 중입니다.", Toast.LENGTH_SHORT).show();
                }

                isFirstClick = false;
                handler.removeCallbacks(resetClickStateRunnable);

            } else {
                tts.speak("검색하시려면 두 번 연속 클릭하세요", TextToSpeech.QUEUE_FLUSH, null, null);
                isFirstClick = true;

                resetClickStateRunnable = () -> isFirstClick = false;
                handler.postDelayed(resetClickStateRunnable, 300);
            }

            // 버튼이 눌린 후 잠시 후에 원래 크기로 복원
            v.postDelayed(() -> {
                v.setScaleX(1.0f);
                v.setScaleY(1.0f);
            }, 200);
        });

    }


    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            initializeLocationManager();
        }
    }

    private void startSTT() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREA);
        sttLauncher.launch(intent);
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
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    private void sendLocationToServer(String addressName, String latitude, String longitude, String endName, String userID) {
        // Start 객체 생성
        Start start = new Start(addressName, latitude, longitude, userID);

        // 서버로 POST 요청
        Call<Start> call = apiService.createStart(start);
        call.enqueue(new Callback<Start>() {
            @Override
            public void onResponse(Call<Start> call, Response<Start> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SearchActivity.this, "현위치 전송 성공", Toast.LENGTH_SHORT).show();
                    // 목적지 검색 요청 전송
                    sendSearchToServer(endName, userID);
                } else {
                    Toast.makeText(SearchActivity.this, "현위치 전송 실패", Toast.LENGTH_SHORT).show();
                    Log.e("API_ERROR", "Error: " + response.errorBody());
                }
            }

            @Override
            public void onFailure(Call<Start> call, Throwable t) {
                Toast.makeText(SearchActivity.this, "서버 통신 실패: " + t.getMessage(), Toast.LENGTH_LONG).show();
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
                    Toast.makeText(SearchActivity.this, "목적지 검색 성공", Toast.LENGTH_SHORT).show();

                    // WalkingRoute 객체 생성
                    WalkRoute walkRoute = new WalkRoute(currentLongitude, currentLatitude, longitude, latitude, currentAddress, name, userID);

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
                Toast.makeText(SearchActivity.this, "서버 통신 실패: " + t.getMessage(), Toast.LENGTH_LONG).show();
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
                    Toast.makeText(SearchActivity.this, "경로 생성 성공!", Toast.LENGTH_SHORT).show();
                    getWalkRoute(walkRoute.getUserID());

                } else {
                    Log.e("API_ERROR", "Error: " + response.errorBody());
                    Toast.makeText(SearchActivity.this, "경로 생성 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("API_ERROR", "Failed: " + t.getMessage());
                Toast.makeText(SearchActivity.this, "서버 통신 실패: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getWalkRoute(String userID) {
        Call<WalkRoute> call = apiService.getWalkRoute(userID);
        call.enqueue(new Callback<WalkRoute>() {
            @Override
            public void onResponse(Call<WalkRoute> call, Response<WalkRoute> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WalkRoute walkRoute = response.body();
                    String res = walkRoute.getResponse();

                    if (res.length() > 600000) {
                        Toast.makeText(SearchActivity.this, "경로 안내 제한", Toast.LENGTH_SHORT).show();
                        tts.speak("현위치에서 너무 먼 거리에 있거나 해당 목적지가 너무 많습니다", TextToSpeech.QUEUE_FLUSH, null, null);
                        return;
                    }

                    Toast.makeText(SearchActivity.this, "경로 가져오기 성공: " + walkRoute.getEndName(), Toast.LENGTH_SHORT).show();

                    String startName = walkRoute.getStartName();
                    String endName = walkRoute.getEndName();
                    String startX = walkRoute.getStartX();
                    String startY = walkRoute.getStartY();
                    String endX = walkRoute.getEndX();
                    String endY = walkRoute.getEndY();
                    String userID = walkRoute.getUserID();

                    goToWalkingRouteActivity(startName, endName, startX, startY, endX, endY, res, userID);
                } else {
                    Toast.makeText(SearchActivity.this, "경로 가져오기 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WalkRoute> call, Throwable t) {
                Toast.makeText(SearchActivity.this, "서버 통신 실패: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


    // WalkingRoute 화면으로 이동하는 메서드
    private void goToWalkingRouteActivity(String currentAddress, String destinationAddress, String startX, String startY, String endX, String endY, String response, String userID) {
        Intent intent = new Intent(SearchActivity.this, WalkingRouteActivity.class);
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

    private void onStartSTTButtonClicked() {
        requestAudioPermission(); // 권한 요청
        startSTT(); // STT 시작
    }

    private void requestAudioPermission() {
    }

    // STT 메서드 구현


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_STT && resultCode == RESULT_OK) {
            // STT 결과 처리
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0); // 첫 번째 결과 가져오기
                Toast.makeText(SearchActivity.this, "목적지를 입력하세요: " + spokenText, Toast.LENGTH_SHORT).show();
            }
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
