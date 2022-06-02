package org.tensorflow.lite.examples.classification;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class MenuActivity extends AppCompatActivity {
    CardView explore, text, currency, person, gender, track;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        //button
        explore = findViewById(R.id.exploreBtn);
        text = findViewById(R.id.textBtn);
        currency = findViewById(R.id.cashBtn);
        person = findViewById(R.id.personBtn);
        gender = findViewById(R.id.agegender);
        track = findViewById(R.id.trackBtn);

        explore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MenuActivity.this, DetectorActivity.class));
            }
        });

        text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MenuActivity.this, TextReader.class));
            }
        });

        currency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MenuActivity.this, ClassifierActivity.class));
            }
        });

        person.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MenuActivity.this, face_detection.class));
            }
        });

        gender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MenuActivity.this, gender.class));
            }
        });

        track.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MenuActivity.this, GPSActivity.class));
            }
        });

    }

}