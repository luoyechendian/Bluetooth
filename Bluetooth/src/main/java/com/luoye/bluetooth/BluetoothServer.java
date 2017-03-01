package com.luoye.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.IOException;
import java.util.UUID;

/**
 * 蓝牙通信服务端，等待其他蓝牙设备接入，并完成蓝牙通信收发。
 * Created by LUOYE on 2016/8/11.
 */
public class BluetoothServer {
    /** 蓝牙服务端状态：未连接 */
    public static final int BLUETOOTH_SERVER_STATUS_DISCONNECT = 0;
    /** 蓝牙服务端状态：接受中... */
    public static final int BLUETOOTH_SERVER_STATUS_ACCEPTING = 1;
    /** 蓝牙服务端状态：已关闭 */
    public static final int BLUETOOTH_SERVER_STATUS_CLOSED = 2;

    /** 接收套接字 */
    private BluetoothServerSocket mServerSocket;
    /** 蓝牙服务端状态 */
    private int mStatus = BLUETOOTH_SERVER_STATUS_DISCONNECT;

    /**
     * 默认构造函数
     * @param adapter 蓝牙适配器
     * @param name
     * @param uuid 口令
     * @param secure 是否是安全通信
     * @throws IOException 构造失败
     */
    BluetoothServer(BluetoothAdapter adapter, String name, UUID uuid, boolean secure) throws IOException {
        if (secure) {
            mServerSocket = adapter.listenUsingRfcommWithServiceRecord(name, uuid);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
                mServerSocket = adapter.listenUsingInsecureRfcommWithServiceRecord(name, uuid);
            } else {
                mServerSocket = adapter.listenUsingRfcommWithServiceRecord(name, uuid);
            }
        }
    }

    /**
     * 获取蓝牙服务端状态。
     * @return 蓝牙服务端当前状态
     * <ul>
     *     <li>{@link #BLUETOOTH_SERVER_STATUS_DISCONNECT}</li>
     *     <li>{@link #BLUETOOTH_SERVER_STATUS_ACCEPTING}</li>
     *     <li>{@link #BLUETOOTH_SERVER_STATUS_CLOSED}</li>
     * </ul>
     */
    public int status() {
        return mStatus;
    }

    /**
     * 接受请求。此方法将会阻塞当前线程直至有连接请求接入或蓝牙服务端已关闭。
     * @return 蓝牙通信终端
     * @throws IOException 蓝牙服务端已关闭或接受已超时
     */
    public BluetoothClient accept() throws IOException {
        return accept(-1);
    }

    /**
     * 接受请求。此方法将会阻塞当前线程直至有连接请求接入或蓝牙服务端已关闭。
     * @param timeout 超时阈值
     * @return 蓝牙通信终端
     * @throws IOException 蓝牙服务端已关闭或接受已超时
     */
    public BluetoothClient accept(int timeout) throws IOException {
        if (mStatus > BLUETOOTH_SERVER_STATUS_DISCONNECT)
            throw new IOException("Bluetooth server is accepting or shutdown.");
        mStatus = BLUETOOTH_SERVER_STATUS_ACCEPTING;
        BluetoothClient client = null;
        try {
            BluetoothSocket socket = mServerSocket.accept(timeout);
            client = new BluetoothClient(socket);
        } catch (IOException e) {
            mStatus = BLUETOOTH_SERVER_STATUS_DISCONNECT;
            throw e;
        }
        mStatus = BLUETOOTH_SERVER_STATUS_DISCONNECT;
        return client;
    }

    /**
     * 异步接受请求。
     * <p>该方法会立即返回，不会阻塞当前线程，当获取到连接请求时回调{@link OnBluetoothAcceptCallback#onAcceptSuccess(BluetoothClient)}方法，
     * 接受请求失败时回调{@link OnBluetoothAcceptCallback#onAcceptFailure(IOException)}方法。</p>
     * @param callback 异步接受请求监听回调
     * @throws IOException 蓝牙服务端正在接受请求或已关闭
     * @throws IllegalArgumentException 监听回调为null
     */
    public void asyncAccept(OnBluetoothAcceptCallback callback) throws IOException {
        asyncAccept(-1, callback);
    }

    /**
     * 异步接受请求。
     * <p>该方法会立即返回，不会阻塞当前线程，当获取到连接请求时回调{@link OnBluetoothAcceptCallback#onAcceptSuccess(BluetoothClient)}方法，
     * 接受请求失败时回调{@link OnBluetoothAcceptCallback#onAcceptFailure(IOException)}方法。</p>
     * @param timeout 超时阈值
     * @param callback 异步接受请求监听回调
     * @throws IOException 蓝牙服务端正在接受请求或已关闭
     * @throws IllegalArgumentException 监听回调为null
     */
    public void asyncAccept(final int timeout, final OnBluetoothAcceptCallback callback) throws IOException {
        if (mStatus > BLUETOOTH_SERVER_STATUS_DISCONNECT)
            throw new IOException("Bluetooth server is accepting or shutdown.");
        if (null == callback)
            throw new IllegalArgumentException("the OnBluetoothAcceptCallback can't be null.");
        mStatus = BLUETOOTH_SERVER_STATUS_ACCEPTING;
        new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        BluetoothSocket socket = mServerSocket.accept(timeout);
                        callback.notifyCallSuccess(new BluetoothClient(socket));
                    }
                } catch (IOException e) {
                    if (mStatus < BLUETOOTH_SERVER_STATUS_CLOSED)
                        mStatus = BLUETOOTH_SERVER_STATUS_DISCONNECT;
                    callback.notifyCallFailure(e);
                }
            }
        }.start();
    }

    /**
     * 关闭蓝牙服务端
     * @throws IOException 异常信息
     */
    public void shutdown() throws IOException {
        mStatus = BLUETOOTH_SERVER_STATUS_CLOSED;
        mServerSocket.close();
    }

    /**
     * 蓝牙连接接收回调接口
     * @author yanl.li
     *
     */
    public static abstract class OnBluetoothAcceptCallback {
        /** 消息句柄 */
        private BluetoothServerHandler mHandler;

        /**
         * 默认构造
         */
        public OnBluetoothAcceptCallback() {
            mHandler = new BluetoothServerHandler(Looper.getMainLooper());
        }

        class BluetoothServerHandler extends Handler {
            /** 蓝牙接受请求失败消息 */
            static final int MESSAGE_BLUETOOTH_ACCEPT_FAILURE = 1;
            /** 蓝牙接受请求成功消息 */
            static final int MESSAGE_BLUETOOTH_ACCEPT_SUCCESS = 2;

            BluetoothServerHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_BLUETOOTH_ACCEPT_SUCCESS:
                        onAcceptSuccess((BluetoothClient) msg.obj);
                        break;
                    case MESSAGE_BLUETOOTH_ACCEPT_FAILURE:
                        onAcceptFailure((IOException) msg.obj);
                        break;
                }
            }
        }

        /**
         * 通知回调获取到连接请求回调方法
         * @param client 蓝牙通信终端
         */
        void notifyCallSuccess(BluetoothClient client) {
            mHandler.obtainMessage(BluetoothServerHandler.MESSAGE_BLUETOOTH_ACCEPT_SUCCESS, client).sendToTarget();
        }

        /**
         * 通知回调接受请求失败回调方法
         * @param e 异常信息
         */
        void notifyCallFailure(IOException e) {
            mHandler.obtainMessage(BluetoothServerHandler.MESSAGE_BLUETOOTH_ACCEPT_FAILURE, e).sendToTarget();
        }

        /**
         * 获取到连接请求
         * @param client 蓝牙通信终端
         */
        public abstract void onAcceptSuccess(BluetoothClient client);

        /**
         * 接受请求失败
         * @param e 异常信息
         */
        public abstract void onAcceptFailure(IOException e);
    }
}
