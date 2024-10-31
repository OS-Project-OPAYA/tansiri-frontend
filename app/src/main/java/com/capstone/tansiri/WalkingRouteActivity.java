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

public class WalkingRouteActivity extends AppCompatActivity implements SensorEventListener {

    private TMapView tMapView; // TMapView 객체 생성
    private static final String TAG = "WalkingRoute"; // TAG 변수 정의

    private ApiService apiService;

    private Button btnWalkRouteNavi;
    private Button btnResearch;
    private Button btnFavorite;

    private double userLat;
    private double userLon;
    private float userDir = 0.0f; // 초기 방위각 값

    private Handler handler = new Handler(); // 핸들러 선언
    private LocationManager locationManager;
    private boolean isLogging = false; // 위치 로그 활성화 여부
    private Runnable logRunnable; // 위치 로그용 Runnable
    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // 화면 세로 고정
        setContentView(R.layout.activity_walking_route); // XML 파일 설정
        btnWalkRouteNavi = findViewById(R.id.btnWalkRouteNavi);
        btnResearch = findViewById(R.id.btnResearch);
        btnFavorite = findViewById(R.id.btnFavorite);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        // 센서 매니저 초기화
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        // 현재 위치와 방위각을 GPS로 가져오기 위한 LocationManager 설정
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
        } catch (SecurityException e) {
            Log.e(TAG, "GPS 권한 필요", e);
        }

        // Intent로부터 위도와 경도를 받아오기
        String currentAddress = getIntent().getStringExtra("currentAddress");
        String destinationAddress = getIntent().getStringExtra("destinationAddress");
        String currentLatitude = getIntent().getStringExtra("startY");
        String currentLongitude = getIntent().getStringExtra("startX");
        String destinationLatitude = getIntent().getStringExtra("endY");
        String destinationLongitude = getIntent().getStringExtra("endX");
        String walkrouteResponse = getIntent().getStringExtra("response");
        String userID = getIntent().getStringExtra("userID");

        btnResearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToSearchActivity();
            }
        });
        btnFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                                        } else {
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
        });




        // btnWalkRouteNavi 버튼 클릭 시 현재 위치와 방위각을 로그로 표시/중단
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

        // TMapView 초기화
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey("EHDhTt6iqk7HwqS2AirSY71g65xVG8Rp3LtZaIIx");

        // TMapView 설정 및 경로 그리기
        tMapView.setOnMapReadyListener(new TMapView.OnMapReadyListener() {
            @Override
            public void onMapReady() {
                double startlat = Double.parseDouble(currentLatitude);
                double startlon = Double.parseDouble(currentLongitude);
                double endlat = Double.parseDouble(destinationLatitude);
                double endlon = Double.parseDouble(destinationLongitude);

                tMapView.setCenterPoint(startlon, startlat); // 초기 위치로 중심 설정
                tMapView.setZoomLevel(14); // 기본 줌 레벨 설정
                tMapView.setCompassMode(true);


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
        });

        // TMapView를 추가할 LinearLayout 참조
        LinearLayout linearLayoutTmap = findViewById(R.id.linearLayoutTmap);
        linearLayoutTmap.addView(tMapView);
    }

    private void startNavigate(String walkrouteResponse) {
        // Parse the response to get the waypoints and their descriptions
        ArrayList<TMapPoint> waypoints = new ArrayList<>();
        ArrayList<String> descriptions = new ArrayList<>(); // 설명을 저장할 리스트
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

                    // waypoint에 대한 설명 추가
                    String description = feature.getJSONObject("properties").getString("description"); // JSON에서 설명 가져오기
                    descriptions.add(description); // 설명을 리스트에 추가

                    // 모든 waypoint의 위도와 경도를 로그로 출력
                    Log.d(TAG, "Waypoint " + (i + 1) + " - Lat: " + lat + ", Lon: " + lon + ", Description: " + description);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing response JSON", e);
            return;
        }

        logRunnable = new Runnable() {
            private int currentWaypointIndex = 0;
            private boolean descriptionShown = false; // 설명이 이미 표시되었는지 확인

            @Override
            public void run() {
                // If there are no more waypoints, stop navigation
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
                    directionToWaypoint += 360; // 0 ~ 360 사이의 값으로 변환
                }

                // 안내 메시지 출력
                String directionMessage;
                if (directionToWaypoint < 30 || directionToWaypoint > 330) {
                    directionMessage = "Straight.";
                } else if (directionToWaypoint > 30 && directionToWaypoint < 150) {
                    directionMessage = "Turn Right.";
                } else {
                    directionMessage = "Turn Left.";
                }

                Log.d(TAG, directionMessage + " Now - Lat: " + userLat + ", Lon: " + userLon + ", Dir: " + userDir + "/" + angleToWaypoint);



                // 첫 번째 waypoint에 대한 설명 표시 (처음 시작할 때만)
                if (currentWaypointIndex == 0 && !descriptionShown) {
                    Toast.makeText(WalkingRouteActivity.this, descriptions.get(currentWaypointIndex), Toast.LENGTH_SHORT).show();
                    descriptionShown = true; // 첫 번째 설명 표시 후 상태 업데이트
                }

                // 다음 waypoint로 이동
                if (isCloseToWaypoint(userLat, userLon, waypointLat, waypointLon)) {
                    // 다음 waypoint에 대한 설명 표시
                    if (currentWaypointIndex < descriptions.size() - 1) {
                        Toast.makeText(WalkingRouteActivity.this, descriptions.get(currentWaypointIndex + 1), Toast.LENGTH_SHORT).show();
                    }
                    currentWaypointIndex++; // 다음 waypoint로 이동
                    descriptionShown = false; // 다음 waypoint에 대한 설명 표시를 위한 초기화
                }

                handler.postDelayed(this, 2000); // 2초마다 반복
            }
        };
        handler.post(logRunnable); // Runnable 시작
        Toast.makeText(this, "위치 로그 시작", Toast.LENGTH_SHORT).show();
    }




    // 현재 위치를 업데이트하여 지도에 반영하는 LocationListener 추가
    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            //userLat = location.getLatitude();
            //userLon = location.getLongitude();

            userLat = 127.46255916;
            userLon = 36.62371065;

            // 방위각을 y축 기준 회전각으로 변경
            Log.d(TAG, "현재 위치 - 위도: " + userLat + ", 경도: " + userLon);

            // 지도 중심을 현재 위치로 업데이트
            if (tMapView != null) {
                tMapView.setCenterPoint(userLon, userLat);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
        }


    };


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

    // 위치 로그 중단 메서드
    private void stopNavigate() {
        if (logRunnable != null) {
            handler.removeCallbacks(logRunnable); // Runnable 중단
        }
        Toast.makeText(this, "위치 로그 중단", Toast.LENGTH_SHORT).show();
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
        sensorManager.unregisterListener(this); // 센서 해제
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener); // 위치 업데이트 해제
        }
    }

    // WalkingRoute 화면으로 이동하는 메서드
    private void goToSearchActivity() {
        Intent intent = new Intent(WalkingRouteActivity.this, SearchActivity.class);
        startActivity(intent);
        finish();
    }
}
