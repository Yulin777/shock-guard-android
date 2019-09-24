package com.yulin.ivan.applesguardian;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.yulin.ivan.applesguardian.workers.ToneWorker;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {

    public final static float THRESHOLD_CONST = 5;
    TextView thresholdTextView;
    TextView vectorsView;
    float currentThreshold;
    private SharedPreferences sharedPref;
    private SensorManager sensorManager;
    List list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        thresholdTextView = findViewById(R.id.threshold);
        vectorsView = findViewById(R.id.vectors);
        findViewById(R.id.dec_threshold).setOnClickListener(this);
        findViewById(R.id.inc_threshold).setOnClickListener(this);

        sharedPref = getPreferences(Context.MODE_PRIVATE);

        currentThreshold = sharedPref.getFloat(getString(R.string.threshold), THRESHOLD_CONST);
        thresholdTextView.setText(String.valueOf(currentThreshold));

        PeriodicWorkRequest toneWorkRequest = new PeriodicWorkRequest.Builder(ToneWorker.class, 1000 / 30, TimeUnit.MILLISECONDS).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("toner worker", ExistingPeriodicWorkPolicy.REPLACE, toneWorkRequest);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        setMotionListeners();
    }

    private void setMotionListeners() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        list = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if (list.size() > 0) {
            sensorManager.registerListener(this, (Sensor) list.get(0), SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(getBaseContext(), "Error: No Accelerometer.", Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onClick(View view) {
        String thresholdString = thresholdTextView.getText().toString();
        currentThreshold = Float.valueOf(thresholdString);

        switch (view.getId()) {

            case R.id.dec_threshold:
                if (currentThreshold == 1f) return;
                currentThreshold -= .1;
                break;

            case R.id.inc_threshold:
                if (currentThreshold == 10f) return;
                currentThreshold += .1;
                break;
        }
        currentThreshold = round(currentThreshold, 1);
        thresholdTextView.setText(String.valueOf(currentThreshold).substring(0, 3));
    }

    /**
     * WTF JAVA?!
     */
    public static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }


    @Override
    protected void onPause() {
        super.onPause();

        String thresholdString = thresholdTextView.getText().toString();
        float thresholdNum = Float.valueOf(thresholdString);
        sharedPref.edit().putFloat(getString(R.string.threshold), thresholdNum).apply();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float[] values = sensorEvent.values;
        vectorsView.setText("x: " + values[0] + "\ny: " + values[1] + "\nz: " + values[2]);
        Log.d("", "x: " + values[0] + "\ny: " + values[1] + "\nz: " + values[2]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

}
