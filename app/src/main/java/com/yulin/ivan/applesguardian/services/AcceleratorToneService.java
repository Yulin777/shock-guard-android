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
import android.os.IBinder;
import android.os.Handler;

import androidx.core.app.NotificationCompat;

import com.yulin.ivan.applesguardian.MainActivity;
import com.yulin.ivan.applesguardian.R;

import java.util.List;

import static android.media.ToneGenerator.MAX_VOLUME;
import static androidx.core.app.NotificationCompat.PRIORITY_MAX;
import static com.yulin.ivan.applesguardian.MainActivity.THRESHOLD_CONST;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class AcceleratorToneService extends Service {
    public static final String CHANNEL_ID = "ForegroundServiceChannel";

    private static final int TONE_DURATION = 300;
    private float currentThreshold;
    private SharedPreferences sharedPref;
    private SensorManager sensorManager;
    private List list;
    private ToneGenerator toneGenerator;
    private SensorEventListener sensorEventListener;
    private boolean isTonePlaying = false;
    private SensorEvent _sensorEvent;

    public AcceleratorToneService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sharedPref = getApplicationContext().getSharedPreferences("", Context.MODE_PRIVATE);

        toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, MAX_VOLUME);

        sensorManager = (SensorManager) getApplicationContext().getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            list = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        }

        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                _sensorEvent = sensorEvent;
                float[] values = _sensorEvent.values;
                currentThreshold = sharedPref.getFloat(getApplicationContext().getString(R.string.threshold), THRESHOLD_CONST);

                if (sqrt(pow(values[0], 2) + pow(values[1], 2) + pow(values[2], 2)) > currentThreshold + 10) {
                    if (!isTonePlaying) {
                        isTonePlaying = true;
                        toneGenerator.startTone(ToneGenerator.TONE_CDMA_HIGH_L, TONE_DURATION);
                        new Handler().postDelayed(() -> isTonePlaying = false, TONE_DURATION);
                    }
                }

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
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

        AsyncTask.execute(() -> {
            if (list.size() > 0) {
                sensorManager.registerListener(sensorEventListener, (Sensor) list.get(0), SensorManager.SENSOR_DELAY_NORMAL);
            }
        });

        return START_STICKY;
    }



    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}
