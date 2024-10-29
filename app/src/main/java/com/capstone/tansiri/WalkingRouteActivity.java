package com.capstone.tansiri;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.capstone.tansiri.map.RetrofitClient;
import com.skt.tmap.TMapPoint;
import com.skt.tmap.TMapView;
import com.skt.tmap.overlay.TMapPolyLine;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


public class WalkingRouteActivity extends AppCompatActivity {

    private TMapView tMapView; // TMapView 객체 생성
    private static final String TAG = "WalkingRoute"; // TAG 변수 정의

    //UserState에 전송하기 위한 자료
    private Button btnWalkRouteNavi;
    private double userLat; // 현재 위치의 위도
    private double userLon; // 현재 위치의 경도
    private double userDir; // 현재 방위각



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walking_route); // XML 파일 설정
        btnWalkRouteNavi = findViewById(R.id.btnWalkRouteNavi);


        // 현재 위치와 방위각을 가져오는 코드가 있다고 가정
        userLat = 36.6257; // 예시 위도 값, 실제 위치값으로 대체하세요
        userLon = 127.4543; // 예시 경도 값, 실제 위치값으로 대체하세요
        userDir = 45.0; // 예시 방위각 값, 실제 값으로 대체하세요

        btnWalkRouteNavi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUserStateToServer(userLat, userLon, userDir);
            }
        });




        // TMapView 초기화
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey("EHDhTt6iqk7HwqS2AirSY71g65xVG8Rp3LtZaIIx"); // 발급받은 API 키 설정

        // Intent로부터 위도와 경도를 String으로 받아오기
        String currentLatitude = getIntent().getStringExtra("startY"); // 현재 위도
        String currentLongitude = getIntent().getStringExtra("startX"); // 현재 경도
        String destinationLatitude = getIntent().getStringExtra("endY"); // 현재 위도
        String destinationLongitude = getIntent().getStringExtra("endX"); // 현재 경도
        String walkrouteResponse = getIntent().getStringExtra("response"); // response


        // TMapView 리스너 설정
        tMapView.setOnMapReadyListener(new TMapView.OnMapReadyListener() {
            @Override
            public void onMapReady() {

                double startlat = Double.parseDouble(currentLatitude);
                double startlon = Double.parseDouble(currentLongitude);
                double endlat = Double.parseDouble(destinationLatitude);
                double endlon = Double.parseDouble(destinationLongitude);
                String response = walkrouteResponse;


                tMapView.setCenterPoint(startlat, startlon); // 현재 위치로 중심 설정
                tMapView.setZoomLevel(13); // 기본 줌 레벨 설정
                tMapView.setCompassMode(true);

                try {
                    // response JSON을 파싱하여 경로 선을 그림
                    ArrayList<TMapPoint> pointList = new ArrayList<>();

                    // JSON 데이터에서 features 배열 파싱
                    JSONObject jsonResponse = new JSONObject(walkrouteResponse);
                    JSONArray featuresArray = jsonResponse.getJSONArray("features");

                    // 각 feature의 geometry -> coordinates 추출
                    for (int i = 0; i < featuresArray.length(); i++) {
                        JSONObject feature = featuresArray.getJSONObject(i);
                        JSONObject geometry = feature.getJSONObject("geometry");

                        // geometry가 "LineString" 타입일 때 좌표를 연결
                        if (geometry.getString("type").equals("LineString")) {
                            JSONArray coordinates = geometry.getJSONArray("coordinates");

                            for (int j = 0; j < coordinates.length(); j++) {
                                JSONArray coord = coordinates.getJSONArray(j);
                                double lon = coord.getDouble(0);
                                double lat = coord.getDouble(1);

                                // 각 좌표를 TMapPoint로 추가
                                pointList.add(new TMapPoint(lat, lon));
                            }
                        }
                    }

                    // TMapPolyLine에 pointList 추가
                    TMapPolyLine line = new TMapPolyLine("line1",pointList);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tMapView != null) {
            tMapView = null; // TMapView 메모리 해제
        }
    }


    // 서버로 위치와 방위각을 전송하는 메서드
    private void sendUserStateToServer(double lat, double lon, double dir) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 서버 URL 설정 (Spring Boot 서버의 엔드포인트)
                    URL url = new URL(RetrofitClient.BASE_URL + "/userstate/save");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setDoOutput(true);

                    // JSON 객체 생성
                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("userLat", lat);
                    jsonParam.put("userLon", lon);
                    jsonParam.put("userDir", dir);

                    // 서버로 데이터 전송
                    OutputStream os = conn.getOutputStream();
                    os.write(jsonParam.toString().getBytes("UTF-8"));
                    os.close();

                    // 응답 코드 확인 (200 OK 등의 코드 확인 가능)
                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        Log.d("WalkingRouteActivity", "Data sent successfully.");
                    } else {
                        Log.e("WalkingRouteActivity", "Failed to send data: " + responseCode);
                    }

                    conn.disconnect();
                } catch (Exception e) {
                    Log.e("WalkingRouteActivity", "Error sending data", e);
                }
            }
        }).start();
    }

}
