package com.luoye.bluetoothsimple;

import android.app.Dialog;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.luoye.bluetooth.Bluetooth;

import java.util.ArrayList;

/**
 * Created by LUOYE on 2017/2/26.
 */

public class DiscoverDialog extends Dialog implements AdapterView.OnItemClickListener {
    /** 蓝牙设备列表 */
    ListView mLivDevices;
    /** 蓝牙设备 */
    Bluetooth mBluetooth;
    /** 蓝牙设备列表适配器 */
    DeviceAdapter mAdapter;
    /** 蓝牙设备选中事件监听接口 */
    OnBluetoothDeviceClickListener mOnBluetoothDeviceClickListener;

    public DiscoverDialog(Context context, Bluetooth bluetooth) {
        super(context);
        mBluetooth = bluetooth;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLivDevices = (ListView) LayoutInflater.from(getContext()).inflate(R.layout.dialog_discover, null);
        setContentView(mLivDevices);
        mAdapter = new DeviceAdapter(getContext(), null);
        mLivDevices.setAdapter(mAdapter);
        mLivDevices.setOnItemClickListener(this);
    }

    public void setOnBluetoothDeviceClickListener(OnBluetoothDeviceClickListener l) {
        mOnBluetoothDeviceClickListener = l;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAdapter.clear();
        ArrayList<Device> devices = new ArrayList<>();
        for (BluetoothDevice device : mBluetooth.getBoundedDevices()) {
            devices.add(new Device(device, System.currentTimeMillis()));
        }
        mAdapter.add(devices);

        mBluetooth.scanDevices(new Bluetooth.OnBluetoothDiscoveryListener() {
            @Override
            public void onDiscoveryStarted() {
                Toast.makeText(getContext(), "开始扫描周边设备！", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeviceFound(BluetoothDevice device, BluetoothClass cls, String name, int rssi) {
                mAdapter.add(new Device(device, System.currentTimeMillis()));
            }

            @Override
            public void onDiscoveryFinished() {
                Toast.makeText(getContext(), "扫描周边设备完成！", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDiscoveryCanceled() {
                Toast.makeText(getContext(), "扫描周边设备中止！", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        mBluetooth.stopScan();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Device device = (Device) mAdapter.getItem(position);
        dismiss();
        if (null != mOnBluetoothDeviceClickListener)
            mOnBluetoothDeviceClickListener.onBluetoothClick(device.device);
    }

    interface OnBluetoothDeviceClickListener {
        void onBluetoothClick(BluetoothDevice device);
    }
}
