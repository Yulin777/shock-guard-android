package com.yulin.ivan.applesguardian;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.yulin.ivan.applesguardian.services.AcceleratorToneService;

import java.math.BigDecimal;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {

    public final static float THRESHOLD_CONST = 5;
    TextView thresholdTextView;
    TextView vectorsView;
    float currentThreshold;
    private SharedPreferences sharedPref;
    private BroadcastReceiver broadcastReceiver;
    private TextView thresholdExceededTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        thresholdTextView = findViewById(R.id.threshold);
        vectorsView = findViewById(R.id.vectors);
        thresholdExceededTextView = findViewById(R.id.exceed_threshold);
        findViewById(R.id.dec_threshold).setOnClickListener(this);
        findViewById(R.id.inc_threshold).setOnClickListener(this);

        sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        currentThreshold = sharedPref.getFloat(getString(R.string.threshold), THRESHOLD_CONST);
        thresholdTextView.setText(String.valueOf(currentThreshold));

        registerMyReceiver();
        startService();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }
    }

    private void registerMyReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                float linearAccAvg = intent.getFloatExtra(getString(R.string.linear_acc_avg), 0);
                vectorsView.setText(String.valueOf(linearAccAvg));

                boolean thresholdExceeded = intent.getBooleanExtra(getString(R.string.threshold_exceeded), false);
                if (thresholdExceeded) {
                    thresholdExceededTextView.setText(String.valueOf(linearAccAvg));
                }
            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter("com.yulin.ivan.applesguardian"));
    }


    public void startService() {
        boolean isServiceRunning = sharedPref.getBoolean(getString(R.string.service_is_running), false);
        if (!isServiceRunning) {
            Intent serviceIntent = new Intent(this, AcceleratorToneService.class);
            ContextCompat.startForegroundService(this, serviceIntent);
        }
    }

    @SuppressLint("ApplySharedPref")
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
        currentThreshold = round(currentThreshold, 2);
        thresholdTextView.setText(String.valueOf(currentThreshold));
        sharedPref.edit().putFloat(getString(R.string.threshold), currentThreshold).apply();
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

    @SuppressLint("SetTextI18n")
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
