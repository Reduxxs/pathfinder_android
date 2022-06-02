package org.tensorflow.lite.examples.classification;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

public class WelcomeMessage extends AppCompatActivity {
    TextView textView;
    //TextToSpeech textToSpeech;
    LinearLayout screenLayout;
    VideoView videoView;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_message);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        videoView = findViewById(R.id.videoBackground);

        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.video_background);

        videoView.setVideoURI(uri);

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
            }
        });

        videoView.start();

        screenLayout = findViewById(R.id.welcome_screen_layout);

        textView = findViewById(R.id.welcome_msg);

        screenLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeMessage.this, MenuActivity.class));
                finish();
            }
        });

        /*screenLayout.setOnTouchListener(new View.OnTouchListener() {
            GestureDetector gestureDetector = new GestureDetector(getApplicationContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public void onLongPress(MotionEvent e) {
                    super.onLongPress(e);
                }

                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    startActivity(new Intent(WelcomeMessage.this, MenuActivity.class));
                    finish();
                    return super.onDoubleTap(e);
                }
            });

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return false;
            }
        });

         */
    }
}