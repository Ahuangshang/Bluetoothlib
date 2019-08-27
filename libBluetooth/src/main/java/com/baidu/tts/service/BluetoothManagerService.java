package com.baidu.tts.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.baidu.tts.BTClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class BluetoothManagerService extends Service {
    public static String HEART_RATE_MEASUREMENT = "0000ffe1-0000-1000-8000-00805f9b34fb";
    protected static String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    protected static String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    protected static String EXTRAS_DEVICE_RSSI = "RSSI";
    public static boolean mConnected = false;
    private String status = "disconnected";
    private String mDeviceAddress;
    public static boolean nConnect = false;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<>();
    public static BluetoothGattCharacteristic target_chara = null;
    public static String RecvString = "";
    public static String ObjectRecv = "";
    @SuppressLint("HandlerLeak")
    private Handler myHandler = new Handler() {
        //2.重写消息处理函数
        public void handleMessage(Message msg) {
            switch (msg.what) {
                //判断发送的消息
                case 1: {
                    //更新View
                    String state = msg.getData().getString("connect_state");
                    if (state.equals("connected")) {
                        nConnect = true;
                    } else {
                        nConnect = false;
                    }
                    break;
                }
                case 2: {
                    String state = msg.getData().getString("RecvData");
                    RecvString += state;
                    break;
                }
            }
            super.handleMessage(msg);
        }
    };
    public static String DeviceName = "";
    private static final int Type = 0;
    private Handler mHandler;
    private ArrayList<Integer> rssis;
    public static ArrayList<BluetoothDevice> mBleArray;
    boolean _discoveryFinished = false;
    boolean bRun = true;
    boolean bThread = false;
    public static BluetoothAdapter mBluetoothAdapter;
    public static boolean mScanning;
    public static boolean scan_flag;
    int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 10000;
    public static int Reader_type = 0;

    public BluetoothManagerService() {
    }

    private static final String TAG = "LocalService";
    private IBinder binder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    //定义内容类继承Binder
    public class LocalBinder extends Binder {
        //返回本地服务
        BluetoothManagerService getService() {
            return BluetoothManagerService.this;
        }
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.i(TAG, "onStart");
        super.onStart(intent, startId);
        mHandler = new Handler();
        mScanning = false;
        mBleArray = new ArrayList<BluetoothDevice>();
        Intent gattServiceIntent = new Intent(BluetoothManagerService.this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        try {
            unbindService(mServiceConnection);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Service not registered");
        }
        try {
            unregisterReceiver(mGattUpdateReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Receiver not registered");
        }
        BTClient.mBluetoothLeService = null;
    }

    /*service 回调函数*/
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            BTClient.mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!BTClient.mBluetoothLeService.initialize()) {
            }
            // Automatically connects to the device upon successful start-up initialization.
            //调用bluetoothservice 的connect 函数，进行连接
            BTClient.mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            BTClient.mBluetoothLeService = null;
        }
    };


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                status = "connected";
                updateConnectionState(status);
                //nConnect=true;
                /////////////////////////////////////////////////////////////
                System.out.println("BroadcastReceiver :" + "device connected");

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                status = "disconnected";
                updateConnectionState(status);
                //nConnect=false;
                // unregisterReceiver(mGattUpdateReceiver);//断开链接注销
                //  BTClient.mBluetoothLeService = null;
                System.out.println("BroadcastReceiver :" + "device disconnected");

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(BTClient.mBluetoothLeService.getSupportedGattServices());
                System.out.println("BroadcastReceiver :" + "device SERVICES_DISCOVERED");
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //byte[] Msg = intent.getExtras().getByteArray(BluetoothLeService.EXTRA_DATA);
                String temp = intent.getExtras().getString(BluetoothLeService.EXTRA_DATA);

                displayData(temp);
            }
        }
    };

    /*更新连接状态*/
    private void updateConnectionState(String status) {
        Message msg = new Message();
        msg.what = 1;
        Bundle b = new Bundle();
        b.putString("connect_state", status);
        msg.setData(b);
        myHandler.sendMessage(msg);
        System.out.println("connect_state:" + status);
    }


    /*意图过滤器*/
    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


    private void displayData(String rev_string) {
        Message msg = new Message();
        msg.what = 2;
        Bundle b = new Bundle();
        b.putString("RecvData", rev_string);
        msg.setData(b);
        myHandler.sendMessage(msg);
        System.out.println("RecvData:" + rev_string);
    }

    public void ScanBtDevice(boolean enable) {
        if (enable) {
            Log.i("SCAN", "begin.....................");
            mScanning = true;
            scan_flag = false;
            if (!mBleArray.isEmpty())
                mBleArray.clear();
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            Log.i("Stop", "stoping................");
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            scan_flag = true;
        }
    }

    public boolean GetConnectState() {
        return nConnect;
    }

    public void ConnectBT(String mDeviceAddress) {
        if (mScanning) {
            /*停止扫描设备*/
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }

        if (BTClient.mBluetoothLeService != null) {
            final boolean result = BTClient.mBluetoothLeService.connect(mDeviceAddress);
        }
    }

    public void DisconnectBT() {
        //unregisterReceiver(mGattUpdateReceiver);
        BTClient.mBluetoothLeService.disconnect();
        BTClient.mBluetoothLeService.close();
        //BTClient.mBluetoothLeService = null;
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {


        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = "unknown_service";
        String unknownCharaString = "unknown_characteristic";

        //服务数据,可扩展下拉列表的第一级数据
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();

        //特征数据（隶属于某一级服务下面的特征值集合）
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();

        //部分层次，所有特征值集合
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {

            //获取服务列表
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();

            //查表，根据该uuid获取对应的服务名称。SampleGattAttributes这个表需要自定义。

            gattServiceData.add(currentServiceData);

            System.out.println("Service uuid:" + uuid);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();

            //从当前循环所指向的服务中读取特征值列表
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();

            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            //对于当前循环所指向的服务中的每一个特征值
            for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();


                if (gattCharacteristic.getUuid().toString().equals(HEART_RATE_MEASUREMENT)) {
                    //测试读取当前Characteristic数据，会触发mOnDataAvailable.onCharacteristicRead()


                    //接受Characteristic被写的通知,收到蓝牙模块的数据后会触发mOnDataAvailable.onCharacteristicWrite()
                    BTClient.mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                    target_chara = gattCharacteristic;
                    //设置数据内容
                    //往蓝牙模块写入数据
                    //mBluetoothLeService.writeCharacteristic(gattCharacteristic);
                }
                List<BluetoothGattDescriptor> descriptors = gattCharacteristic.getDescriptors();
                for (BluetoothGattDescriptor descriptor : descriptors) {
                    System.out.println("---descriptor UUID:" + descriptor.getUuid());
                    //获取特征值的描述
                    BTClient.mBluetoothLeService.getCharacteristicDescriptor(descriptor);
                    //mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                }

                gattCharacteristicGroupData.add(currentCharaData);
            }
            //按先后顺序，分层次放入特征值集合中，只有特征值
            mGattCharacteristics.add(charas);
            //构件第二级扩展列表（服务下面的特征值）
            gattCharacteristicData.add(gattCharacteristicGroupData);

        }
    }

    /*扫描蓝牙设备的回调函数，会返回蓝牙BluetoothDevice，可以获取name MAC 等等*/
    public BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    // TODO Auto-generated method stub
                    if (!mBleArray.contains(device)) {
                        mBleArray.add(device);
                        //rssis.add(rssi);
                    }
                    System.out.println("Address:" + device.getAddress());
                }
            };


}
