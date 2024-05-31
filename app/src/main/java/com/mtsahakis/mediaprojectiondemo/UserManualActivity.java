package com.mtsahakis.mediaprojectiondemo;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class UserManualActivity extends AppCompatActivity {

    private boolean isDarkMode;
    private LinearLayout rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_manual);

        rootView = findViewById(R.id.rootView);

        // Get the dark mode state from the intent
        isDarkMode = getIntent().getBooleanExtra("isDarkMode", false);

        // Apply the dark mode state
        applyDarkMode();

        // Set up the back button in the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // Handle the back button click here
        return true;
    }

    private void applyDarkMode() {
        if (isDarkMode) {
            rootView.setBackgroundColor(ContextCompat.getColor(this, R.color.background_color_dark));
            for (int i = 0; i < rootView.getChildCount(); i++) {
                View view = rootView.getChildAt(i);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(ContextCompat.getColor(this, R.color.text_color_dark));
                }
            }
        } else {
            rootView.setBackgroundColor(ContextCompat.getColor(this, R.color.background_color));
            for (int i = 0; i < rootView.getChildCount(); i++) {
                View view = rootView.getChildAt(i);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(ContextCompat.getColor(this, R.color.text_color));
                }
            }
        }
    }
}
