package com.baidu.tts.ui;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.baidu.tts.R;
import com.baidu.tts.adapter.BluetoothDeviceAdapter;
import com.baidu.tts.entry.BluetoothDeviceModel;
import com.baidu.tts.service.BluetoothManagerService;
import com.baidu.tts.util.RFIDReaderUtil;
import com.baidu.tts.viewutil.DialogBtnClickListener;
import com.baidu.tts.viewutil.DialogUtil;
import com.chad.library.adapter.base.BaseQuickAdapter;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.baidu.tts.util.RFIDReaderUtil.READ_CARD_INSTANCE_TYPE_HIGH;
import static com.baidu.tts.util.RFIDReaderUtil.READ_CARD_INSTANCE_TYPE_LOW;
import static com.baidu.tts.util.RFIDReaderUtil.READ_CARD_INSTANCE_TYPE_MIDDLE;
import static com.baidu.tts.util.RFIDReaderUtil.READ_CARD_INSTANCE_TYPE_VERY_HIGH;
import static com.baidu.tts.viewutil.DialogUtil.BUTTON_TYPE_NO_BTN;
import static com.baidu.tts.viewutil.DialogUtil.CONTENT_TYPE_VIEW;

public class BluetoothDeviceListActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_CODE_PERMISSION_LOCATION = 100;
    private static final int REQUEST_CODE_OPEN_GPS = 200;
    private Handler mHandler = new Handler();
    private static final long SCAN_PERIOD = 15000;
    private boolean isScanning = false;
    private RecyclerView rvBlueDevices;
    private BluetoothDeviceAdapter bluetoothDeviceAdapter;
    private static final int RESULT_NUM = 300;
    private ArrayList<BluetoothDeviceModel> blueDeviceData = new ArrayList<>();
    private BluetoothManagerService myService;
    int distance = READ_CARD_INSTANCE_TYPE_LOW;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_device);

        RFIDReaderUtil.init();

        rvBlueDevices = findViewById(R.id.rv_blue_devices);

        //设置布局管理器
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rvBlueDevices.setLayoutManager(layoutManager);

        //设置适配器
        bluetoothDeviceAdapter = new BluetoothDeviceAdapter(R.layout.item_blue_device_list, blueDeviceData);
        rvBlueDevices.setAdapter(bluetoothDeviceAdapter);

        //设置分割线
        rvBlueDevices.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        //设置item点击事件
        bluetoothDeviceAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                BluetoothDeviceModel item = (BluetoothDeviceModel) adapter.getItem(position);
                //确保点击的蓝牙设备是RFID标签读写器，然后才能进行下一步(读写器的型号不同这里的筛选规则也不同，视具体情况改写这里的逻辑)
                if (item == null || item.getDeviceName() == null || item.getDeviceName().equals("")) {
                    Toast.makeText(BluetoothDeviceListActivity.this, "该设备不是RFID标签读写器设备，请重新选择", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!item.getDeviceName().substring(0, 1).equals("U")) {
                    Toast.makeText(BluetoothDeviceListActivity.this, "该设备不是RFID标签读写器设备，请重新选择", Toast.LENGTH_SHORT).show();
                    return;
                }

                //连接蓝牙设备
                RFIDReaderUtil.connectRFIDReader(item.getDeviceMac());
                setSensingDistance();

            }
        });

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        initBle();
    }

    /**
     * 设置感应距离
     */
    private void setSensingDistance() {
        View selectView = LayoutInflater.from(this).inflate(R.layout.view_sensing_distance, null);
        RadioGroup radio = selectView.findViewById(R.id.radio);
        radio.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radioButton_one) {
                    distance = READ_CARD_INSTANCE_TYPE_LOW;
                } else if (checkedId == R.id.radioButton_two) {
                    distance = READ_CARD_INSTANCE_TYPE_MIDDLE;
                } else if (checkedId == R.id.radioButton_three) {
                    distance = READ_CARD_INSTANCE_TYPE_HIGH;
                } else if (checkedId == R.id.radioButton_four) {
                    distance = READ_CARD_INSTANCE_TYPE_VERY_HIGH;
                }
            }
        });
        selectView.findViewById(R.id.sure).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RFIDReaderUtil.setReadCardInstance(distance);
                Toast.makeText(BluetoothDeviceListActivity.this, "设置感应距离成功", Toast.LENGTH_SHORT).show();
                BluetoothDeviceListActivity.this.finish();
            }
        });
        new DialogUtil(new WeakReference<Activity>(this))
                .setContent(selectView).setContentShowType(CONTENT_TYPE_VIEW)
                .setCanceledOnTouchOutside(false).show();

    }

    private void initBle() {
        //判断设备硬件是否有蓝牙功能模块
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "该设备没有蓝牙功能模块", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //判断设备是否支持蓝牙
        if (!isSupportBluetooth()) {
            Toast.makeText(this, "该设备不支持蓝牙功能", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //判断设备蓝牙是否开启
        if (!isOpenBluetooth()) {
            //异步自动开启蓝牙
            openBluetoothAsync();
        }
        checkPermissions();
    }

    private boolean isSupportBluetooth() {
        return mBluetoothAdapter != null;
    }

    private boolean isOpenBluetooth() {
        return mBluetoothAdapter.isEnabled();
    }

    private void openBluetoothAsync() {
        mBluetoothAdapter.enable();
    }

    /**
     * 检查权限
     */
    private void checkPermissions() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
        List<String> permissionDeniedList = new ArrayList<>();
        for (String permission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted(permission);
            } else {
                permissionDeniedList.add(permission);
            }
        }
        if (!permissionDeniedList.isEmpty()) {
            String[] deniedPermissions = permissionDeniedList.toArray(new String[permissionDeniedList.size()]);
            ActivityCompat.requestPermissions(this, deniedPermissions, REQUEST_CODE_PERMISSION_LOCATION);
        }
    }

    @Override
    public final void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_LOCATION:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            onPermissionGranted(permissions[i]);
                        }
                    }
                }
                break;
            case REQUEST_CODE_OPEN_GPS:
                if (grantResults.length > 0) {
                    for (int grantResult : grantResults) {
                        if (grantResult == PackageManager.PERMISSION_GRANTED) {
                            scanLeDevice(true);
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    private void onPermissionGranted(String permission) {
        switch (permission) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkGPSIsOpen()) {

                    DialogUtil dialogUtil = new DialogUtil(new WeakReference<Activity>(this));
                    dialogUtil.setTop("提示").setContent("当前手机扫描蓝牙需要打开定位功能").setLeftBtnClickListener(new DialogBtnClickListener() {
                        @Override
                        public void btnClick() {
                            finish();
                        }
                    }).setRightBtn("前往设置").setRightDialogBtnClickListener(new DialogBtnClickListener() {
                        @Override
                        public void btnClick() {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(intent, REQUEST_CODE_OPEN_GPS);
                        }
                    }).setCancelable(false).show();
                } else {
                    //参数true为开始扫描蓝牙设备；false为停止扫描
                    scanLeDevice(true);
                }
                break;
        }
    }

    private boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return false;
        }
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            //15秒后停止搜索
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);
            isScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback); //开始搜索
        } else {
            isScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);//停止搜索
        }
    }


    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            //这里是个子线程，下面把它转换成主线程处理
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    getBluetoothDeviceData();

                    //在这里可以把搜索到的设备保存起来
                    //device.getName();获取蓝牙设备名字
                    //device.getAddress();获取蓝牙设备mac地址
                    //rssi是信号强度，即手机与设备之间的信号强度。信号强度转设备与手机距离的公式：d = 10^((abs(rssi) - A) / (10 * n))
                }

                //过滤掉相同MAC地址的蓝牙设备，然后将这些设备添加到蓝牙设备数据列表，并刷新显示
                private void getBluetoothDeviceData() {
                    if (blueDeviceData.size() == 0) {
                        addData();
                    } else {
                        boolean isHaveSameMac = false;
                        for (int i = 0; i < blueDeviceData.size(); i++) {
                            if (device.getAddress().equals(blueDeviceData.get(i).getDeviceMac())) {
                                isHaveSameMac = true;
                            }
                        }
                        if (!isHaveSameMac) {
                            addData();
                        }
                    }
                }

                private void addData() {
                    BluetoothDeviceModel bluetoothDeviceModel = new BluetoothDeviceModel();
                    bluetoothDeviceModel.setDeviceName(device.getName() == null ? "" : device.getName());
                    bluetoothDeviceModel.setDeviceMac(device.getAddress());
                    blueDeviceData.add(bluetoothDeviceModel);
                    bluetoothDeviceAdapter.notifyDataSetChanged();
                }
            });
        }

    };

}
