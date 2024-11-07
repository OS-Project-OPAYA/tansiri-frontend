package com.capstone.tansiri.Activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.capstone.tansiri.ai.ImageClassifier;
import com.capstone.tansiri.R;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {
    private ExecutorService cameraExecutor;
    private ImageClassifier imageClassifier;
    private static final int CAMERA_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.capstone.tansiri.R.layout.activity_camera);

        // 레이아웃이 설정된 후 PreviewView 초기화
        PreviewView previewView = findViewById(com.capstone.tansiri.R.id.previewView);
        if (previewView == null) {
            Log.e("CameraError", "PreviewView is null");
            Toast.makeText(this, "PreviewView initialization failed.", Toast.LENGTH_SHORT).show();
            return;
        }

        cameraExecutor = Executors.newSingleThreadExecutor();
        imageClassifier = new ImageClassifier(this);

        // Initialize TensorFlow Lite model asynchronously
        imageClassifier.initializeModelAsync();

        requestCameraPermission();
    }

    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // Call to superclass method

        if (requestCode == CAMERA_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCamera() {
        try {
            // Handling ExecutionException and InterruptedException
            ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(this).get();
            PreviewView previewView = findViewById(R.id.previewView);

            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(previewView.getSurfaceProvider());

            CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

            ImageAnalysis imageAnalyzer = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();
            imageAnalyzer.setAnalyzer(cameraExecutor, imageProxy -> {
                try {
                    @OptIn(markerClass = ExperimentalGetImage.class)  // Opt-in for using ExperimentalGetImage
                    Image image = imageProxy.getImage();
                    if (image != null) {
                        Bitmap bitmap = imageProxyToBitmap(imageProxy);
                        if (bitmap != null) {
                            // Image classification
                            imageClassifier.classifyImage(bitmap);
                            bitmap.recycle();
                        } else {
                            Log.e("BitmapError", "Bitmap is null");
                        }
                        image.close(); // Don't forget to close the image
                    }
                } finally {
                    imageProxy.close();
                }
            });

            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer);

        } catch (ExecutionException | InterruptedException e) {
            Log.e("CameraError", "Error getting camera provider", e);
            // Handle the error, e.g., show a message to the user or retry
        }
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        // Convert YUV to RGB bitmap here (depends on the format of imageProxy)
        // This should handle YUV_420_888 format that CameraX provides
        // You can use ImageUtil class or custom conversion logic
        Bitmap bitmap = null;
        try {
            int width = imageProxy.getWidth();
            int height = imageProxy.getHeight();
            Image image = imageProxy.getImage();
            if (image != null) {
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            }
        } catch (Exception e) {
            Log.e("ImageConversionError", "Error converting ImageProxy to Bitmap", e);
        }
        return bitmap;
    }
}
