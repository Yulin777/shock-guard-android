package com.yulin.ivan.applesguardian.workers;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;
import com.yulin.ivan.applesguardian.MainActivity;
import com.yulin.ivan.applesguardian.R;

import java.util.List;

import static android.content.Context.SENSOR_SERVICE;
import static com.yulin.ivan.applesguardian.MainActivity.THRESHOLD_CONST;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * Created by Ivan on 2019-09-22.
 */

public class ToneWorker extends ListenableWorker {

    public static final int TONE_DURATION = 300;
    float currentThreshold;
    private SharedPreferences sharedPref;
    private SensorManager sensorManager;
    List list;
    private ToneGenerator toneGenerator;
    private SensorEventListener sensorEventListener;
    private boolean isTonePlaying = false;
    private SensorEvent _sensorEvent;

    public ToneWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        sharedPref = context.getSharedPreferences("", Context.MODE_PRIVATE);

        toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

        sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        list = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);

        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                _sensorEvent = sensorEvent;
                float[] values = _sensorEvent.values;
                currentThreshold = sharedPref.getFloat(context.getString(R.string.threshold), THRESHOLD_CONST);

                if (sqrt(pow(values[0], 2) + pow(values[1], 2) + pow(values[2], 2)) > currentThreshold + 10) {
                    if (!isTonePlaying) {
                        isTonePlaying = true;
                        toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, TONE_DURATION);
                        new Handler().postDelayed(() -> isTonePlaying = false, TONE_DURATION);
                    }
                }

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };
    }


    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        if (list.size() > 0) {
            sensorManager.registerListener(sensorEventListener, (Sensor) list.get(0), SensorManager.SENSOR_DELAY_NORMAL);
        }

        return null;
    }
}
