package com.capstone.tansiri.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Trace;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.capstone.tansiri.R;
import com.capstone.tansiri.ai.env.ImageUtils;
import com.capstone.tansiri.ai.env.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Locale;

public abstract class CameraActivity extends AppCompatActivity
        implements OnImageAvailableListener,
        Camera.PreviewCallback,
//        CompoundButton.OnCheckedChangeListener,
        View.OnClickListener {
    private static final Logger LOGGER = new Logger();

    private static final int PERMISSIONS_REQUEST = 1;

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String ASSET_PATH = "";
    protected int previewWidth = 0;
    protected int previewHeight = 0;
    private boolean debug = false;
    protected Handler handler;
    private HandlerThread handlerThread;
    private boolean useCamera2API;
    private boolean isProcessingFrame = false;
    private byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    private int yRowStride;
    protected int defaultModelIndex = 0;
    protected int defaultDeviceIndex = 0;
    private Runnable postInferenceCallback;
    private Runnable imageConverter;
    protected ArrayList<String> modelStrings = new ArrayList<String>();

//  private LinearLayout bottomSheetLayout;
//  private LinearLayout gestureLayout;
//  private BottomSheetBehavior<LinearLayout> sheetBehavior;

    protected TextView inferenceTimeTextView;
    //  protected ImageView bottomSheetArrowImageView;
    private ImageView plusImageView, minusImageView;
    protected ListView deviceView;
    protected TextView threadsTextView;
    private TextView detectclassTextView;
    private TextView detectedclassInfoTextView;
    protected ListView modelView;
    /**
     * Current indices of device and model.
     */
    int currentDevice = -1;
    int currentModel = -1;
    int currentNumThreads = -1;

    private Button btnFinishMap;
    ArrayList<String> deviceStrings = new ArrayList<String>();


    private Vibrator vibrator;
    private TextToSpeech tts;
    private int finishClickCount = 0;
    private static final long DOUBLE_TAP_TIMEOUT = 500; // 두 번 클릭 간 최대 시간
    private long lastTouchTime = 0;



    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        LOGGER.d("onCreate " + this);
        super.onCreate(null);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        detectclassTextView = findViewById(R.id.detectclass);
        detectedclassInfoTextView = findViewById(R.id.detectedclass_info);//여기서 감지된 객체 클래스명을 보냄


        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.KOREAN); // 한국어로 설정
                tts.speak("전방 촬영 화면입니다.", TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
            }
        });



        setContentView(R.layout.tfe_od_activity_camera);

        // 버튼 참조 가져오기
        btnFinishMap = findViewById(R.id.btn_camera_finish);


        // btnFinishMap에 모션과 두 번 클릭 기능 추가
        btnFinishMap.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 버튼을 살짝 축소
                        v.setScaleX(0.95f);
                        v.setScaleY(0.95f);

                        // 진동 추가
                        if (vibrator != null && vibrator.hasVibrator()) {
                            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)); // 0.1초 진동
                        }

                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastTouchTime < DOUBLE_TAP_TIMEOUT) {
                            // 두 번 클릭 시 TTS 실행 취소하고 종료 기능 실행
                            if (tts != null) {
                                tts.speak("촬영 종료", TextToSpeech.QUEUE_FLUSH, null, null);
                            }

                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    finish();
                                }
                            }, 1000);

                            // 클릭 카운트 초기화
                            finishClickCount = 0;
                        } else {
                            // 첫 번째 클릭 시 TTS 안내 메시지 출력
                            lastTouchTime = currentTime;
                            finishClickCount = 1;
                            if (tts != null) {
                                tts.speak("촬영을 종료하시려면 두 번 연속 클릭해 주세요", TextToSpeech.QUEUE_FLUSH, null, null);
                            }

                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        // 버튼을 원래 크기로 복원
                        v.setScaleX(1f);
                        v.setScaleY(1f);
                        return true;
                }
                return false;
            }
        });


        if (hasPermission()) {
            setFragment();
        } else {
            requestPermission();
        }

        threadsTextView = findViewById(R.id.threads);
        currentNumThreads = Integer.parseInt(threadsTextView.getText().toString().trim());
        plusImageView = findViewById(R.id.plus);
        minusImageView = findViewById(R.id.minus);
        deviceView = findViewById(R.id.device_list);
//    deviceStrings.add("CPU");
        deviceStrings.add("객체 감지 시작");
