package com.luoye.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * 蓝牙通信设备，用来设置蓝牙模块和获取蓝牙通信端。
 * Created by LUOYE on 2016/8/10.
 */
public class Bluetooth {
    /** 蓝牙安全通信UUID */
    public static final String UUID_SECURE = "fa87c0d0-afac-11de-8a39-0800200c9a66";
    /** 蓝牙非安全通信UUID */
    public static final String UUID_INSECURE = "8ce255c0-200a-11e0-ac64-0800200c9a66";

    /** 上下文 */
    private Context mContext;
    /** 蓝牙适配器 */
    private BluetoothAdapter mBluetoothAdapter;
    /** 蓝牙搜索广播接收器 */
    private BluetoothDiscoveryBroadcastReceiver mReceiver;
    /** 蓝牙搜索监听接口 */
    private OnBluetoothDiscoveryListener mOnBluetoothDiscoveryListener;

    /**
     * 默认构造
     * @throws IllegalArgumentException 上下文为空或当前设备不支持蓝牙模块时抛出
     */
    public Bluetooth(Context context) {
        if (null == context) {
            throw new IllegalArgumentException("this context can't be null.");
        }
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (null == mBluetoothAdapter) {
            throw new IllegalArgumentException("Bluetooth is not supported on this hardware platform.");
        }
    }

    /**
     * 获取蓝牙适配器
     * @return 蓝牙适配器
     */
    public BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    /**
     * 直接开启蓝牙模块
     * @return true 成功开启
     */
    public boolean enable() {
        return mBluetoothAdapter.enable();
    }

    /**
     * 通知用户开启蓝牙模块
     * @param activity 当前页面组件
     * @param requestCode 请求码
     */
    public void enable(Activity activity, int requestCode) {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 蓝牙模块是否已开启
     * @return true 蓝牙模块已开启
     */
    public boolean isEnabled() {
        return mBluetoothAdapter.isEnabled();
    }

    /**
     * 直接关闭蓝牙模块
     * @return true 成功关闭
     */
    public boolean disable() {
        return mBluetoothAdapter.disable();
    }

    /**
     * 使蓝牙设备可搜可见
     * @param activity 界面组件
     * @param requestCode 请求码
     * @param duration 可见时长，最大不超过300s
     */
    public void discoverable(Activity activity, int requestCode, int duration) {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 是否正在扫描周边蓝牙设备
     * @return true 正在扫描
     */
    public boolean isDiscovering() {
        return mBluetoothAdapter.isDiscovering();
    }

    /**
     * 获取已配对过的蓝牙设备列表
     * @return 已配对过的蓝牙设备列表
     */
    public Set<BluetoothDevice> getBoundedDevices() {
        return mBluetoothAdapter.getBondedDevices();
    }

    /**
     * 扫描附近的设备，并根据监听接口回调来监听扫描结果。
     * <p>若监听接口为空，需要自己注册广播接收事件</p>
     * @param l 蓝牙扫描监听接口
     * @return true 开始扫描附近的设备，false 蓝牙设备未打开或扫描周边设备已开启
     */
    public boolean scanDevices(OnBluetoothDiscoveryListener l) {
        if (isDiscovering())
            return false;

        if (null != l) {
            mOnBluetoothDiscoveryListener = l;
            mReceiver = new BluetoothDiscoveryBroadcastReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            mContext.registerReceiver(mReceiver, filter);
        }
        return mBluetoothAdapter.startDiscovery();
    }

    /**
     * 停止扫描
     * @return false 蓝牙设备未开启
     */
    public boolean stopScan() {
        if (null != mOnBluetoothDiscoveryListener) {
            mOnBluetoothDiscoveryListener.onDiscoveryCanceled();
            mOnBluetoothDiscoveryListener = null;
        }
        if (null != mReceiver) {
            mContext.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        return mBluetoothAdapter.cancelDiscovery();
    }

    /**
     * 新建蓝牙通信终端
     * @param device 蓝牙设备
     * @param uuid 口令
     * @param secure 是否是安全通信
     * @return 蓝牙通信终端
     * @throws IOException 蓝牙模块不可用或无权限
     */
    public BluetoothClient newBluetoothClient(BluetoothDevice device, String uuid, boolean secure) throws IOException {
        return newBluetoothClient(device, formUUID(uuid), secure);
    }

    /**
     * 新建蓝牙通信终端
     * @param device 蓝牙设备
     * @param uuid 口令
     * @param secure 是否是安全通信
     * @return 蓝牙通信终端
     * @throws IOException 蓝牙模块不可用或无权限
     */
    public BluetoothClient newBluetoothClient(BluetoothDevice device, UUID uuid, boolean secure) throws IOException {
        return new BluetoothClient(device, uuid, secure);
    }

    /**
     * 新建蓝牙通信服务端
     * @param name
     * @param uuid 口令
     * @param secure 是否是安全通信
     * @return 蓝牙通信服务端
     * @throws IOException 蓝牙模块不可用或无权限
     */
    public BluetoothServer newBluetoothServer(String name, String uuid, boolean secure) throws IOException {
        return newBluetoothServer(name, formUUID(uuid), secure);
    }

    /**
     * 新建蓝牙通信服务端
     * @param name
     * @param uuid 口令
     * @param secure 是否是安全通信
     * @return 蓝牙通信服务端
     * @throws IOException 蓝牙模块不可用或无权限
     */
    public BluetoothServer newBluetoothServer(String name, UUID uuid, boolean secure) throws IOException {
        return new BluetoothServer(mBluetoothAdapter, name, uuid, secure);
    }

    /**
     * 创建UUID
     * @param uuid UUID字符串
     * @return null 字符串为空
     */
    private UUID formUUID(String uuid) {
        return null == uuid ? null : UUID.fromString(uuid);
    }

    /**
     * 蓝牙搜索广播接收器
     */
    private class BluetoothDiscoveryBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND == action) {
                mOnBluetoothDiscoveryListener.onDeviceFound((BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE),
                        (BluetoothClass) intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS),
                        intent.getStringExtra(BluetoothDevice.EXTRA_NAME),
                        intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0));
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED == action) {
                mOnBluetoothDiscoveryListener.onDiscoveryStarted();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                mOnBluetoothDiscoveryListener.onDiscoveryFinished();
                mOnBluetoothDiscoveryListener = null;
                mContext.unregisterReceiver(mReceiver);
                mReceiver = null;
            }
        }
    }

    /**
     * 蓝牙搜索监听接口
     */
    public static interface OnBluetoothDiscoveryListener {
        /**
         * 蓝牙搜索开始触发事件
         */
        public void onDiscoveryStarted();

        /**
         * 搜索到蓝牙设备
         * @param device 蓝牙设备
         * @param cls 蓝牙硬件类型
         * @param name 蓝牙设备名称
         * @param rssi 蓝牙设备RSSI
         */
        public void onDeviceFound(BluetoothDevice device, BluetoothClass cls, String name, int rssi);

        /**
         * 蓝牙搜索完成触发事件
         */
        public void onDiscoveryFinished();

        /**
         * 蓝牙搜索取消触发事件
         */
        public void onDiscoveryCanceled();
    }
}
