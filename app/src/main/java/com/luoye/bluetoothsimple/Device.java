package com.luoye.bluetoothsimple;

import android.bluetooth.BluetoothDevice;

/**
 * Created by LUOYE on 2017/2/26.
 */

public class Device {
    public BluetoothDevice device;
    public long time;

    public Device(BluetoothDevice device, long time) {
        this.device = device;
        this.time = time;
    }
}
