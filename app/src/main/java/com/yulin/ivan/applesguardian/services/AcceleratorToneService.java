package com.yulin.ivan.applesguardian.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.yulin.ivan.applesguardian.MainActivity;
import com.yulin.ivan.applesguardian.R;

import static android.media.ToneGenerator.MAX_VOLUME;
import static androidx.core.app.NotificationCompat.PRIORITY_MAX;
import static com.yulin.ivan.applesguardian.MainActivity.THRESHOLD_CONST;

public class AcceleratorToneService extends Service implements SensorEventListener {
    public static final String CHANNEL_ID = "ForegroundServiceChannel";

    private static final int TONE_DURATION = 300;
    private static final int SAMPLING_PERIOD = 1000 / 30;
    private SharedPreferences sharedPref;
    private SensorManager sensorManager;
    private ToneGenerator toneGenerator;
    private boolean isTonePlaying = false;
    private Sensor linearAccelerationSensor;

    public AcceleratorToneService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        System.out.println("remove");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, MAX_VOLUME);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        displayNotification();
        sharedPref.edit().putBoolean(getString(R.string.service_is_running), true).apply();

        AsyncTask.execute(() -> sensorManager.registerListener(this, linearAccelerationSensor, SAMPLING_PERIOD));

        return START_STICKY;
    }

    private void displayNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Your apples are protected")
                .setSmallIcon(R.drawable.ic_center_focus_weak_black_24dp)
                .setContentIntent(pendingIntent)
                .setPriority(PRIORITY_MAX)
                .build();

        startForeground(1, notification);

    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float currentThreshold = sharedPref.getFloat(getApplicationContext().getString(R.string.threshold), THRESHOLD_CONST);
        float linearAccAvg = linearSensorValuesAvg(sensorEvent);
        sendBroadcastToActivity(linearAccAvg, linearAccAvg > currentThreshold);

        if (linearAccAvg > currentThreshold) {
            if (!isTonePlaying) {
                isTonePlaying = true;
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_HIGH_L, TONE_DURATION);
                new Handler().postDelayed(() -> isTonePlaying = false, TONE_DURATION);
            }
        }
    }

    private void sendBroadcastToActivity(float linearAccAvg, boolean thresholdExceeded) {
        Intent in = new Intent("com.yulin.ivan.applesguardian");
        in.putExtra(getString(R.string.linear_acc_avg), linearAccAvg);
        in.putExtra(getString(R.string.threshold_exceeded), thresholdExceeded);
        in.putExtra(getString(R.string.service_is_running), true);
        sendBroadcast(in);
    }

    private float linearSensorValuesAvg(SensorEvent sensorEvent) {
        float[] values = new float[3];
        System.arraycopy(sensorEvent.values, 0, values, 0, values.length);

        float sum = 0;
        for (float value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override
    public void onDestroy() {
        sharedPref.edit().putBoolean(getString(R.string.service_is_running), false).apply();
        sensorManager.unregisterListener(this);

        Intent in = new Intent("com.yulin.ivan.applesguardian");
        in.putExtra(getString(R.string.service_is_running), false);
        sendBroadcast(in);

        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        sharedPref.edit().putBoolean(getString(R.string.service_is_running), false).apply();
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        sharedPref.edit().putBoolean(getString(R.string.service_is_running), true).apply();
        super.onRebind(intent);
    }


}
