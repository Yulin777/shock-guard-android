package com.yulin.ivan.applesguardian;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.yulin.ivan.applesguardian.services.AcceleratorToneService;
import com.yulin.ivan.applesguardian.services.BootDeviceReceiver;

import java.math.BigDecimal;

/**
 * created byy Ivan Y on 25/09/19
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener {

    public final static float THRESHOLD_CONST = 5;
    private static final float MAX_THRESHOLD_VALUE = 20;
    private static final float MIN_THRESHOLD_VALUE = 1;
    public static boolean isRunning = false;
    TextView thresholdTextView;
    TextView vectorsView;
    TextView thresholdExceededTextView;
    float currentThreshold;
    private SharedPreferences sharedPref;
    private RepeatListener touchRepeatListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        isRunning = true;

        // the code to execute repeatedly
        touchRepeatListener = new RepeatListener(400, 100, this);

        thresholdTextView = findViewById(R.id.threshold);
        vectorsView = findViewById(R.id.vectors);
        thresholdExceededTextView = findViewById(R.id.exceed_threshold);
        findViewById(R.id.dec_threshold).setOnClickListener(this);
        findViewById(R.id.dec_threshold).setOnTouchListener(touchRepeatListener);
        findViewById(R.id.inc_threshold).setOnClickListener(this);
        findViewById(R.id.inc_threshold).setOnTouchListener(touchRepeatListener);

        sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        currentThreshold = sharedPref.getFloat(getString(R.string.threshold), THRESHOLD_CONST);
        thresholdTextView.setText(String.valueOf(currentThreshold));

        registerMyReceiver();

        if (isBluetoothHeadsetConnected()) {
            sendBroadcast(new Intent(this, BootDeviceReceiver.class).setAction("doesnt matter"));
        }



    }


    private void registerMyReceiver() {

        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                boolean isServiceRunning = intent.getBooleanExtra(getString(R.string.service_is_running), true);

                if (!isServiceRunning) {
                    vectorsView.setText(getString(R.string.waiting_for_bt_connection));

                } else {

                    float linearAccAvg = intent.getFloatExtra(getString(R.string.linear_acc_avg), 0);
                    vectorsView.setText(String.valueOf(linearAccAvg));

                    boolean thresholdExceeded = intent.getBooleanExtra(getString(R.string.threshold_exceeded), false);
                    if (thresholdExceeded) {
                        thresholdExceededTextView.setText(String.valueOf(linearAccAvg));
                    }
                }
            }
        };

        registerReceiver(broadcastReceiver, new IntentFilter("com.yulin.ivan.applesguardian"));
    }

    @Override
    protected void onStart() {
        isRunning = true;
        super.onStart();
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
                if (currentThreshold == MIN_THRESHOLD_VALUE) return;
                currentThreshold -= .1;
                break;

            case R.id.inc_threshold:
                if (currentThreshold == MAX_THRESHOLD_VALUE) return;
                currentThreshold += .1;
                break;
        }
        currentThreshold = round(currentThreshold, 2);
        thresholdTextView.setText(String.valueOf(currentThreshold));
        sharedPref.edit().putFloat(getString(R.string.threshold), currentThreshold).apply();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
    }


    /**
     * WTF JAVA?!
     */
    public static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

/*

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
*/

    @Override
    protected void onDestroy() {
        isRunning = false;
        super.onDestroy();
    }

    public static boolean isBluetoothHeadsetConnected() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()
                && mBluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothHeadset.STATE_CONNECTED;
    }

}
