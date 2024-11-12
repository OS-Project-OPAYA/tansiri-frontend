package com.capstone.tansiri.Activity;

import com.capstone.tansiri.R;
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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;


import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.Priority;
import com.google.android.material.snackbar.Snackbar;
import com.skt.tmap.TMapPoint;
import com.skt.tmap.TMapView;
import com.skt.tmap.overlay.TMapMarkerItem;
import com.skt.tmap.overlay.TMapPolyLine;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

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

    private TextToSpeech tts;

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

    private Integer totalTime;
    private Integer totalDistance;
    private String distanceMessage;

    private Snackbar snackbar;


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


        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.KOREAN); // 한국어로 설정
                tts.speak("경로를 탐색하였습니다. 경로 안내를 받으시려면 하단의 버튼을 눌러주세요", TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                Log.e(TAG, "TTS 초기화 실패");
            }
        });



        // 버튼 클릭 리스너 설정
        setupButtonListeners();
    }

    private void setInitialMapPosition() {
        Double clat = Double.parseDouble(currentLatitude);
        Double clon = Double.parseDouble(currentLongitude);
        tMapView.setCenterPoint(clat, clon); // 초기 위치로 중심 설정
        tMapView.setZoomLevel(17); // 기본 줌 레벨 설정
    }

    private void setUserMarker() {
        tMapView.setCenterPoint(userLat, userLon); // 초기 위치로 중심 설정

        // 기존 마커가 있는 경우 제거
        if (tMapView.getMarkerItemFromId("userMarker") != null) {
            tMapView.removeTMapMarkerItem("userMarker");
        }

        TMapMarkerItem marker = new TMapMarkerItem();
        marker.setId("userMarker");
        marker.setTMapPoint(new TMapPoint(userLat, userLon));

         // 마커 이미지 설정
        Bitmap markerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.current_location_icon);
        marker.setIcon(markerBitmap);

        // 마커를 TMapView에 추가
        tMapView.addTMapMarkerItem(marker);
    }






    private void drawRoute() {
        try {
            ArrayList<TMapPoint> pointList = new ArrayList<>();
            JSONObject jsonResponse = new JSONObject(walkrouteResponse);
            JSONArray featuresArray = jsonResponse.getJSONArray("features");

            // 출발지와 목적지 마커를 먼저 추가
            TMapPoint startPoint = null;
            TMapPoint endPoint = null;

            for (int i = 0; i < featuresArray.length(); i++) {
                JSONObject feature = featuresArray.getJSONObject(i);
                JSONObject geometry = feature.getJSONObject("geometry");
                JSONObject properties = feature.getJSONObject("properties");

                // 출발지 (pointType: "SP")
                if (geometry.getString("type").equals("Point") && properties.getString("pointType").equals("SP")) {
                    JSONArray coordinates = geometry.getJSONArray("coordinates");
                    double lon = coordinates.getDouble(0);
                    double lat = coordinates.getDouble(1);

                    // 출발지 마커 추가
                    TMapMarkerItem start = new TMapMarkerItem();
                    start.setId("start");
                    start.setTMapPoint(new TMapPoint(lat, lon));

                    // 마커 이미지 설정
                    Bitmap markerBitmap1 = BitmapFactory.decodeResource(getResources(), R.drawable.start_icon);
                    start.setIcon(markerBitmap1);

                    // 마커를 TMapView에 추가
                    tMapView.addTMapMarkerItem(start);

                    // 출발지 점을 pointList에 추가
                    startPoint = new TMapPoint(lat, lon);
                }

                // 목적지 (pointType: "EP")
                if (geometry.getString("type").equals("Point") && properties.getString("pointType").equals("EP")) {
                    JSONArray coordinates = geometry.getJSONArray("coordinates");
                    double lon = coordinates.getDouble(0);
                    double lat = coordinates.getDouble(1);

                    // 목적지 마커 추가
                    TMapMarkerItem end = new TMapMarkerItem();
                    end.setId("end");
                    end.setTMapPoint(new TMapPoint(lat, lon));

                    // 마커 이미지 설정
                    Bitmap markerBitmap2 = BitmapFactory.decodeResource(getResources(), R.drawable.destination_icon);
                    end.setIcon(markerBitmap2);

                    // 마커를 TMapView에 추가
                    tMapView.addTMapMarkerItem(end);

                    // 목적지 점을 pointList에 추가
                    endPoint = new TMapPoint(lat, lon);
                }
            }


            // 경유지 마커 추가 (경유지는 경로에 포함하지 않음)
            for (int i = 0; i < featuresArray.length(); i++) {
                JSONObject feature = featuresArray.getJSONObject(i);
                JSONObject geometry = feature.getJSONObject("geometry");
                JSONObject properties = feature.getJSONObject("properties");

                // 경유지 마커 추가 (pointType: "GP")
                if (geometry.getString("type").equals("Point") && properties.getString("pointType").equals("GP")) {
                    JSONArray coordinates = geometry.getJSONArray("coordinates");
                    double lon = coordinates.getDouble(0);
                    double lat = coordinates.getDouble(1);

                    // 각 경유지 마커 추가
                    TMapMarkerItem waypoint = new TMapMarkerItem();
                    waypoint.setId("waypoint_" + properties.getInt("pointIndex"));
                    waypoint.setTMapPoint(new TMapPoint(lat, lon));

                    // 마커 이미지 설정
                    Bitmap markerBitmap3 = BitmapFactory.decodeResource(getResources(), R.drawable.index_icon); // 다른 마커 이미지 사용 가능
                    waypoint.setIcon(markerBitmap3);

                    // 마커를 TMapView에 추가x`
                    tMapView.addTMapMarkerItem(waypoint);
                }
            }

            // LineString만 경로로 추가 (Point 연결은 제외)
            for (int i = 0; i < featuresArray.length(); i++) {
                JSONObject feature = featuresArray.getJSONObject(i);
                JSONObject geometry = feature.getJSONObject("geometry");

                if (geometry.getString("type").equals("LineString")) {
                    JSONArray coordinates = geometry.getJSONArray("coordinates");

                    // LineString의 점들을 pointList에 추가
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

            // 경로 라인 그리기
            if (pointList.size() > 1) {
                TMapPolyLine line = new TMapPolyLine("line1", pointList);
                tMapView.addTMapPolyLine(line);
            }

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
    private int researchClickCount = 0;
    private int favoriteClickCount = 0;
    private static final long DOUBLE_CLICK_INTERVAL = 500; // 더블 클릭 감지 시간 (밀리초)


    private void setupButtonListeners() {
        // 진동 초기화
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // 목적지 재검색 버튼 리스너
        btnResearch.setOnClickListener(view -> {
            applyButtonEffect(view, vibrator); // 버튼 효과 및 진동 추가

            researchClickCount++;
            new Handler().postDelayed(() -> {
                if (researchClickCount == 1) {
                    tts.speak("목적지를 재검색하려면 두번 연속 클릭하세요", TextToSpeech.QUEUE_FLUSH, null, null);
                } else if (researchClickCount == 2) {
                    goToSearchActivity();
                }
                researchClickCount = 0;
            }, DOUBLE_CLICK_INTERVAL);
        });

        // 즐겨찾기 등록 버튼 리스너
        btnFavorite.setOnClickListener(view -> {
            applyButtonEffect(view, vibrator); // 버튼 효과 및 진동 추가

            favoriteClickCount++;
            new Handler().postDelayed(() -> {
                if (favoriteClickCount == 1) {
                    tts.speak("즐겨찾기에 등록하려면 두번 연속 클릭하세요", TextToSpeech.QUEUE_FLUSH, null, null);
                } else if (favoriteClickCount == 2) {
                    saveToFavorites();
                }
                favoriteClickCount = 0;
            }, DOUBLE_CLICK_INTERVAL);
        });

        // 객체 인식 버튼 리스너
        btnObjectRecognition.setOnClickListener(view -> {
            applyButtonEffect(view, vibrator); // 버튼 효과 및 진동 추가
            startActivity(new Intent(WalkingRouteActivity.this, DetectorActivity.class));
        });

        // 도보 경로 안내 버튼 리스너
        btnWalkRouteNavi.setOnClickListener(view -> {
            applyButtonEffect(view, vibrator); // 버튼 효과 및 진동 추가

            if (!isNavigating) {
                // 네비게이션 시작
                startNavigate(walkrouteResponse);
                btnWalkRouteNavi.setText("안내 종료");
                hideOtherButtons();
                isNavigating = true;
                if (tts != null) {
                    String startMessage = "경로 안내를 시작합니다. " + distanceMessage;
                    tts.speak(startMessage, TextToSpeech.QUEUE_FLUSH, null, null);


                    snackbar = Snackbar.make(findViewById(android.R.id.content), distanceMessage, Snackbar.LENGTH_LONG);
                    View snackbarView = snackbar.getView();

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            snackbar.dismiss();
                        }
                    }, 10000);

                    snackbar.show();

                }
            } else {
                // 네비게이션 종료
                stopNavigate();
                btnWalkRouteNavi.setText("도보 경로 안내");
                showOtherButtons();
                isNavigating = false;

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    goToSearchActivity();
                    finish();
                }, 2000);
            }
        });
    }


    // 버튼 클릭 효과 및 진동 추가 함수
    private void applyButtonEffect(View v, Vibrator vibrator) {
        // 버튼이 눌렸을 때 크기 작아짐
        v.setScaleX(0.95f);
        v.setScaleY(0.95f);

        // 진동 추가
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
        }

        // 버튼이 눌린 후 잠시 후에 원래 크기로 복원
        v.postDelayed(() -> {
            v.setScaleX(1.0f);
            v.setScaleY(1.0f);
        }, 200);
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
                                    tts.speak("즐겨찾기에 등록하였습니다", TextToSpeech.QUEUE_FLUSH, null, null);

                                }
                            }

                            @Override
                            public void onFailure(Call<Favorite> call, Throwable t) {
                                Log.e("API_ERROR", "Save Failure: " + t.getMessage());
                            }
                        });
                    } else {
                        Toast.makeText(getApplicationContext(), "이미 즐겨찾기에 등록되어 있습니다.", Toast.LENGTH_SHORT).show();
                        tts.speak("이미 즐겨찾기에 등록되어 있습니다.", TextToSpeech.QUEUE_FLUSH, null, null);

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

            // 총거리와 소요 시간 추출
            if (featuresArray.length() > 0) {
                JSONObject firstFeature = featuresArray.getJSONObject(0);
                JSONObject properties = firstFeature.getJSONObject("properties");

                totalDistance = properties.getInt("totalDistance");
                totalTime = properties.getInt("totalTime");

                // 총 거리 및 시간 안내
                int minutes = totalTime / 60;
                distanceMessage = String.format("총 거리는 %dm이고 예상 소요시간은 %d분입니다.", totalDistance, minutes);

                // TTS로 안내 (TTS 초기화가 완료된 후 실행되어야 함)
                if (tts != null) {
                    tts.speak(distanceMessage, TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }

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
        }

        setupSensorListener(); // 방위각 감지 시작


        logRunnable = new Runnable() {
            private int currentWaypointIndex = 0;

            @Override
            public void run() {
                if (currentWaypointIndex >= waypoints.size()) {
                    // Snackbar 표시 및 TTS 음성 출력
                    Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "목적지에 도착했습니다.", Snackbar.LENGTH_SHORT);
                    snackbar.show();

                    // TTS로 "목적지에 도착했습니다."를 읽기
                    tts.speak("목적지에 도착했습니다.", TextToSpeech.QUEUE_FLUSH, null, null);                    stopNavigate();
                    return;
                }

                TMapPoint currentWaypoint = waypoints.get(currentWaypointIndex);
                double waypointLat = currentWaypoint.getLatitude();
                double waypointLon = currentWaypoint.getLongitude();

                double distanceToWaypoint = calculateDistance(userLat, userLon, waypointLat, waypointLon);

                setUserMarker(); // 위치 로그가 업데이트될 때마다 마커 설정

                if (isCloseToWaypoint(userLat, userLon, waypointLat, waypointLon)) {
                    // 현재 위치가 거점에 가까우면 토스트와 TTS로 설명을 안내합니다.
                    String waypointDescription = descriptions.get(currentWaypointIndex);
                    Toast.makeText(WalkingRouteActivity.this, waypointDescription, Toast.LENGTH_SHORT).show();
                    tts.speak(waypointDescription, TextToSpeech.QUEUE_FLUSH, null, null);  // 거점 설명을 TTS로 출력
                    currentWaypointIndex++;
                } else {
                    // 거점과의 거리와 방향 메시지를 생성하고 토스트와 TTS로 안내합니다.
                    double targetBearing = calculateBearing(userLat, userLon, waypointLat, waypointLon);
                    String directionMessage = getDirectionMessage(userDir, targetBearing);
                    String distanceMessage = String.format("다음 거점까지의 거리: %.2f m | 방향: %s |", distanceToWaypoint, directionMessage);

                    Toast.makeText(WalkingRouteActivity.this, distanceMessage, Toast.LENGTH_SHORT).show();
                    tts.speak(directionMessage, TextToSpeech.QUEUE_FLUSH, null, null);  // 거리와 방향 정보를 TTS로 출력
                }

                handler.postDelayed(this, 2000);
            }
        };

        // 첫 실행을 5초 지연시킴
        handler.postDelayed(logRunnable, 9000);

    }


    // 위치 로그 중단 메서드
    private void stopNavigate() {
        if (logRunnable != null) {
            handler.removeCallbacks(logRunnable); // Runnable 중단
        }
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "경로 안내를 종료합니다.", Snackbar.LENGTH_INDEFINITE);
        snackbar.show();

        if (tts != null) {
            tts.speak("경로 안내를 종료합니다.", TextToSpeech.QUEUE_FLUSH, null, null);
        }
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

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        fusedLocationClient.removeLocationUpdates(locationCallback);
        if (tMapView != null) {
            tMapView = null;
        }
        if (sensorManager != null && sensorEventListener != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }

        // WalkingRouteActivity 화면에서 나갈 때, 안내가 진행 중이면 종료
        if (isNavigating) {
            stopNavigate();
        }
    }
}