//    deviceStrings.add("NNAPI");
        deviceView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        ArrayAdapter<String> deviceAdapter =
                new ArrayAdapter<>(
                        CameraActivity.this, R.layout.deviceview_row, R.id.deviceview_row_text, deviceStrings);
        deviceView.setAdapter(deviceAdapter);
        deviceView.setItemChecked(defaultDeviceIndex, true);
        currentDevice = defaultDeviceIndex;
        deviceView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        updateActiveModel();
                    }
                });


        modelView = findViewById((R.id.model_list));

        modelStrings = getModelStrings(getAssets(), ASSET_PATH);
        modelView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        ArrayAdapter<String> modelAdapter =
                new ArrayAdapter<>(
                        CameraActivity.this, R.layout.listview_row, R.id.listview_row_text, modelStrings);
        modelView.setAdapter(modelAdapter);
        modelView.setItemChecked(defaultModelIndex, true);
        currentModel = defaultModelIndex;
        modelView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        updateActiveModel();
                    }
                });

        inferenceTimeTextView = findViewById(R.id.inference_info);

        plusImageView.setOnClickListener(this);
        minusImageView.setOnClickListener(this);
    }


    protected ArrayList<String> getModelStrings(AssetManager mgr, String path) {
        ArrayList<String> res = new ArrayList<String>();
        try {
            String[] files = mgr.list(path);
            for (String file : files) {
                String[] splits = file.split("\\.");
                if (splits[splits.length - 1].equals("tflite")) {
                    res.add(file);
                }
            }

        } catch (IOException e) {
            System.err.println("getModelStrings: " + e.getMessage());
        }
        return res;
    }

    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }

    protected int getLuminanceStride() {
        return yRowStride;
    }

    protected byte[] getLuminance() {
        return yuvBytes[0];
    }

    /**
     * Callback for android.hardware.Camera API
     */
    @Override
    public void onPreviewFrame(final byte[] bytes, final Camera camera) {
        if (isProcessingFrame) {
            LOGGER.w("Dropping frame!");
            return;
        }

        try {
            // Initialize the storage bitmaps once when the resolution is known.
            if (rgbBytes == null) {
                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                previewHeight = previewSize.height;
                previewWidth = previewSize.width;
                rgbBytes = new int[previewWidth * previewHeight];
                onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
            }
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            return;
        }

        isProcessingFrame = true;
        yuvBytes[0] = bytes;
        yRowStride = previewWidth;

        imageConverter =
                new Runnable() {
                    @Override
                    public void run() {
                        ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
                    }
                };

        postInferenceCallback =
                new Runnable() {
                    @Override
                    public void run() {
                        camera.addCallbackBuffer(bytes);
                        isProcessingFrame = false;
                    }
                };
        String detectedObject = processImage();
        if (detectedObject != null && !detectedObject.isEmpty()) {
            detectclassTextView.setText("감지된 객체:");
            detectedclassInfoTextView.setText(detectedObject);
        } else {
            detectclassTextView.setText("감지된 객체 없음");
            detectedclassInfoTextView.setText("");
        }


        if (rgbBytes != null) {
            int detectedObjectColor = 0xFF00FF00; // Green bounding box color
            for (int i = 0; i < rgbBytes.length; i++) {
                if (rgbBytes[i] == detectedObjectColor) {
                    // 진동 코드 또는 기타 알림 기능을 추가할 수 있습니다.
                    break;
                }
            }
        }
    }

    /**
     * Callback for Camera2 API
     */
    @Override
    public void onImageAvailable(final ImageReader reader) {
        // We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) {
            return;
        }
        if (rgbBytes == null) {
            rgbBytes = new int[previewWidth * previewHeight];
        }
        try {
            final Image image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            if (isProcessingFrame) {
                image.close();
                return;
            }
            isProcessingFrame = true;
            Trace.beginSection("imageAvailable");
            final Plane[] planes = image.getPlanes();
            fillBytes(planes, yuvBytes);
            yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            imageConverter =
                    new Runnable() {
                        @Override
                        public void run() {
                            ImageUtils.convertYUV420ToARGB8888(
                                    yuvBytes[0],
                                    yuvBytes[1],
                                    yuvBytes[2],
                                    previewWidth,
                                    previewHeight,
                                    yRowStride,
                                    uvRowStride,
                                    uvPixelStride,
                                    rgbBytes);
                        }
                    };

            postInferenceCallback =
                    new Runnable() {
                        @Override
                        public void run() {
                            image.close();
                            isProcessingFrame = false;
                        }
                    };

            processImage();

            if (rgbBytes != null) {
                int detectedObjectColor = 0xFF00FF00; // Green bounding box color
                for (int i = 0; i < rgbBytes.length; i++) {
                    if (rgbBytes[i] == detectedObjectColor) {
                        // 진동 코드 추가
                        if (vibrator != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                // Android 8.0 이상에서는 VibrationEffect 사용
                                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                            } else {
                                // Android 8.0 미만에서는 기본 vibrate 사용
                                vibrator.vibrate(500);  // 500ms 동안 진동
                            }
                        }
                        break;  // 특정 색상이 감지되면 루프 종료
                    }
                }
            }
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            Trace.endSection();
            return;
        }
        Trace.endSection();
    }

    @Override
    public synchronized void onStart() {
        LOGGER.d("onStart " + this);
        super.onStart();
    }

    @Override
    public synchronized void onResume() {
        LOGGER.d("onResume " + this);
        super.onResume();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public synchronized void onPause() {
        LOGGER.d("onPause " + this);

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }

        super.onPause();
    }

    @Override
    public synchronized void onStop() {
        LOGGER.d("onStop " + this);
        super.onStop();
    }

    @Override
    public synchronized void onDestroy() {
        LOGGER.d("onDestroy " + this);
        super.onDestroy();
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST) {
            if (allPermissionsGranted(grantResults)) {
                setFragment();
            } else {
                requestPermission();
            }
        }
    }

    private static boolean allPermissionsGranted(final int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
                Toast.makeText(
                                CameraActivity.this,
                                "Camera permission is required for this demo",
                                Toast.LENGTH_LONG)
                        .show();
            }
            requestPermissions(new String[]{PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
        }
    }

    // Returns true if the device supports the required hardware level, or better.
    private boolean isHardwareLevelSupported(
            CameraCharacteristics characteristics, int requiredLevel) {
        int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            return requiredLevel == deviceLevel;
        }
        // deviceLevel is not LEGACY, can use numerical sort
        return requiredLevel <= deviceLevel;
    }

    private String chooseCamera() {
        final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (final String cameraId : manager.getCameraIdList()) {
                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                final StreamConfigurationMap map =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                if (map == null) {
                    continue;
                }


                useCamera2API =
                        (facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
                                || isHardwareLevelSupported(
                                characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
                LOGGER.i("Camera API lv2?: %s", useCamera2API);
                return cameraId;
            }
        } catch (CameraAccessException e) {
            LOGGER.e(e, "Not allowed to access camera");
        }

        return null;
    }

    protected void setFragment() {
        String cameraId = chooseCamera();

        Fragment fragment;
        if (useCamera2API) {
            CameraConnectionFragment camera2Fragment =
                    CameraConnectionFragment.newInstance(
                            new CameraConnectionFragment.ConnectionCallback() {
                                @Override
                                public void onPreviewSizeChosen(final Size size, final int rotation) {
                                    previewHeight = size.getHeight();
                                    previewWidth = size.getWidth();
                                    CameraActivity.this.onPreviewSizeChosen(size, rotation);
                                }
                            },
                            this,
                            getLayoutId(),
                            getDesiredPreviewFrameSize());

            camera2Fragment.setCamera(cameraId);
            fragment = camera2Fragment;
        } else {
            fragment =
                    new LegacyCameraConnectionFragment(this, getLayoutId(), getDesiredPreviewFrameSize());
        }

        getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

    protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    public boolean isDebug() {
        return debug;
    }

    protected void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }

    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

//  @Override
//  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//    setUseNNAPI(isChecked);
//    if (isChecked) apiSwitchCompat.setText("NNAPI");
//    else apiSwitchCompat.setText("TFLITE");
//  }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.plus) {
            String threads = threadsTextView.getText().toString().trim();
            int numThreads = Integer.parseInt(threads);
            if (numThreads >= 9) return;
            numThreads++;
            threadsTextView.setText(String.valueOf(numThreads));
            setNumThreads(numThreads);
        } else if (v.getId() == R.id.minus) {
            String threads = threadsTextView.getText().toString().trim();
            int numThreads = Integer.parseInt(threads);
            if (numThreads == 1) {
                return;
            }
            numThreads--;
            threadsTextView.setText(String.valueOf(numThreads));
            setNumThreads(numThreads);
        }
    }

//  protected void showFrameInfo(String frameInfo) {
//    frameValueTextView.setText(frameInfo);
//  }

//  protected void showCropInfo(String cropInfo) {
//    cropValueTextView.setText(cropInfo);
//  }

    protected void showInference(String inferenceTime) {
        inferenceTimeTextView.setText(inferenceTime);
    }

    protected abstract void updateActiveModel();

    protected abstract String processImage();

    protected abstract void onPreviewSizeChosen(final Size size, final int rotation);

    protected abstract int getLayoutId();

    protected abstract Size getDesiredPreviewFrameSize();

    protected abstract void setNumThreads(int numThreads);

    protected abstract void setUseNNAPI(boolean isChecked);
}
