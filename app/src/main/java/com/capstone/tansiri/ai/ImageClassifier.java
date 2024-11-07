package com.capstone.tansiri.ai;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class ImageClassifier {
    private final Activity activity;
    private Interpreter interpreter;
    private TensorBuffer probabilityBuffer;
    private ImageProcessor imageProcessor;
    private List<String> classNames;

    public ImageClassifier(Activity activity) {
        this.activity = activity;
    }

    public void initializeModelAsync() {
        // Load model asynchronously
        new Thread(() -> {
            try {
                MappedByteBuffer tfliteModel = loadModelFile("third.tflite");
                interpreter = new Interpreter(tfliteModel);

                probabilityBuffer = TensorBuffer.createFixedSize(new int[]{1, 25200, 15}, DataType.FLOAT32);

                imageProcessor = new ImageProcessor.Builder()
                        .add(new ResizeOp(640, 640, ResizeOp.ResizeMethod.BILINEAR))
                        .build();

                classNames = loadLabels();
            } catch (IOException e) {
                Log.e("ModelError", "Error initializing model", e);
            }
        }).start();
    }

    public void classifyImage(Bitmap bitmap) {
        if (interpreter != null) {
            try {
                // Preprocess image
                TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
                tensorImage.load(bitmap);
                TensorImage processedImage = imageProcessor.process(tensorImage);

                interpreter.run(processedImage.getBuffer(), probabilityBuffer.getBuffer());

                // Process results
                float[] results = probabilityBuffer.getFloatArray();
                List<String> detectedObjects = new ArrayList<>();

                for (int i = 0; i < 25200; i++) {
                    float confidence = results[i * 15 + 4];
                    if (confidence > 0.5f) {
                        float maxClassProb = 0f;
                        int maxClassIndex = -1;
                        for (int j = 5; j < 15; j++) {
                            if (results[i * 15 + j] > maxClassProb) {
                                maxClassProb = results[i * 15 + j];
                                maxClassIndex = j - 5;
                            }
                        }

                        if (maxClassIndex >= 0 && maxClassIndex < classNames.size()) {
                            String detectedClass = classNames.get(maxClassIndex) + ": " + (confidence * maxClassProb * 100) + "%";
                            detectedObjects.add(detectedClass);
                        }
                    }
                }

                // Handle results (e.g., update UI)
                Log.d("DetectionResults", detectedObjects.toString());

            } catch (Exception e) {
                Log.e("InferenceError", "Error during inference", e);
            }
        }
    }

    private List<String> loadLabels() {
        List<String> labels = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(activity.getAssets().open("label.txt")));
            String line;
            while ((line = reader.readLine()) != null) {
                labels.add(line);
            }
        } catch (IOException e) {
            Log.e("LabelLoadError", "Error reading label file", e);
        }
        return labels;
    }

    private MappedByteBuffer loadModelFile(String modelFileName) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelFileName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}