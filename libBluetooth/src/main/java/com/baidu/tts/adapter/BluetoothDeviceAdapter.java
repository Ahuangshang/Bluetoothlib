package com.baidu.tts.adapter;


import androidx.annotation.Nullable;

import com.baidu.tts.R;
import com.baidu.tts.entry.BluetoothDeviceModel;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class BluetoothDeviceAdapter extends BaseQuickAdapter<BluetoothDeviceModel, BaseViewHolder> {

    public BluetoothDeviceAdapter(int layoutResId, @Nullable List<BluetoothDeviceModel> data) {
        super(layoutResId, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, BluetoothDeviceModel item) {
        helper.setText(R.id.tv_blue_device_name, "设备名称：" + item.getDeviceName())
                .setText(R.id.tv_blue_device_mac, "设备MAC：" + item.getDeviceMac());
    }
}
