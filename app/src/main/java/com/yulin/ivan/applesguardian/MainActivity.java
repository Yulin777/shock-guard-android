package com.yulin.ivan.applesguardian;

import androidx.appcompat.app.AppCompatActivity;

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
import android.os.Handler;

import java.math.BigDecimal;
import java.util.List;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {

    private final static float THRESHOLD_CONST = 5;
    private static final int TONE_DURATION = 300;
    TextView thresholdTextView;
    TextView vectorsView;
    float currentThreshold;
    private SharedPreferences sharedPref;
    private SensorManager sensorManager;
    List list;
    private ToneGenerator toneGenerator;
    private boolean isTonePlaying = false;

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
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
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
                currentThreshold -= .1;
                break;

            case R.id.inc_threshold:
                currentThreshold += .1;
                break;
        }
        currentThreshold = round(currentThreshold, 1);
        thresholdTextView.setText(String.valueOf(currentThreshold).substring(0, 3));
    }

    @Override
    protected void onStop() {
        if (list.size() > 0) {
            sensorManager.unregisterListener(this);
        }
        super.onStop();
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
}
