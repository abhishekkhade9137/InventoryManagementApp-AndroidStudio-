package com.example.inventoryapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class splash_screen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Skip login - go directly to MainActivity
                Intent mainIntent = new Intent(splash_screen.this, MainActivity.class);
                startActivity(mainIntent);
                finish();
            }
        }, 2000); // 2 seconds
    }
}
