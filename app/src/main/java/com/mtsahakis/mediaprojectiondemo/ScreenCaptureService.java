package com.mtsahakis.mediaprojectiondemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import androidx.core.util.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ScreenCaptureService extends Service {

    private static final String TAG = "ScreenCaptureService";
    private static final String RESULT_CODE = "RESULT_CODE";
    private static final String DATA = "DATA";
    private static final String ACTION = "ACTION";
    private static final String START = "START";
    private static final String STOP = "STOP";
    private static final String SCREENCAP_NAME = "screencap";

    private MediaProjection mMediaProjection;
    private String mStoreDir;
    private ImageReader mImageReader;
    private Handler mHandler;
    private VirtualDisplay mVirtualDisplay;
    private int mDensity;
    private int mWidth;
    private int mHeight;
    private static int IMAGES_PRODUCED = 0;

    private Intent screenIntent;

    private WindowManager windowManager;
    private View overlayView;

    public static Intent getStartIntent(Context context, int resultCode, Intent data, ScreenCaptureService service) {
        Intent intent = new Intent(context, service.getClass());
        intent.putExtra(ACTION, START);
        intent.putExtra(RESULT_CODE, resultCode);
        intent.putExtra(DATA, data);
        return intent;
    }

    public static Intent getStopIntent(Context context, ScreenCaptureService service) {
        Intent intent = new Intent(context, service.getClass());
        intent.putExtra(ACTION, STOP);
        return intent;
    }

    private static boolean isStartCommand(Intent intent) {
        return (intent.hasExtra(RESULT_CODE) && intent.hasExtra(DATA)
                && intent.hasExtra(ACTION) && START.equals(intent.getStringExtra(ACTION))) || intent.getBooleanExtra("from_notification", false);
    }

    private static boolean isStopCommand(Intent intent) {
        return intent.hasExtra(ACTION) && STOP.equals(intent.getStringExtra(ACTION));
    }

    private static int getVirtualDisplayFlags() {
        return DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    }

    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            mHandler.postDelayed(() -> {
                Image image = null;
                FileOutputStream fos = null;
                try {
                    image = mImageReader.acquireLatestImage();
                    if (image != null) {
                        Image.Plane[] planes = image.getPlanes();
                        ByteBuffer buffer = planes[0].getBuffer();
                        int pixelStride = planes[0].getPixelStride();
                        int rowStride = planes[0].getRowStride();
                        int rowPadding = rowStride - pixelStride * mWidth;

                        Bitmap bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888);
                        bitmap.copyPixelsFromBuffer(buffer);

                        File file = new File(mStoreDir, "myscreen_" + IMAGES_PRODUCED + ".png");
                        fos = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        IMAGES_PRODUCED++;
                        Log.d(TAG, "captured image: " + IMAGES_PRODUCED);

                        bitmap.recycle();

                        if (IMAGES_PRODUCED > 1) {
                            sendImageToServer(file);
                        }

                        mImageReader.setOnImageAvailableListener(null, null);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to capture image: " + e.getMessage(), e);
                } finally {
                    if (image != null) {
                        image.close();
                    }
                    if (fos != null) {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            Log.e(TAG, "Failed to close FileOutputStream: " + e.getMessage(), e);
                        }
                    }
                }
            }, 800);
        }
    }

    private void sendImageToServer(File imageFile) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", imageFile.getName(), RequestBody.create(imageFile, MediaType.parse("image/png")))
                .build();

        Request request = new Request.Builder()
                .url("http://211.202.137.183:1285/process-image")
                .post(requestBody)
                .build();

        sendRequestWithRetry(client, request, 3); // Retry up to 3 times
    }

    private void sendRequestWithRetry(OkHttpClient client, Request request, int retryCount) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to send image: " + e.getMessage(), e);
                if (retryCount > 0) {
                    Log.d(TAG, "Retrying... (" + retryCount + " attempts left)");
                    sendRequestWithRetry(client, request, retryCount - 1);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Failed to receive response: " + response);
                    return;
                }

                try {
                    byte[] imageBytes = response.body().bytes();
                    Bitmap translatedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                    String translatedImagePath = mStoreDir + "/translated_image_" + IMAGES_PRODUCED + ".png";
                    FileOutputStream fos = new FileOutputStream(translatedImagePath);
                    translatedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.close();

                    new Handler(Looper.getMainLooper()).post(() -> displayOverlay(translatedImagePath));
                } catch (IOException e) {
                    Log.e(TAG, "Error processing server response: " + e.getMessage(), e);
                } finally {
                    response.close();
                }
            }
        });
    }


    private void displayOverlay(String imagePath) {
        if (windowManager == null) {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }

        if (overlayView != null) {
            windowManager.removeView(overlayView);
        }

        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null);
        ImageView imageView = overlayView.findViewById(R.id.overlayImageView);
        Button closeButton = overlayView.findViewById(R.id.closeButton);

        Bitmap translatedBitmap = BitmapFactory.decodeFile(imagePath);
        imageView.setImageBitmap(translatedBitmap);

        closeButton.setOnClickListener(v -> removeOverlay());

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        windowManager.addView(overlayView, params);
    }

    private void removeOverlay() {
        if (overlayView != null) {
            windowManager.removeView(overlayView);
            overlayView = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mStoreDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "screenshots").getAbsolutePath();
        File storeDirectory = new File(mStoreDir);
        if (!storeDirectory.exists()) {
            if (!storeDirectory.mkdirs()) {
                Log.e(TAG, "failed to create file storage directory.");
                stopSelf();
            }
        }

        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                mHandler = new Handler();
                Looper.loop();
            }
        }.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isStartCommand(intent)) {
            Pair<Integer, Notification> notification = NotificationUtils.getNotification(this, this);
            startForeground(notification.first, notification.second);

            int resultCode = intent.getIntExtra(RESULT_CODE, Activity.RESULT_CANCELED);

            if(screenIntent == null) {
                screenIntent = intent.getParcelableExtra(DATA);
            }
            startProjection(resultCode, screenIntent);
        } else if (isStopCommand(intent)) {
            stopProjection();
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    @SuppressLint("WrongConstant")
    private void startProjection(int resultCode, Intent data) {
        MediaProjectionManager projectionManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mMediaProjection = projectionManager.getMediaProjection(resultCode, data);

        if (mMediaProjection != null) {
            mDensity = Resources.getSystem().getDisplayMetrics().densityDpi;
            WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            mWidth = size.x;
            mHeight = size.y;

            mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2);
            mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                    SCREENCAP_NAME, mWidth, mHeight, mDensity, getVirtualDisplayFlags(), mImageReader.getSurface(), null, mHandler);

            mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), mHandler);

            mMediaProjection.registerCallback(new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    stopProjection();
                }
            }, mHandler);
        }
    }

    private void stopProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }

        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }

        if (mImageReader != null) {
            mImageReader.setOnImageAvailableListener(null, null);
            mImageReader.close();
            mImageReader = null;
        }

        removeOverlay();
    }
}
