package com.example.quartermasterpackage;

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
                // Start the main activity after the delay
                Intent mainIntent = new Intent(splash_screen.this, MainActivity.class);
                startActivity(mainIntent);

                // Close the splash activity to prevent going back to it
                finish();
            }
        }, 3000); // 3000 milliseconds = 3 seconds
    }
}