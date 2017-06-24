package com.example.thomas.bluetoothcontroller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.github.lzyzsd.circleprogress.DonutProgress;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import static com.example.thomas.bluetoothcontroller.controllerActivity.throttle;

public class controllerActivity extends AppCompatActivity {
    private static final String TAG = "controllerActivity";
    SeekBar throttleBar;
    TextView motorSpeedDisplay;
    TextView btState;
    private int motorSpeed;
    static boolean stopSending = false;
    private DonutProgress dBatteryVoltage;
    private TextView textVoltage;
    RelativeLayout layout;
    static String initialThrottle = "1500, 1n";
    static byte[] throttle = initialThrottle.getBytes(Charset.defaultCharset());
    sendBTData thread = new sendBTData();
    String throttleString;
    int progressValue;
    static double vMin = 20.6;
    static double vMax = 25.2;



    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action))
            {
                updateConnectionState();
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action))
            {
                updateConnectionState();
            }
        }
    };

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "OnReceive: battery update received");
            String vin = intent.getStringExtra("inputData");
            int vint = Integer.parseInt(vin);
            double voltage = (vint/21.031746);
            textVoltage.setText(String.format("%.2f V", voltage));
            Log.d(TAG,"Voltage Read: "+ voltage);

            int pVoltage;
            if(voltage > vMin && voltage < vMax) {
                pVoltage = (int) ((voltage - vMin) / (vMax - vMin) * 100);
            }
            else if(voltage > vMax){
                pVoltage = 100;
            }
            else
                pVoltage = 0;
            dBatteryVoltage.setIntDonut_progress(pVoltage);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);
        init();
        btState = (TextView) findViewById(R.id.showBTState);
        btState.setText(MainActivity.deviceName + " : Disconnected");
        dBatteryVoltage = (DonutProgress) findViewById(R.id.donut_progress);
        textVoltage = (TextView) findViewById(R.id.tVoltage);

        throttleBar.setProgress(throttleBar.getMax() / 2);
        throttleBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public synchronized void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        progressValue = progress;
                        motorSpeed = (progressValue - 1000) / 10;
                        int throttleVal = motorSpeed * 5 + 1500;
                        Integer tempVal = new Integer(throttleVal);
                        throttleString = tempVal.toString();
                        throttleString = throttleString + ", 1n";

                        motorSpeedDisplay.setText("Motor Power: " + motorSpeed);
                        Log.d(TAG, "progress: " + throttleString);
                        throttle = throttleString.getBytes(Charset.defaultCharset());
                       // sendCommand(throttle);
                    }


                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        seekBar.setProgress(seekBar.getMax() / 2);
                        progressValue = 0;
                        int throttleVal = motorSpeed * 5 + 1500;
                        motorSpeed = (progressValue - 1000) / 10;
                        Integer tempVal = new Integer(throttleVal);
                        throttleString = tempVal.toString();
                        throttleString = throttleString + ", 1n";
                        throttle = throttleString.getBytes(Charset.defaultCharset());
                    }
                });
        thread.start();
    }
    protected void onDestroy(){
        super.onDestroy();
        try {
            unregisterReceiver(mBroadcastReceiver);
            unregisterReceiver(mBatInfoReceiver);
        }catch(RuntimeException e){
            Log.e(TAG, "onDestroy: reciever not registered!");
        }
    }

    private void init() {
        throttleBar = (SeekBar) findViewById(R.id.seekBar);
        motorSpeed = 0;
        motorSpeedDisplay = (TextView) findViewById(R.id.motorSpeedDisplay);
        motorSpeedDisplay.setText("Motor Speed: " + 0);
        layout = (RelativeLayout) findViewById(R.id.Relative);

        IntentFilter btDiscoveryIntent = new IntentFilter();
        btDiscoveryIntent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        btDiscoveryIntent.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        btDiscoveryIntent.addAction(BluetoothDevice.ACTION_FOUND);
        btDiscoveryIntent.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        btDiscoveryIntent.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        btDiscoveryIntent.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        btDiscoveryIntent.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        registerReceiver(mBatInfoReceiver, new IntentFilter("BLUETOOTH_MESSAGE_RECEIVED"));
        registerReceiver(mBroadcastReceiver, btDiscoveryIntent);
    }

    public void sendCommand(byte[] throttle) {
        Log.d(TAG, "send throttle");
            try {
                MainActivity.mBluetoothConnection.write(throttle);
                Log.d(TAG, "BT Write: " + throttleString);
            } catch (NullPointerException e) {
                Log.e(TAG, "controllerActivity Write: NullPointerException: " + e.getMessage());
            }
    }
    public void disconnect(View view) {
        if (MainActivity.btConnectionState)
            MainActivity.mBluetoothConnection.cancel();

        Log.d(TAG, "BT Device Connected: " + MainActivity.btConnectionState);
        stopSending = true;
        finish();
    }

    public  void updateConnectionState() {
        if(MainActivity.btConnectionState)
            btState.setText(MainActivity.deviceName + " : Connected");

        if(!MainActivity.btConnectionState)
            btState.setText(MainActivity.deviceName + " : Disconnected");
    }

    public class sendBTData extends Thread{
        public void run(){
            while(!stopSending) {
                sendCommand(throttle);
                try {
                    thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            stopSending=false;
        }
    }
}