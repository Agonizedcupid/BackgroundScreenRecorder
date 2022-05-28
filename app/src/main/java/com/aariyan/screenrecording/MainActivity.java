package com.aariyan.screenrecording;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.aariyan.screenrecording.Constant.Constant;
import com.aariyan.screenrecording.Service.RecorderService;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;

public class MainActivity extends AppCompatActivity {

    public static ToggleButton recordBtn;

    private MediaProjectionManager mediaProjectionManager;
//    private MediaProjection mediaProjection;
//    private VirtualDisplay virtualDisplay;
//    private MediaProjectionCallback mediaProjectionCallback;
//    private MediaRecorder mediaRecorder;
//
//    private String videoUri;
//
//    private int screenDensity;
//    private static final int DISPLAY_WIDTH = 720;
//    private static final int DISPLAY_HEIGHT = 1280;
//
//    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
//
//    static {
//        ORIENTATIONS.append(Surface.ROTATION_0, 90);
//        ORIENTATIONS.append(Surface.ROTATION_90, 0);
//        ORIENTATIONS.append(Surface.ROTATION_180, 270);
//        ORIENTATIONS.append(Surface.ROTATION_270, 180);
//    }


    private ConstraintLayout snackBarLayout;

    public static int RESULT_CODE;
    public static Intent DATA;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initMediaRecorder();

        initUI();
    }

    @Override
    protected void onResume() {
        if (RecorderService.isServiceRunning) {
            recordBtn.setChecked(true);
        } else {
            recordBtn.setChecked(false);
        }
        super.onResume();
    }

    private void initMediaRecorder() {
        //mediaRecorder = new MediaRecorder();
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }


    private void initUI() {
        snackBarLayout = findViewById(R.id.snackBarLayout);

        recordBtn = findViewById(R.id.recordBtn);
        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        + ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            || ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.RECORD_AUDIO)) {
                        recordBtn.setChecked(false);
                        Snackbar.make(snackBarLayout, "Permissions", Snackbar.LENGTH_INDEFINITE)
                                .setAction("ENABLE", new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                Manifest.permission.RECORD_AUDIO
                                        }, Constant.REQUEST_PERMISSION);
                                    }
                                }).show();
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.RECORD_AUDIO
                        }, Constant.REQUEST_PERMISSION);
                    }
                } else {
                    toggleScreenShare(view);
                }
            }
        });
    }

    private void toggleScreenShare(View view) {
        if (((ToggleButton) view).isChecked()) {
            //initRecorder();
            recordScreen();
        } else {
            Toast.makeText(this, "Stopping Service ....", Toast.LENGTH_SHORT).show();
            stopMainService();
//            mediaRecorder.stop();
//            mediaRecorder.reset();
//            stopScreenRecording();
        }
    }

    private void recordScreen() {
        if (!RecorderService.isServiceRunning) {
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), Constant.REQUEST_CODE);
            return;
        }
//
//        virtualDisplay = createVirtualDisplay();
//        mediaRecorder.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != Constant.REQUEST_CODE) {
            Toast.makeText(this, "Error!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
            recordBtn.setChecked(false);
            return;
        }

        startMainService(resultCode, data);
    }



//    private void stopScreenRecording() {
//        if (virtualDisplay == null)
//            return;
//
//        virtualDisplay.release();
//        destroyMediaProjection();
//    }
//
//    private void destroyMediaProjection() {
//        if (mediaProjection != null) {
//            mediaProjection.unregisterCallback(mediaProjectionCallback);
//            mediaProjection.stop();
//            mediaProjection = null;
//        }
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constant.REQUEST_PERMISSION: {
                if ((grantResults.length > 0) && grantResults[0] + grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    toggleScreenShare(recordBtn);
                } else {
                    recordBtn.setChecked(false);

                    Snackbar.make(snackBarLayout, "Permissions", Snackbar.LENGTH_INDEFINITE)
                            .setAction("ENABLE", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                            Manifest.permission.RECORD_AUDIO
                                    }, Constant.REQUEST_PERMISSION);
                                }
                            }).show();
                }
                return;
            }
        }
    }

    //Here stopping the service if it's running already:
    private void stopMainService() {
        //Checking whether the service is already running or not:
        if (RecorderService.isServiceRunning) {
            stopService(new Intent(MainActivity.this, RecorderService.class));
        }
    }

    //Starting the main Service to track:
    public void startMainService(int resultCode, Intent data) {
        RESULT_CODE = resultCode;
        DATA = data;
        //Log.d(TAG, "startService called");
        if (!RecorderService.isServiceRunning) {
            //Intent serviceIntent = new Intent(MainActivity.this, RecorderService.class);
            Intent serviceIntent = RecorderService.newIntent(MainActivity.this, resultCode, data);
            ContextCompat.startForegroundService(MainActivity.this,serviceIntent);
            //startService(serviceIntent);
        }
    }
}