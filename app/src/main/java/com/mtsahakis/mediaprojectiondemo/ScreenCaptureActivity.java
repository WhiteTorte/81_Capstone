package com.example.Capstone81;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

public class ScreenCaptureActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 100;
    public static final int PERMISSION_REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 101;

    private static Intent projectionIntent;
    private static int projectionResultCode;

    private boolean isDarkMode = false;
    private LinearLayout rootView;
    private final ScreenCaptureService screenService = new ScreenCaptureService();

    @Override
    public void onBackPressed() {
        finish();
        finishAffinity();
        finishAndRemoveTask();
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootView = findViewById(R.id.rootView);
        Button startButton = findViewById(R.id.startButton);
        Button stopButton = findViewById(R.id.stopButton);
        Button darkModeButton = findViewById(R.id.darkModeButton);
        Button userManualButton = findViewById(R.id.userManualButton);
        Button exitButton = findViewById(R.id.exitButton);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotificationUtils.getNotification(ScreenCaptureActivity.this, screenService);
                if (projectionIntent == null) {
                    requestProjectionPermission();
                } else {
                    startScreenCapture();
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopScreenCapture();
            }
        });

        darkModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleDarkMode();
            }
        });

        userManualButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ScreenCaptureActivity.this, UserManualActivity.class);
                intent.putExtra("isDarkMode", isDarkMode);
                startActivity(intent);
            }
        });

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                System.exit(0);
            }
        });

        boolean notification = getIntent().getBooleanExtra("from_notification", false);

        if (getIntent() != null && notification) {
            if (projectionIntent == null) {
                requestProjectionPermission();
            } else {
                startScreenCapture();
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    private void requestProjectionPermission() {
        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent captureIntent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                projectionIntent = data;
                projectionResultCode = resultCode;
                startScreenCapture();
            }
        }
    }

    private void startScreenCapture() {
        if (projectionIntent != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(ScreenCaptureService.getStartIntent(this, projectionResultCode, projectionIntent, screenService));
            } else {
                startService(ScreenCaptureService.getStartIntent(this, projectionResultCode, projectionIntent, screenService));
            }
        }
    }

    private void stopScreenCapture() {
        startService(ScreenCaptureService.getStopIntent(this, screenService));
    }

    private void toggleDarkMode() {
        isDarkMode = !isDarkMode;
        updateDarkMode();
    }

    private void updateDarkMode() {
        int buttonBackgroundColor = isDarkMode ? R.color.button_background_dark : R.color.button_background;
        int textColor = isDarkMode ? R.color.text_color_dark : R.color.text_color;

        rootView.setBackgroundColor(ContextCompat.getColor(this, isDarkMode ? R.color.background_color_dark : R.color.background_color));

        updateTextColors(textColor, buttonBackgroundColor);
    }

    private void updateTextColors(int textColor, int buttonBackgroundColor) {
        TextView appTitle = findViewById(R.id.appTitle);
        TextView subTitle = findViewById(R.id.subTitle);
        TextView description = findViewById(R.id.description);
        Button startButton = findViewById(R.id.startButton);
        Button stopButton = findViewById(R.id.stopButton);
        Button userManualButton = findViewById(R.id.userManualButton);
        Button darkModeButton = findViewById(R.id.darkModeButton);
        Button exitButton = findViewById(R.id.exitButton);

        appTitle.setTextColor(ContextCompat.getColor(this, textColor));
        subTitle.setTextColor(ContextCompat.getColor(this, textColor));
        description.setTextColor(ContextCompat.getColor(this, textColor));

        startButton.setTextColor(ContextCompat.getColor(this, textColor));
        stopButton.setTextColor(ContextCompat.getColor(this, textColor));
        userManualButton.setTextColor(ContextCompat.getColor(this, textColor));
        darkModeButton.setTextColor(ContextCompat.getColor(this, textColor));
        exitButton.setTextColor(ContextCompat.getColor(this, textColor));

        startButton.setBackgroundTintList(ContextCompat.getColorStateList(this, buttonBackgroundColor));
        stopButton.setBackgroundTintList(ContextCompat.getColorStateList(this, buttonBackgroundColor));
        userManualButton.setBackgroundTintList(ContextCompat.getColorStateList(this, buttonBackgroundColor));
        darkModeButton.setBackgroundTintList(ContextCompat.getColorStateList(this, buttonBackgroundColor));
        exitButton.setBackgroundTintList(ContextCompat.getColorStateList(this, buttonBackgroundColor));
    }
}
