package com.example.Capstone81;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class TranslatedImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translated_image);

        String imagePath = getIntent().getStringExtra("translatedImagePath");
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);

        ImageView imageView = findViewById(R.id.translatedImageView);
        imageView.setImageBitmap(bitmap);
    }
}
