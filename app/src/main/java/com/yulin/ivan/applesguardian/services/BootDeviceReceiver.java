package com.yulin.ivan.applesguardian.services;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.yulin.ivan.applesguardian.MainActivity;

import static com.yulin.ivan.applesguardian.MainActivity.isBluetoothHeadsetConnected;

/**
 * Created by Ivan on 2019-09-25.
 */

public class BootDeviceReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {

        Intent serviceIntent = new Intent(context, AcceleratorToneService.class);
        serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        /*
        the main activity is a defined singleton in manifest
        anyway, avoid calling its instance twice
         */
        if (!MainActivity.isRunning) {
            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }


        /*
        turn on the service on BT connection only
         */
        if (intent.getAction() != null) {
            if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_CONNECTED) || isBluetoothHeadsetConnected()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }

            } else /*if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED))*/ {
                context.stopService(serviceIntent);
            }

        }

    }


}