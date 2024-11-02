package com.capstone.tansiri;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.capstone.tansiri.map.ApiService;
import com.capstone.tansiri.map.RetrofitClient;
import com.capstone.tansiri.map.entity.Favorite;
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



import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;



public class WalkingRouteActivity extends AppCompatActivity implements SensorEventListener {

    private TMapView tMapView; // TMapView 객체 생성
    private static final String TAG = "WalkingRoute"; // TAG 변수 정의

    private ApiService apiService;

    private Button btnWalkRouteNavi;
    private Button btnResearch;
    private Button btnFavorite;

    private double userLat = 0.0;
    private double userLon = 0.0;
    private float userDir = 0.0f; // 초기 방위각 값

    private Handler handler = new Handler(); // 핸들러 선언
    private LocationManager locationManager;
    private boolean isLogging = false; // 위치 로그 활성화 여부
    private Runnable logRunnable; // 위치 로그용 Runnable
    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;


    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;


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

        btnWalkRouteNavi = findViewById(R.id.btnWalkRouteNavi);
        btnResearch = findViewById(R.id.btnResearch);
        btnFavorite = findViewById(R.id.btnFavorite);

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
        setupLocationRequest();
        setupLocationCallback();

        // 버튼 클릭 리스너 설정
        setupButtonListeners();
    }

    private void setInitialMapPosition() {
        double startlat = Double.parseDouble(currentLatitude);
        double startlon = Double.parseDouble(currentLongitude);
        tMapView.setCenterPoint(startlat, startlon); // 초기 위치로 중심 설정
        tMapView.setZoomLevel(14); // 기본 줌 레벨 설정
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
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000) // 5초 간격
                .setMaxUpdates(10) // 최대 업데이트 수 설정 (옵션)
                .setMinUpdateIntervalMillis(2000) // 최소 업데이트 간격 (2초)
                .setWaitForAccurateLocation(true) // 보다 정확한 위치를 기다리도록 설정
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
                        // 위치를 TMapView의 중심으로 설정
                        tMapView.setCenterPoint(location.getLongitude(), location.getLatitude(), true);
                    }
                } else {
                    Log.e("WalkingRouteActivity", "TMapView가 초기화되지 않았습니다.");
                }
            }
        };
    }



    private void setupButtonListeners() {
        btnResearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToSearchActivity();
            }
        });

        btnFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveToFavorites();
            }
        });

        btnWalkRouteNavi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLogging = !isLogging; // 버튼 클릭 시 로그 활성화/비활성화 토글
                if (isLogging) {
                    startNavigate(walkrouteResponse);
                } else {
                    stopNavigate();
                }
            }
        });
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

                    Log.d(TAG, "Waypoint " + (i + 1) + " - Lat: " + lat + ", Lon: " + lon + ", Description: " + description);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing response JSON", e);
            return;
        }

        logRunnable = new Runnable() {
            private int currentWaypointIndex = 1;
            private boolean descriptionShown = false;

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

                // 방위각 계산
                double angleToWaypoint = calculateBearing(userLat, userLon, waypointLat, waypointLon);
                float directionToWaypoint = (float) (angleToWaypoint - userDir);
                if (directionToWaypoint < 0) {
                    directionToWaypoint += 360;
                }

                String directionMessage;
                if (directionToWaypoint < 30 || directionToWaypoint > 330) {
                    directionMessage = "Straight";
                } else if (directionToWaypoint > 30 && directionToWaypoint < 150) {
                    directionMessage = "Turn Right";
                } else {
                    directionMessage = "Turn Left";
                }

                // 현재 위치와 다음 거점까지의 거리 계산
                double distanceToWaypoint = calculateDistance(userLat, userLon, waypointLat, waypointLon);

                // 방위각, 남은 거리 포함한 안내 메시지 생성
                String message = directionMessage +
                        " - Distance: " + String.format("%.2f", distanceToWaypoint) + "m";
                Toast.makeText(WalkingRouteActivity.this, message, Toast.LENGTH_SHORT).show();

                if (currentWaypointIndex == 0 && !descriptionShown) {
                    Toast.makeText(WalkingRouteActivity.this, descriptions.get(currentWaypointIndex), Toast.LENGTH_SHORT).show();
                    descriptionShown = true;
                }

                if (isCloseToWaypoint(userLat, userLon, waypointLat, waypointLon)) {
                    if (currentWaypointIndex < descriptions.size() - 1) {
                        Toast.makeText(WalkingRouteActivity.this, descriptions.get(currentWaypointIndex + 1), Toast.LENGTH_SHORT).show();
                    }
                    currentWaypointIndex++;
                    descriptionShown = false;
                }

                handler.postDelayed(this, 5000);
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
    }





    // 두 좌표 간의 거리 계산 (단위: 미터)
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // 지구 반지름 (미터 단위)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }


    // 현재 위치와 목표 지점이 가까운지 확인하는 메서드
    private boolean isCloseToWaypoint(double currentLat, double currentLon, double waypointLat, double waypointLon) {
        float[] results = new float[1];
        Location.distanceBetween(currentLat, currentLon, waypointLat, waypointLon, results);
        return results[0] < 10; // 10m 이내에 있는 경우
    }

    // 방위각을 계산하는 메서드
    private double calculateBearing(double lat1, double lon1, double lat2, double lon2) {
        double deltaLon = Math.toRadians(lon2 - lon1);
        double y = Math.sin(deltaLon) * Math.cos(Math.toRadians(lat2));
        double x = Math.cos(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) -
                Math.sin(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(deltaLon);
        double bearing = Math.atan2(y, x);
        return (Math.toDegrees(bearing) + 360) % 360; // 0 ~ 360 사이의 값으로 변환
    }


    // SensorEventListener 구현
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] rotationMatrix = new float[9];
            float[] orientation = new float[3];

            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, orientation);

            // 방위각 계산 (y축 기준 회전각)
            userDir = (float) Math.toDegrees(orientation[0]); // 변경: orientation[1] 사용
            if (userDir < 0) {
                userDir += 360; // 0 ~ 360 사이의 값으로 변환
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 정확도 변화 시 처리 로직 (필요한 경우)
        Log.d(TAG, "Sensor accuracy changed: " + accuracy);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI); // 센서 등록
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister the sensor listener to save battery
        sensorManager.unregisterListener(this);

        // Optionally stop location updates
        fusedLocationClient.removeLocationUpdates(locationCallback);

        // If you want to free up the TMapView resources, you can keep it as it is
        if (tMapView != null) {
            tMapView = null; // This may not be necessary, just ensure you handle it in onDestroy
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


    // WalkingRoute 화면으로 이동하는 메서드
    private void goToSearchActivity() {
        Intent intent = new Intent(WalkingRouteActivity.this, SearchActivity.class);
        startActivity(intent);
        finish();
    }

}