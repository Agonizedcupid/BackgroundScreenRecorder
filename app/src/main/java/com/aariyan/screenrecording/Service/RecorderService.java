package com.aariyan.screenrecording.Service;

import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION;
import static android.os.Environment.DIRECTORY_MOVIES;
import static com.aariyan.screenrecording.MainActivity.recordBtn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.aariyan.screenrecording.MainActivity;
import com.aariyan.screenrecording.R;

import java.io.IOException;

public class RecorderService extends Service {
    private static final String EXTRA_RESULT_CODE = "resultcode";
    private static final String EXTRA_DATA = "data";

    private int resultCode;
    private Intent datas;

    //For checking is the service is running or not:
    public static boolean isServiceRunning;

    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private MediaProjectionCallback mediaProjectionCallback;
    private MediaRecorder mediaRecorder;

    private String videoUri;

    private int screenDensity;
    private static final int DISPLAY_WIDTH = 720;
    private static final int DISPLAY_HEIGHT = 1280;

    //Channel id will be used for NotificationChannel:
    private String CHANNEL_ID = "com.udvash.supervisor";

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    public static Intent newIntent(Context context, int resultCode, Intent data) {
        Intent intent = new Intent(context, RecorderService.class);
        intent.putExtra(EXTRA_RESULT_CODE, resultCode);
        intent.putExtra(EXTRA_DATA, data);
        return intent;
    }

    public RecorderService() {
        isServiceRunning = false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //notifying the service is running:
        isServiceRunning = true;
//Creating a notification channel for latest android version
        createNotificationChannel();
        createNotification();

        initRecorder();
    }

    private void createNotification() {
        //This intent will be used as pending intent; means when user will click on notification tab it will open this activity:
        Intent notificationIntent = new Intent(this, MainActivity.class);
        //Attaching the pending intent:
        //PendingIntent.FLAG_IMMUTABLE is used for >= android 11:
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                //this is the notification title:
                .setContentTitle("Service is Running")
                //Notification sub-title:
                .setContentText("Activity Tracking!")
                //notification icon:
                //setting the pending intent on the notification:
                .setContentIntent(pendingIntent)
                //set the background color of intent
                .setColor(getResources().getColor(R.color.teal_700))
                //Finally build the notification to show:
                .build();
        /**
         * A started service can use the startForeground API to put the service in a foreground state,
         * where the system considers it to be something the user is actively aware of and thus not
         * a candidate for killing when low on memory.
         */
        // it will starting show the ForeGround notification:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        }
    }

    //Notification channel is only needed for above Oreo:
    private void createNotificationChannel() {
        //Checking the device OS version:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //App name
            String appName = getString(R.string.app_name);
            //creating the notification channel here and adding all the information:
            NotificationChannel serviceChannel = new NotificationChannel(
                    //Channel id. that could be anything but same package name is recommended:
                    CHANNEL_ID,
                    //Putting the app name to show
                    appName,
                    //This is the importance on notification showing:
                    //For now we are setting as Default:
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            //Instantiating the Notification Manager:
            NotificationManager manager = getSystemService(NotificationManager.class);
            //Finally creating the notification channel and passing as parameter of manager:
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //return super.onStartCommand(intent, flags, startId);
        resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        datas = intent.getParcelableExtra(EXTRA_DATA);
        Toast.makeText(this, "Service Started!", Toast.LENGTH_SHORT).show();

        startRecording(resultCode, datas);

        return START_REDELIVER_INTENT;
    }

    private void startRecording(int resultCode, Intent datas) {

        mediaProjectionCallback = new MediaProjectionCallback();
        MediaProjectionManager mProjectionManager = (MediaProjectionManager) getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
//        mediaRecorder = new MediaRecorder();
//
//        DisplayMetrics metrics = new DisplayMetrics();
//        WindowManager wm = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
//        wm.getDefaultDisplay().getRealMetrics(metrics);
//
//        int mScreenDensity = metrics.densityDpi;
//        int displayWidth = metrics.widthPixels;
//        int displayHeight = metrics.heightPixels;
//
//        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
//        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//        mediaRecorder.setVideoEncodingBitRate(8 * 1000 * 1000);
//        mediaRecorder.setVideoFrameRate(15);
//        mediaRecorder.setVideoSize(displayWidth, displayHeight);
//
//        String videoDir = Environment.getExternalStoragePublicDirectory(DIRECTORY_MOVIES).getAbsolutePath();
//        Long timestamp = System.currentTimeMillis();
//
//        String orientation = "portrait";
//        if( displayWidth > displayHeight ) {
//            orientation = "landscape";
//        }
//        String filePathAndName = videoDir + "/time_" + timestamp.toString() + "_mode_" + orientation + ".mp4";
//
//        mediaRecorder.setOutputFile( filePathAndName );
//
//        try {
//            mediaRecorder.prepare();
//        } catch (IllegalStateException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        mediaProjection = mProjectionManager.getMediaProjection(resultCode, datas);

        mediaProjection.registerCallback(mediaProjectionCallback, null);
        virtualDisplay = createVirtualDisplay();

        mediaRecorder.start();

        Log.v("SERVICE_TEST", "Started recording");

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        isServiceRunning = false;
        stopForeground(true);
        super.onDestroy();
    }

    private VirtualDisplay createVirtualDisplay() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealMetrics(metrics);

        int mScreenDensity = metrics.densityDpi;
        int displayWidth = metrics.widthPixels;
        int displayHeight = metrics.heightPixels;
        return mediaProjection.createVirtualDisplay("MainActivity", displayWidth, displayHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.getSurface(), null, null);
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

    private void initRecorder() {
        mediaRecorder = new MediaRecorder();
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

//        int rotation = getWindowManager().getDefaultDisplay().getRotation();
//        int orientation = ORIENTATIONS.get(rotation + 90);
//        mediaRecorder.setOrientationHint(orientation);
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
}