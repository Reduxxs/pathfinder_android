package org.tensorflow.lite.examples.classification;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import org.tensorflow.lite.examples.classification.R;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SplashScreen extends AppCompatActivity {
    Handler handler;
    ImageView box, eyeshape, iris, circuit;
    Animation topAnim, bottomAnim, midAnim1, midAnim2;
    TextView appName;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);

        topAnim = AnimationUtils.loadAnimation(this, R.anim.box_animation);
        bottomAnim = AnimationUtils.loadAnimation(this, R.anim.eyeris_animation);
        midAnim1 = AnimationUtils.loadAnimation(this, R.anim.circuit_animation);
        midAnim2 = AnimationUtils.loadAnimation(this, R.anim.app_name_animation);

        box = findViewById(R.id.imgBox);
        eyeshape = findViewById(R.id.imgEye);
        iris = findViewById(R.id.imgIris);
        circuit = findViewById(R.id.imgCircuit);
        appName = findViewById(R.id.app_name);

        box.setAnimation(topAnim);
        eyeshape.setAnimation(bottomAnim);
        iris.setAnimation(bottomAnim);
        circuit.setAnimation(midAnim1);
        appName.setAnimation(midAnim2);

        handler=new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sharedPreferences = getSharedPreferences("Welcome Screen", MODE_PRIVATE);
                boolean isFirstTime = sharedPreferences.getBoolean("firstTime", true);
                if (isFirstTime){
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("firstTime", false);
                    editor.commit();

                    Intent intent=new Intent(SplashScreen.this, WelcomeMessage.class);
                    startActivity(intent);
                    finish();
                }
                else {
                    Intent intent=new Intent(SplashScreen.this, MenuActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        },5000);
    }
}
