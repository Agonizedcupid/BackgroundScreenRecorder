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
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;

public class MainActivity extends AppCompatActivity {

    private ToggleButton recordBtn;

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private MediaProjectionCallback mediaProjectionCallback;
    private MediaRecorder mediaRecorder;

    private String videoUri;

    private int screenDensity;
    private static final int DISPLAY_WIDTH = 720;
    private static final int DISPLAY_HEIGHT = 1280;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }


    private ConstraintLayout snackBarLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //getting Display density:
        displayMetrics();

        //init Media Recorder:
        initMediaRecorder();

        initUI();
    }

    private void initMediaRecorder() {
        mediaRecorder = new MediaRecorder();
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    private void displayMetrics() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenDensity = metrics.densityDpi;

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
            initRecorder();
            recordScreen();
        } else {
            mediaRecorder.stop();
            mediaRecorder.reset();
            stopScreenRecording();
        }
    }

    private void recordScreen() {
        if (mediaProjection == null) {
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), Constant.REQUEST_CODE);
            return;
        }

        virtualDisplay = createVirtualDisplay();
        mediaRecorder.start();
    }

    private VirtualDisplay createVirtualDisplay() {
        return mediaProjection.createVirtualDisplay("MainActivity", DISPLAY_WIDTH, DISPLAY_HEIGHT, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.getSurface(), null, null);
    }

    private void initRecorder() {
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        videoUri = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                + new StringBuilder("/AARIYAN_").append(System.currentTimeMillis()).append(".mp4").toString();

        mediaRecorder.setOutputFile(videoUri);
        mediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
        //mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setVideoEncodingBitRate(512 * 1000);
        mediaRecorder.setVideoFrameRate(30);

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int orientation = ORIENTATIONS.get(rotation + 90);
        mediaRecorder.setOrientationHint(orientation);
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

        mediaProjectionCallback = new MediaProjectionCallback();
        mediaProjection =  mediaProjectionManager.getMediaProjection(resultCode, data);
        mediaProjection.registerCallback(mediaProjectionCallback, null);
        virtualDisplay = createVirtualDisplay();
        mediaRecorder.start();
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            if (recordBtn.isChecked()) {
                recordBtn.setChecked(false);
                mediaRecorder.stop();
                mediaRecorder.reset();
            }
            mediaProjection = null;
            stopScreenRecording();
            super.onStop();
        }
    }

    private void stopScreenRecording() {
        if (virtualDisplay == null)
            return;

        virtualDisplay.release();
        destroyMediaProjection();
    }

    private void destroyMediaProjection() {
        if (mediaProjection != null) {
            mediaProjection.unregisterCallback(mediaProjectionCallback);
            mediaProjection.stop();
            mediaProjection = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constant.REQUEST_PERMISSION:
            {
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
}