package org.tensorflow.lite.examples.classification;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static android.content.ContentValues.TAG;

public class TextReader extends AppCompatActivity {
    SurfaceView surfaceView;
    TextView textView;
    CameraSource cameraSource;
    final int requestPermissionID = 1001;
    TextToSpeech textToSpeech;
    private String oldTxt = "";
    private Set<String> wordsSet;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_reader);

        Path path = Paths.get("wordlist.txt");
        byte[] readBytes = new byte[0];
        try {
            readBytes = Files.readAllBytes(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String wordListContents = null;
        try {
            wordListContents = new String(readBytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String[] words = wordListContents.split("\n");
        wordsSet = new HashSet<>();
        Collections.addAll(wordsSet, words);


        surfaceView = findViewById(R.id.cameraView);
        textView = findViewById(R.id.scannedText);

        onStartCamera();

    }

    private void onStartCamera() {
        final TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        if (!textRecognizer.isOperational()) {
            Log.w(TAG, "Detector dependencies not loaded yet");
        } else {
            cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setAutoFocusEnabled(false)
                    .setRequestedFps(2.0f)
                    .build();

            surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(@NonNull SurfaceHolder holder) {
                    try {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(TextReader.this, new String[]{Manifest.permission.CAMERA},
                                    requestPermissionID);
                            return;
                        }
                        cameraSource.start(surfaceView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

                }

                @Override
                public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                    cameraSource.stop();
                }
            });

            textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {

                }

                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {
                    final SparseArray<TextBlock> items = detections.getDetectedItems();
                    if (items.size() != 0) {
                        textView.post(new Runnable() {
                            @Override
                            public void run() {
                                TextToSpeech tts = new TextToSpeech(getApplicationContext(), null);
                                StringBuilder stringBuilder = new StringBuilder();
                                for (int i = 0; i < items.size(); i++) {
                                    TextBlock item = items.valueAt(i);
                                    stringBuilder.append(item.getValue());
                                    stringBuilder.append("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
                                }

                                textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener(){
                                    @Override
                                    public void onInit(int status) {
                                        if (status == TextToSpeech.SUCCESS){
                                            Double perce =  similarity(stringBuilder.toString(), oldTxt);
                                                Log.d("myTagNew", stringBuilder.toString());
                                                Log.d("myTagNew", perce.toString());
                                                    if (!oldTxt.equalsIgnoreCase(stringBuilder.toString()) && stringBuilder.toString().length() > 2 && perce < 0.70){

                                                        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener(){
                                                            @Override
                                                            public void onInit(int status) {
                                                                if (status == TextToSpeech.SUCCESS){
                                                                    int lang = textToSpeech.setLanguage(Locale.ENGLISH);

                                                                    int speech = textToSpeech.speak(stringBuilder.toString(), TextToSpeech.QUEUE_FLUSH, null);
                                                                }
                                                            }
                                                        });
                                                    }else {
                                                        tts.stop();
                                                    }
                                                    oldTxt = stringBuilder.toString();
                                        }
                                    }
                                });
                            }
                        });
                    }
                }
            });
        }

    }

    public boolean check_for_word(String word)
    {
        return wordsSet.contains(word);
    }

    public static double similarity(String s1, String s2) {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) { // longer should always have greater length
            longer = s2; shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) { return 1.0; /* both strings are zero length */ }
        return (longerLength - editDistance(longer, shorter)) / (double) longerLength;
    }

    public static int editDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0)
                    costs[j] = j;
                else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1))
                            newValue = Math.min(Math.min(newValue, lastValue),
                                    costs[j]) + 1;
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0)
                costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }

}