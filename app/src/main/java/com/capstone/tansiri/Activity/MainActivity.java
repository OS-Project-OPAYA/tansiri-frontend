package com.capstone.tansiri.Activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.capstone.tansiri.R;
import com.capstone.tansiri.map.Util;
import com.capstone.tansiri.map.RetrofitClient;
import com.capstone.tansiri.map.ApiService;
import com.capstone.tansiri.map.entity.Favorite;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextToSpeech tts;
    private final int DOUBLE_TAP_TIMEOUT = 300;
    private final Handler handler = new Handler();
    private HashMap<ImageButton, String> buttonNames;
    private Toast lastToast = null;
    private String userID;

    private ApiService apiService;
    private Runnable ttsRunnable;

    public static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.3f;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userID = Util.getDeviceID(getApplicationContext());

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.KOREA);
            }
        });

        apiService = RetrofitClient.getClient().create(ApiService.class);
        buttonNames = new HashMap<>();

        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        ImageButton btnCamera = findViewById(R.id.btnCamera);
        buttonNames.put(btnCamera, "전방 촬영");

        ImageButton btnSearch = findViewById(R.id.btnSearch);
        buttonNames.put(btnSearch, "목적지 검색");

        ImageButton btnFavorite = findViewById(R.id.btnFavorite);
        buttonNames.put(btnFavorite, "즐겨찾기");

        for (final ImageButton button : buttonNames.keySet()) {
            final long[] lastTouchTime = {0};
            final Toast[] lastToast = {null};

            button.setOnTouchListener((v, event) -> {
                Vibrator vibrator = (Vibrator) v.getContext().getSystemService(Context.VIBRATOR_SERVICE);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setScaleX(0.95f);
                        v.setScaleY(0.95f);

                        // 진동 추가
                        if (vibrator != null && vibrator.hasVibrator()) {
                            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)); // 0.1초 진동
                        }

                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastTouchTime[0] < DOUBLE_TAP_TIMEOUT) {
                            // 두 번 클릭 시 TTS 실행 취소하고 기능 실행
                            handler.removeCallbacks(ttsRunnable);
                            if (lastToast[0] != null) {
                                lastToast[0].cancel();
                            }
                            buttonFunction(v);
                        } else {
                            // 첫 번째 클릭 시 TTS 안내 메시지 출력
                            lastTouchTime[0] = currentTime;
                            String buttonName = buttonNames.get(v);
                            ttsRunnable = () -> speakButtonMessage(buttonName);
                            handler.postDelayed(ttsRunnable, DOUBLE_TAP_TIMEOUT);

                            if (lastToast[0] != null) {
                                lastToast[0].cancel();
                            }

                            lastToast[0] = Toast.makeText(v.getContext(), "두 번 연속으로 클릭하세요!", Toast.LENGTH_LONG);
                            lastToast[0].show();
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        v.setScaleX(1f);
                        v.setScaleY(1f);
                        return true;

                    default:
                        return false;
                }
            });
        }
    }

    private void speakButtonMessage(String buttonName) {
        String message;
        switch (buttonName) {
            case "전방 촬영":
                message = "전방 촬영을 하려면 두번 클릭하세요";
                break;
            case "목적지 검색":
                message = "목적지 검색을 하려면 두번 클릭하세요";
                break;
            case "즐겨찾기":
                message = "즐겨찾기 목록을 보려면 두번 클릭하세요";
                break;
            default:
                message = "";
                break;
        }
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void buttonFunction(View button) {
        int buttonId = button.getId();

        if (buttonId == R.id.btnCamera) {
            startActivity(new Intent(MainActivity.this, DetectorActivity.class));
        } else if (buttonId == R.id.btnSearch) {
            checkSearchAndProceed(userID);
        } else if (buttonId == R.id.btnFavorite) {
            checkFavoritesAndProceed(userID);
        }
    }

    private void checkSearchAndProceed(String userID) {
        if (tts != null) {
            tts.speak("로딩 중", TextToSpeech.QUEUE_FLUSH, null, null);
        }

        Call<List<Favorite>> call = apiService.getFavoritesByUserId(userID);
        call.enqueue(new Callback<List<Favorite>>() {
            @Override
            public void onResponse(Call<List<Favorite>> call, Response<List<Favorite>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    startActivity(new Intent(MainActivity.this, SearchActivity.class));
                } else {
                    Toast.makeText(MainActivity.this, "서버 통신 에러", Toast.LENGTH_SHORT).show();
                    if (tts != null) {
                        tts.speak("다시 클릭해주세요", TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Favorite>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "서버 통신 에러", Toast.LENGTH_SHORT).show();
                if (tts != null) {
                    tts.speak("다시 클릭해주세요", TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }
        });
    }

    private void checkFavoritesAndProceed(String userID) {
        if (tts != null) {
            tts.speak("로딩 중", TextToSpeech.QUEUE_FLUSH, null, null);
        }
        Call<List<Favorite>> call = apiService.getFavoritesByUserId(userID);
        call.enqueue(new Callback<List<Favorite>>() {
            @Override
            public void onResponse(Call<List<Favorite>> call, Response<List<Favorite>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    startActivity(new Intent(MainActivity.this, FavoriteActivity.class));
                } else {
                    Toast.makeText(MainActivity.this, "서버 통신 에러", Toast.LENGTH_SHORT).show();
                    if (tts != null) {
                        tts.speak("다시 클릭해주세요", TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Favorite>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "서버 통신 에러", Toast.LENGTH_SHORT).show();
                if (tts != null) {
                    tts.speak("다시 클릭해주세요", TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        handler.removeCallbacks(ttsRunnable);
        super.onDestroy();
    }
}
