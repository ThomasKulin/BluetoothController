package com.example.thomas.bluetoothcontroller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.Manifest;
import android.os.Build;
import android.widget.AdapterView;


import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static com.example.thomas.bluetoothcontroller.controllerActivity.throttle;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, View.OnClickListener {
    public static final String TAG = "MainActivity";
    public static String deviceAddress;
    public static String deviceName;
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    ArrayAdapter<String> listAdapter;
    ListView listView;
    static BluetoothAdapter btAdapter;
    Set<BluetoothDevice> devicesArray;
    public static ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    public DeviceListAdapter mDeviceListAdapter;
    ListView listViewNewDevices;
    //Context context;

    public static BluetoothConnectionService mBluetoothConnection;
    public static BluetoothDevice cBTDevice;
    public static boolean btConnectionState = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //paired device list
        listView = (ListView) findViewById(R.id.listView);
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, 0);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(MainActivity.this);

        //available device list
        listViewNewDevices = (ListView) findViewById(R.id.listViewNewDevices);
        listViewNewDevices.setOnItemClickListener(MainActivity.this);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        mBTDevices = new ArrayList<>();

        IntentFilter btDiscoveryIntent = new IntentFilter();
        btDiscoveryIntent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        btDiscoveryIntent.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        btDiscoveryIntent.addAction(BluetoothDevice.ACTION_FOUND);
        btDiscoveryIntent.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        btDiscoveryIntent.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        btDiscoveryIntent.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        btDiscoveryIntent.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(mBroadcastReceiver, btDiscoveryIntent);

        IntentFilter filter = new IntentFilter();
        registerReceiver(mBroadcastReceiver2, filter);

        //Paired Device List
        if (btAdapter == null) {
            Toast.makeText(getApplicationContext(), "No Bluetooth detected", Toast.LENGTH_SHORT).show();
            finish();
        }
        else {
            if (!btAdapter.isEnabled()) {
                turnOnBT();
            }
            getPairedDevices();
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive: called");

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                if (state == BluetoothAdapter.STATE_ON) {
                    Log.d(TAG, "ACTION_STATE_CHANGED: STATE_ON");
                }
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.d(TAG,"ACTION_DISCOVERY_STARTED");
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG, "ACTION_DISCOVERY_FINISHED");
            }
            else if (BluetoothDevice.ACTION_FOUND.equals(action)){
                Log.d(TAG, "onReceive: ACTION FOUND");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(!mBTDevices.contains(device) && !devicesArray.contains(device) ) {
                    mBTDevices.add(device);
                    Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                    mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                    listViewNewDevices.setAdapter(mDeviceListAdapter);
                }
            }
            else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action))
            {
                Log.d(TAG, "onReceive: Bluetooth Connected");
                //Update();
                btConnectionState = true;
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action))
            {
                Log.d(TAG, "onReceive: Bluetooth Disconnected");
                //Update();
                btConnectionState = false;
            }
            else {
                Log.d(TAG, "Action not found: " + action);
            }
        }
    };

    private BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive broadcast receiver 2: called");
            final String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //option 1: bonded already
                if(mDevice.getBondState() == BluetoothDevice.BOND_BONDING){
                    Log.d(TAG, "BroadcastReceiver2: BOND_BONDED");
                    //cBTDevice = mDevice;
                }
                //option 2: creating a bond
                else if(mDevice.getBondState() == BluetoothDevice.BOND_BONDING){
                    Log.d(TAG, "BroadcastReceiver2: BOND_BONDING");

                }
                //option 3: breaking a bond
                else if (mDevice.getBondState() == BluetoothDevice.BOND_NONE){
                    Log.d(TAG, "BroadcastReceiver2: BOND_NONE");

                }

            }


        }
    };
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "OnDestroy: called");
        unregisterReceiver(mBroadcastReceiver);
        unregisterReceiver(mBroadcastReceiver2);
        try{
            btAdapter.cancelDiscovery();
        }catch(RuntimeException e){
            e.printStackTrace();
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            Toast.makeText(getApplicationContext(), "Bluetooth must be enabled to continue", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    public void switchtoControllerActivity(){
        Log.d(TAG, "switchto: called");
        Intent newActivity = new Intent(this, controllerActivity.class);
        startActivity(newActivity);
    }

    private void turnOnBT() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, 1);
    }

    private void getPairedDevices() {
        listAdapter.clear();
        devicesArray = btAdapter.getBondedDevices();
        if (devicesArray.size() > 0) {
            for (BluetoothDevice device : devicesArray) {
                listAdapter.add(device.getName() + "\n" + device.getAddress());
                if(mBTDevices.contains(device)){
                   mBTDevices.remove(device);
                }
            }
        }
    }

    public void btDiscover(View view) {
        Log.d(TAG, "btDiscover: Looking for unpaired devices.");

        if (btAdapter.isDiscovering()) {
            Log.d(TAG, "Pair Board: Cancelling discovery.");
            btAdapter.cancelDiscovery();

            //check BT permissions in android 5+
            //checkBTPermissions();

            btAdapter.startDiscovery();
        }
        else if (!btAdapter.isDiscovering()){
            Log.d(TAG, "Pair Board: Starting Discovery");
            checkBTPermissions();
            btAdapter.startDiscovery();
        }
        getPairedDevices();
    }

    public void startConnection(BluetoothDevice device){

        startBTConnection(device, MY_UUID);
    }

    public void startBTConnection(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");

        mBluetoothConnection.startClient(device,uuid);
    }

    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }else{
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        //first cancel discovery because its very memory intensive.
       btAdapter.cancelDiscovery();
        Log.d(TAG, "the adapter you used is: " + adapterView.getId());
        if(adapterView.getId() == R.id.listView){
            Log.d(TAG, "Paired Devices: connect");

            Object[] arrayView = devicesArray.toArray();
            cBTDevice = (BluetoothDevice) arrayView[i];

            deviceName = cBTDevice.getName();
            deviceAddress = cBTDevice.getAddress();
            Log.d(TAG, "onItemClick: deviceName = " + deviceName);
            Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

            mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
            Log.d(TAG, "BT Device Connected: " + btConnectionState);
            startConnection(cBTDevice);
            switchtoControllerActivity();


        }
        else if (adapterView.getId() == R.id.listViewNewDevices) {
            Log.d(TAG, "New Devices: pair");
            Log.d(TAG, "onItemClick: You Clicked on a device.");
            String deviceName = mBTDevices.get(i).getName();
            String deviceAddress = mBTDevices.get(i).getAddress();

            Log.d(TAG, "onItemClick: deviceName = " + deviceName);
            Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

            //create the bond.
            //NOTE: Requires API 17+? I think this is JellyBean
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                Log.d(TAG, "Trying to pair with " + deviceName);
                mBTDevices.get(i).createBond();
            }
        }
    }

    @Override
    public void onClick(View view) {
    }
}