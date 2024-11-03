package com.capstone.tansiri;

import com.capstone.tansiri.map.ApiService;
import com.capstone.tansiri.map.RetrofitClient;
import com.capstone.tansiri.map.entity.Favorite;

import android.content.Context;
import android.content.Intent;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import android.location.Location;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;


import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.Priority;
import com.skt.tmap.TMapPoint;
import com.skt.tmap.TMapView;
import com.skt.tmap.overlay.TMapMarkerItem;
import com.skt.tmap.overlay.TMapPolyLine;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;




public class WalkingRouteActivity extends AppCompatActivity {

    private TMapView tMapView; // TMapView 객체 생성
    private static final String TAG = "WalkingRoute"; // TAG 변수 정의

    private ApiService apiService;

    private Button btnWalkRouteNavi;
    private Button btnResearch;
    private Button btnFavorite;
    private Button btnObjectRecognition;

    private double userLat = 0.0;
    private double userLon = 0.0;
    private float userDir = 0.0f; // 초기 방위각 값

    private Handler handler = new Handler(); // 핸들러 선언
    private Runnable logRunnable; // 위치 로그용 Runnable


    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
    private SensorEventListener sensorEventListener;
    private boolean isNavigating = false;



    private String currentAddress;
    private String currentLatitude;
    private String currentLongitude;
    private String destinationAddress;
    private String destinationLatitude;
    private String destinationLongitude;
    private String walkrouteResponse;
    private String userID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walking_route);

        //버튼 초기화
        btnWalkRouteNavi = findViewById(R.id.btnWalkRouteNavi);
        btnResearch = findViewById(R.id.btnResearch);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnObjectRecognition = findViewById(R.id.btnObjectRecognition);


        apiService = RetrofitClient.getClient().create(ApiService.class);


        // 센서 매니저 초기화
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        currentAddress = getIntent().getStringExtra("currentAddress");
        destinationAddress = getIntent().getStringExtra("destinationAddress");
        currentLatitude = getIntent().getStringExtra("startY");
        currentLongitude = getIntent().getStringExtra("startX");
        destinationLatitude = getIntent().getStringExtra("endY");
        destinationLongitude = getIntent().getStringExtra("endX");
        walkrouteResponse = getIntent().getStringExtra("response");
        userID = getIntent().getStringExtra("userID");


        // TMapView 초기화
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey("EHDhTt6iqk7HwqS2AirSY71g65xVG8Rp3LtZaIIx");

        // TMapView를 추가할 LinearLayout 참조
        LinearLayout linearLayoutTmap = findViewById(R.id.linearLayoutTmap);
        linearLayoutTmap.addView(tMapView);

        // TMapView 설정 및 경로 그리기
        tMapView.setOnMapReadyListener(new TMapView.OnMapReadyListener() {
            @Override
            public void onMapReady() {
                setInitialMapPosition();
                drawRoute();
            }
        });

        // 위치 요청 설정
        setupLocationCallback();
        setupLocationRequest();

        // 버튼 클릭 리스너 설정
        setupButtonListeners();
    }

    private void setInitialMapPosition() {
        Double clat = Double.parseDouble(currentLatitude);
        Double clon = Double.parseDouble(currentLongitude);
        tMapView.setCenterPoint(clat, clon); // 초기 위치로 중심 설정
        tMapView.setZoomLevel(14); // 기본 줌 레벨 설정
    }

    private void setUserMarker() {
        tMapView.setCenterPoint(userLat, userLon); // 초기 위치로 중심 설정

        // 기존 마커가 있는 경우 제거
        if (tMapView.getMarkerItemFromId("userMarker") != null) {
            tMapView.removeTMapMarkerItem("userMarker");
        }

        // 새 마커 추가
        TMapMarkerItem marker = new TMapMarkerItem();
        marker.setId("userMarker");
        marker.setTMapPoint(new TMapPoint(userLat, userLon));
        tMapView.addTMapMarkerItem(marker);
    }






    private void drawRoute() {
        try {
            ArrayList<TMapPoint> pointList = new ArrayList<>();
            JSONObject jsonResponse = new JSONObject(walkrouteResponse);
            JSONArray featuresArray = jsonResponse.getJSONArray("features");

            for (int i = 0; i < featuresArray.length(); i++) {
                JSONObject feature = featuresArray.getJSONObject(i);
                JSONObject geometry = feature.getJSONObject("geometry");

                if (geometry.getString("type").equals("LineString")) {
                    JSONArray coordinates = geometry.getJSONArray("coordinates");

                    for (int j = 0; j < coordinates.length(); j++) {
                        JSONArray coord = coordinates.getJSONArray(j);
                        if (coord.length() >= 2) {
                            double lon = coord.getDouble(0);
                            double lat = coord.getDouble(1);
                            pointList.add(new TMapPoint(lat, lon));
                        }
                    }
                }
            }

            TMapPolyLine line = new TMapPolyLine("line1", pointList);
            tMapView.addTMapPolyLine(line);

        } catch (Exception e) {
            Log.e(TAG, "Error parsing response JSON", e);
        }
    }

    private void setupLocationRequest() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000) // 5초 간격
                .setMinUpdateIntervalMillis(2000) // 최소 업데이트 간격 (2초)
                .setWaitForAccurateLocation(false) // 보다 정확한 위치를 기다리도록 설정
                .build(); // 빌더를 호출하여 LocationRequest 객체를 생성

        try {
            if (locationCallback != null) {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            } else {
                Log.e(TAG, "locationCallback이 null입니다.");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "위치 권한 필요", e);
        }
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                // TMapView가 null인지 확인
                if (tMapView != null) {
                    Location location = locationResult.getLastLocation();
                    if (location != null) {
                        userLat = location.getLatitude();
                        userLon = location.getLongitude();
                    }
                } else {
                    Log.e("WalkingRouteActivity", "TMapView가 초기화되지 않았습니다.");
                }
            }
        };
    }


    private void setupButtonListeners() {
        // 목적지 재검색 버튼 리스너
        btnResearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToSearchActivity();
            }
        });

        // 즐겨찾기 등록 버튼 리스너
        btnFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveToFavorites();
            }
        });

        // 도보 경로 안내 버튼 리스너
        btnWalkRouteNavi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isNavigating) {
                    // 네비게이션 시작
                    startNavigate(walkrouteResponse);
                    btnWalkRouteNavi.setText("안내 종료"); // 버튼 텍스트 변경
                    hideOtherButtons(); // 다른 버튼 숨기기
                    isNavigating = true; // 네비게이션 상태 업데이트
                } else {
                    // 네비게이션 종료
                    stopNavigate(); // 네비게이션 중지
                    btnWalkRouteNavi.setText("도보 경로 안내"); // 버튼 텍스트 원래대로 변경
                    showOtherButtons(); // 다른 버튼 보이기
                    isNavigating = false; // 네비게이션 상태 업데이트
                    goToSearchActivity(); // 목적지 검색 창으로 이동
                    Toast.makeText(WalkingRouteActivity.this, "경로 안내가 종료되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }

    // 다른 버튼 숨기기
    private void hideOtherButtons() {
        btnResearch.setVisibility(View.GONE);
        btnFavorite.setVisibility(View.GONE);
    }

    // 다른 버튼 보이기
    private void showOtherButtons() {
        btnResearch.setVisibility(View.VISIBLE);
        btnFavorite.setVisibility(View.VISIBLE);
    }




    private void saveToFavorites() {
        Favorite favorite = new Favorite(currentAddress, destinationAddress, currentLatitude, currentLongitude, destinationLatitude, destinationLongitude, walkrouteResponse, userID);
        Call<Boolean> checkDuplicateCall = apiService.checkDuplicateFavorite(favorite);
        checkDuplicateCall.enqueue(new Callback<Boolean>() {
            @Override
            public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean isDuplicate = response.body();
                    if (!isDuplicate) {
                        // 중복이 아닐 경우에만 저장
                        Call<Favorite> saveCall = apiService.saveFavorite(favorite);
                        saveCall.enqueue(new Callback<Favorite>() {
                            @Override
                            public void onResponse(Call<Favorite> call, Response<Favorite> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(getApplicationContext(), "즐겨찾기에 저장되었습니다!", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<Favorite> call, Throwable t) {
                                Log.e("API_ERROR", "Save Failure: " + t.getMessage());
                            }
                        });
                    } else {
                        Toast.makeText(getApplicationContext(), "이미 즐겨찾기에 저장된 항목입니다.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e("API_ERROR", "중복 확인 실패 - Code: " + response.code() + ", Message: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Boolean> call, Throwable t) {
                Log.e("API_ERROR", "Check Duplicate Failure: " + t.getMessage());
            }
        });
    }


    private void startNavigate(String walkrouteResponse) {
        ArrayList<TMapPoint> waypoints = new ArrayList<>();
        ArrayList<String> descriptions = new ArrayList<>();

        btnObjectRecognition.setVisibility(View.VISIBLE);


        try {
            JSONObject jsonResponse = new JSONObject(walkrouteResponse);
            JSONArray featuresArray = jsonResponse.getJSONArray("features");

            for (int i = 0; i < featuresArray.length(); i++) {
                JSONObject feature = featuresArray.getJSONObject(i);
                JSONObject geometry = feature.getJSONObject("geometry");
                if (geometry.getString("type").equals("Point")) {
                    JSONArray coordinates = geometry.getJSONArray("coordinates");
                    double lon = coordinates.getDouble(0);
                    double lat = coordinates.getDouble(1);
                    waypoints.add(new TMapPoint(lat, lon));

                    String description = feature.getJSONObject("properties").getString("description");
                    descriptions.add(description);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing response JSON", e);
            return;
        }

        setupSensorListener(); // 방위각 감지 시작


        logRunnable = new Runnable() {
            private int currentWaypointIndex = 0;

            @Override
            public void run() {
                if (currentWaypointIndex >= waypoints.size()) {
                    Toast.makeText(WalkingRouteActivity.this, "목적지에 도착했습니다.", Toast.LENGTH_SHORT).show();
                    stopNavigate();
                    return;
                }

                TMapPoint currentWaypoint = waypoints.get(currentWaypointIndex);
                double waypointLat = currentWaypoint.getLatitude();
                double waypointLon = currentWaypoint.getLongitude();

                double distanceToWaypoint = calculateDistance(userLat, userLon, waypointLat, waypointLon);

                setUserMarker(); // 위치 로그가 업데이트될 때마다 마커 설정


                if (isCloseToWaypoint(userLat, userLon, waypointLat, waypointLon)) {
                    Toast.makeText(WalkingRouteActivity.this, descriptions.get(currentWaypointIndex), Toast.LENGTH_SHORT).show();
                    currentWaypointIndex++;
                } else {
                    double targetBearing = calculateBearing(userLat, userLon, waypointLat, waypointLon);
                    String directionMessage = getDirectionMessage(userDir, targetBearing);
                    String distanceMessage = String.format("다음 거점까지의 거리: %.2f m | 방향: %s |", distanceToWaypoint, directionMessage);
                    Toast.makeText(WalkingRouteActivity.this, distanceMessage, Toast.LENGTH_SHORT).show();
                }

                handler.postDelayed(this, 3000); // 3초마다 업데이트
            }
        };

        handler.post(logRunnable);
        Toast.makeText(this, "위치 로그 시작", Toast.LENGTH_SHORT).show();
    }


    // 위치 로그 중단 메서드
    private void stopNavigate() {
        if (logRunnable != null) {
            handler.removeCallbacks(logRunnable); // Runnable 중단
        }
        Toast.makeText(this, "위치 로그 중단", Toast.LENGTH_SHORT).show();
        btnObjectRecognition.setVisibility(View.GONE);
    }


    // 두 좌표 간의 거리 계산 (단위: 미터)
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0];
    }


    // 현재 위치와 목표 지점이 가까운지 확인하는 메서드
    private boolean isCloseToWaypoint(double currentLat, double currentLon, double waypointLat, double waypointLon) {
        float[] results = new float[1];
        Location.distanceBetween(currentLat, currentLon, waypointLat, waypointLon, results);
        return results[0] < 5; // 10m 이내에 있는 경우
    }


    private double calculateBearing(double startLat, double startLon, double endLat, double endLon) {
        // 위도와 경도를 라디안으로 변환
        double startLatRad = Math.toRadians(startLat);
        double endLatRad = Math.toRadians(endLat);
        double deltaLon = Math.toRadians(endLon - startLon);

        // 방위각 계산
        double y = Math.sin(deltaLon) * Math.cos(endLatRad);
        double x = Math.cos(startLatRad) * Math.sin(endLatRad) - Math.sin(startLatRad) * Math.cos(endLatRad) * Math.cos(deltaLon);

        // 방위각을 도 단위로 변환하고 0도에서 360도 범위로 조정
        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360) % 360;
    }



    // 사용자의 현재 방위각과 목표 방위각을 비교하여 방향 결정
    private String getDirectionMessage(double userDir, double targetBearing) {
        double angleDifference = (targetBearing - userDir + 360) % 360;

        if (angleDifference < 15 || angleDifference > 345) {
            return "직진";
        } else if (angleDifference >= 15 && angleDifference < 180) {
            return "우회전";
        } else {
            return "좌회전";
        }
    }

    // 방위각 업데이트 리스너 설정
    private void setupSensorListener() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                    float[] rotationMatrix = new float[9];
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);

                    // 축을 재조정하여 휴대폰을 세운 상태에서도 방위각이 정확히 계산되도록 설정
                    float[] adjustedRotationMatrix = new float[9];
                    SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, adjustedRotationMatrix);

                    float[] orientation = new float[3];
                    SensorManager.getOrientation(adjustedRotationMatrix, orientation);

                    userDir = (float) Math.toDegrees(orientation[0]);
                    if (userDir < 0) {
                        userDir += 360;
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };

        sensorManager.registerListener(sensorEventListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
    }






    // WalkingRoute 화면으로 이동하는 메서드
    private void goToSearchActivity() {
        Intent intent = new Intent(WalkingRouteActivity.this, SearchActivity.class);
        startActivity(intent);
        finish();
    }



    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null && sensorEventListener != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }

        // WalkingRouteActivity 화면에서 나갈 때, 안내가 진행 중이면 종료
        if (isNavigating) {
            stopNavigate();
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
        if (tMapView != null) {
            tMapView = null;
        }
    }
}