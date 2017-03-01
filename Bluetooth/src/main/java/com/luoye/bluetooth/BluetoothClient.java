package com.luoye.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 蓝牙通信终端，负责主动连接其他指定的蓝牙设备，并完成蓝牙通信数据的收发。
 * Created by LUOYE on 2016/8/11.
 */
public class BluetoothClient {
    /** 蓝牙终端状态：未连接 */
    public static final int BLUETOOTH_CLIENT_STATUS_DISCONNECT = 0;
    /** 蓝牙终端状态：连接中... */
    public static final int BLUETOOTH_CLIENT_STATUS_CONNECTING = 1;
    /** 蓝牙终端状态：已连接 */
    public static final int BLUETOOTH_CLIENT_STATUS_CONNECTED = 2;
    /** 蓝牙终端状态：已关闭 */
    public static final int BLUETOOTH_CLIENT_STATUS_CLOSED = 3;

    /** 蓝牙终端套接字 */
    private BluetoothSocket mSocket;
    /** 蓝牙终端状态 */
    private int mStatus = BLUETOOTH_CLIENT_STATUS_DISCONNECT;
    /** 蓝牙终端读取线程 */
    private ReadThread mReadThread;
    /** 蓝牙异步读取监听接口 */
    private BluetoothInputCallback mBluetoothInputCallback;

    /**
     * 默认构造函数，包可见
     * @param device 蓝牙设备
     * @param uuid 口令
     * @param secure 是否是安全通信
     * @throws IOException 构造失败
     */
    BluetoothClient(BluetoothDevice device, UUID uuid, boolean secure) throws IOException {
        if (secure) {
            mSocket = device.createRfcommSocketToServiceRecord(uuid);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
                mSocket = device.createInsecureRfcommSocketToServiceRecord(uuid);
            } else {
                mSocket = device.createRfcommSocketToServiceRecord(uuid);
            }
        }
    }

    /**
     * 构造函数，当蓝牙服务端接受连接请求后，通过此构造方法创建通信端。
     * @param socket 蓝牙终端套接字
     */
    BluetoothClient(BluetoothSocket socket) {
        mSocket = socket;
        mStatus = BLUETOOTH_CLIENT_STATUS_CONNECTED;
    }

    /**
     * 获取蓝牙终端状态
     * <ul>
     *     <li>{@link #BLUETOOTH_CLIENT_STATUS_DISCONNECT}</li>
     *     <li>{@link #BLUETOOTH_CLIENT_STATUS_CONNECTING}</li>
     *     <li>{@link #BLUETOOTH_CLIENT_STATUS_CONNECTED}</li>
     *     <li>{@link #BLUETOOTH_CLIENT_STATUS_CLOSED}</li>
     * </ul>
     * @return
     */
    public int status() {
        return mStatus;
    }

    /**
     * 连接蓝牙服务端。
     * <p>该方法会阻塞当前线程直至生成一个连接或连接失败。若当前方法返回时没有抛出任何异常，那么连接成功。</p>
     * <p>可以在其他线程中调用{@link #shutdown}方法中止当前连接请求。</p>
     * @throws IOException 蓝牙已连接或蓝牙终端已关闭
     */
    public void connect() throws IOException {
        if (mStatus > BLUETOOTH_CLIENT_STATUS_DISCONNECT)
            throw new IOException("Bluetooth Client is connecting or connected.");

        mStatus = BLUETOOTH_CLIENT_STATUS_CONNECTING;
        try {
            mSocket.connect();
        } catch (IOException e) {
            mStatus = BLUETOOTH_CLIENT_STATUS_DISCONNECT;
            throw e;
        }
        mStatus = BLUETOOTH_CLIENT_STATUS_CONNECTED;
    }

    /**
     * 异步连接蓝牙服务端。
     * <p>该方法会立即返回，不会阻塞当前线程，当连接成功回调{@link OnBluetoothConnectCallback#onConnectSuccess(BluetoothClient)}方法，
     * 连接失败时回调{@link OnBluetoothConnectCallback#onConnectFailure(Exception)}方法。</p>
     * @param callback 异步连接监听回调
     * @throws IOException 蓝牙已连接
     */
    public void asyncConnect(final OnBluetoothConnectCallback callback) throws IOException {
        if (mStatus > BLUETOOTH_CLIENT_STATUS_DISCONNECT)
            throw new IOException("Bluetooth client is connecting or connected.");

        if (null == callback) {
            throw new IllegalArgumentException("the OnBluetoothConnectCallback can't be null.");
        }

        mStatus = BLUETOOTH_CLIENT_STATUS_CONNECTING;
        new Thread() { // 开启连接线程等待建立连接
            @Override
            public void run() {
                boolean bSuccess = true;
                try {
                    if (mSocket.getRemoteDevice().getBondState() == BluetoothDevice.BOND_NONE) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            mSocket.getRemoteDevice().createBond();
                        } else {
                            Method method = BluetoothDevice.class.getMethod("createBound");
                            method.invoke(mSocket.getRemoteDevice());
                        }
                    }
                    mSocket.connect();
                } catch (Exception e) {
                    bSuccess = false;
                    if (mStatus < BLUETOOTH_CLIENT_STATUS_CLOSED)
                        mStatus = BLUETOOTH_CLIENT_STATUS_DISCONNECT;
                    callback.notifyCallFailure(e);
                }

                if (bSuccess) {
                    mStatus = BLUETOOTH_CLIENT_STATUS_CONNECTED;
                    callback.notifyCallSuccess(BluetoothClient.this);
                }
            }
        }.start();
    }

//    /**
//     * 获取蓝牙输入流。<p>就算蓝牙未连接，依然可以返回输入流，但同时会返回IO异常，直至蓝牙连接成功。</p>
//     * @return 蓝牙输入流
//     * @throws IOException 蓝牙未连接时抛出
//     */
//    public InputStream getInputStream() throws IOException {
//        return mSocket.getInputStream();
//    }
//
//    /**
//     * 获取蓝牙输出流。<p>就算蓝牙未连接，依然可以返回输出流，但同时会返回IO异常，直至蓝牙连接成功。</p>
//     * @return 蓝牙输出流
//     * @throws IOException 蓝牙未连接时抛出
//     */
//    public OutputStream getOutputStream() throws IOException {
//        return mSocket.getOutputStream();
//    }

//    /**
//     * 读取蓝牙输入流并拷贝至缓存。<p>从输入流中最多读取{@code length}长度的字节并从{@code buffer}的{@code offset}
//     * 位置开始拷贝给buffer。</p>
//     * <p>此方法会阻塞当前线程。</p>
//     * @param buffer 字节缓存
//     * @param offset buffer缓存的起始位置
//     * @param length 期望读取的字节数
//     * @return 最终读取的字节数，若为-1则可能输入流已关闭或蓝牙连接已断开
//     * @throws IndexOutOfBoundsException 字节缓存操作越界
//     * @throws IOException 输入流已关闭或其他IO异常
//     */
//    public int read(byte[] buffer, int offset, int length) throws IOException {
//        return mSocket.getInputStream().read(buffer, offset, length);
//    }
//
//    /**
//     * 读取蓝牙输入流并拷贝至缓存。
//     * <p>此方法会阻塞当前线程。</p>
//     * @param buffer 字节缓存
//     * @return 最终读取的字节数，若为-1则可能输入流已关闭或蓝牙连接已断开
//     * @throws IndexOutOfBoundsException 字节缓存操作越界
//     * @throws IOException 输入流已关闭或其他IO异常
//     */
//    public int read(byte[] buffer) throws IOException {
//        return mSocket.getInputStream().read(buffer);
//    }

    /**
     * 将字节缓存{@code buffer}写入蓝牙输出流。
     * @param buffer 字节缓存
     * @throws IOException 蓝牙输出流异常
     */
    public void write(byte[] buffer) throws IOException {
        mSocket.getOutputStream().write(buffer);
    }

    /**
     * 将字节缓存{@code buffer}从指定起始位置{@code offset}开始，写入{@code count}字节到蓝牙输出流。
     * @param buffer 字节缓存
     * @param offset buffer缓存的起始位置
     * @param count 期望写入的字节数
     * @throws IOException 蓝牙输出流异常
     * @throws IndexOutOfBoundsException 字节缓存操作越界
     */
    public void write(byte[] buffer, int offset, int count) throws IOException {
        mSocket.getOutputStream().write(buffer, offset, count);
    }

//    /**
//     * 写入字节缓存并刷新蓝牙输出流
//     * @param buffer 字节缓存
//     * @throws IOException
//     */
//    public void writeAndFlush(byte[] buffer) throws IOException {
//        OutputStream os = mSocket.getOutputStream();
//        os.write(buffer);
//        os.flush();
//    }
//
//    public void writeAndFlush(byte[] buffer, int offset, int count) throws IOException {
//        OutputStream os = mSocket.getOutputStream();
//        os.write(buffer, offset, count);
//        os.flush();
//    }


    /**
     * 异步读取蓝牙数据
     * @param callback 监听回调
     */
    public void asyncRead(final BluetoothInputCallback callback) throws IOException {
        if (null == callback)
            return ;
        if (null != mBluetoothInputCallback)
            throw new IOException("asyncRead again,please call cancelAsyncRead before call.");

        // 判别当前蓝牙是否已连接
        if (BLUETOOTH_CLIENT_STATUS_CONNECTED != mStatus)
            return;

        mBluetoothInputCallback = callback;

        if (null == mReadThread || mReadThread.getState() == Thread.State.TERMINATED) {
            mReadThread = new ReadThread();
            mReadThread.start();
        } else if (mReadThread.getState() == Thread.State.WAITING) {
            mReadThread.notify();
        }
    }

    /**
     * 取消异步监听.
     */
    public void cancelAsyncRead() {
        if (null != mBluetoothInputCallback) {
            synchronized (mBluetoothInputCallback) {
                mBluetoothInputCallback = null;
            }
        }
    }

    /**
     * 蓝牙是否已连接
     * @return true 蓝牙已连接
     */
    public boolean isConnected() {
        return mSocket.isConnected() && mStatus == BLUETOOTH_CLIENT_STATUS_CONNECTED;
    }

    /**
     * 获取远程蓝牙设备
     * @return 蓝牙设备
     */
    public BluetoothDevice getRemoteDevice() {
        return mSocket.getRemoteDevice();
    }

    /**
     * 关闭蓝牙客户端
     * @throws IOException
     */
    public void shutdown() throws IOException {
        mStatus = BLUETOOTH_CLIENT_STATUS_CLOSED;
        mSocket.close();
    }

    /**
     * 接收线程
     */
    class ReadThread extends Thread {
        @Override
        public void run() {
            try {
                InputStream is = mSocket.getInputStream();
                byte[] buffer = new byte[1024];
                int nSize = 0;
                boolean bWait = false;
                while ((nSize = is.read(buffer)) != -1) {
                    byte[] newBuffer = new byte[nSize];
                    System.arraycopy(buffer, 0, newBuffer, 0, nSize);
                    if (null != mBluetoothInputCallback) {
                        synchronized (mBluetoothInputCallback) {
                            mBluetoothInputCallback.notifyInput(newBuffer);
                        }
                    } else {
                        bWait = true;
                    }

                    if (bWait) {
                        try {
                            bWait = false;
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break; // 此处会导致数据丢失
                        }

                        // 将上次未发送的数据发送出去
                        if (null != mBluetoothInputCallback)
                            mBluetoothInputCallback.notifyInput(buffer);
                    } else {
                        try {
                            sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                // 蓝牙断开时不会返回-1，而是直接抛出异常
//                //若返回-1，当前终端与服务端断开连接
//                if (mStatus < BLUETOOTH_CLIENT_STATUS_CLOSED) {
//                    mStatus = BLUETOOTH_CLIENT_STATUS_DISCONNECT;
//                    if (mBluetoothInputCallback != null)
//                        mBluetoothInputCallback.notifyDisconnected(new IOException("bluetooth is disconnected."));
//                }
            } catch (IOException e) {
                if (mStatus < BLUETOOTH_CLIENT_STATUS_CLOSED) {
                    mStatus = BLUETOOTH_CLIENT_STATUS_DISCONNECT;
                    if (mBluetoothInputCallback != null)
                        mBluetoothInputCallback.notifyDisconnected(e);
                }
            }
        }
    }

    /**
     * 蓝牙终端连接监听回调
     */
    public static abstract class OnBluetoothConnectCallback {
        /** 消息句柄 */
        private BluetoothClientHandler mHandler;

        /**
         * 默认构造
         */
        public OnBluetoothConnectCallback() {
            mHandler = new BluetoothClientHandler(Looper.getMainLooper());
        }

        /**
         * 蓝牙终端连接线程消息句柄
         */
        class BluetoothClientHandler extends  Handler {
            /** 蓝牙连接失败消息 */
            static final int MESSAGE_BLUETOOTH_CONNECT_FAILURE = 1;
            /** 蓝牙连接成功消息 */
            static final int MESSAGE_BLUETOOTH_CONNECT_SUCCESS = 2;

            BluetoothClientHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_BLUETOOTH_CONNECT_SUCCESS:
                        onConnectSuccess((BluetoothClient) msg.obj);
                        break;
                    case MESSAGE_BLUETOOTH_CONNECT_FAILURE:
                        onConnectFailure(((Exception) msg.obj));
                        break;
                }
            }
        }

        /**
         * 通知回调连接成功方法
         * @param client 蓝牙终端
         */
        void notifyCallSuccess(BluetoothClient client) {
            mHandler.sendMessage(mHandler.obtainMessage(BluetoothClientHandler.MESSAGE_BLUETOOTH_CONNECT_SUCCESS, client));
        }

        /**
         * 通知回调连接失败方法
         * @param e 异常信息
         */
        void notifyCallFailure(Exception e) {
            mHandler.sendMessage(mHandler.obtainMessage(BluetoothClientHandler.MESSAGE_BLUETOOTH_CONNECT_FAILURE, e));
        }

        /**
         * 连接重试
         * @param time 重试次数
         * @param timeout 时间阈值
         * @param e 连接失败异常
         * @return true 请求重试，false 退出重试
         */
//    boolean onRetryConnect(int time, int timeout, IOException e);

        /**
         * 连接超时
         * @param timeout 时间阈值
         */
//        public abstract void onConnectTimeOut(long timeout);

        /**
         * 蓝牙连接成功
         * @param client 蓝牙终端
         */
        public abstract void onConnectSuccess(BluetoothClient client);

        /**
         * 蓝牙连接失败
         * @param e 配对或连接异常信息
         */
        public abstract void onConnectFailure(Exception e);
    }

    /**
     * 蓝牙终端数据接收监听回调
     */
    public static abstract class BluetoothInputCallback {
        /** 蓝牙终端数据接收线程消息句柄 */
        private BluetoothInputHandler mHandler;

        /**
         * 默认构造
         */
        public BluetoothInputCallback() {
            mHandler = new BluetoothInputHandler(Looper.getMainLooper());
        }

        class BluetoothInputHandler extends Handler {
            /** 数据接收消息 */
            static final int MESSAGE_BLUETOOTH_INPUT_ONINPUT = 1;
            /** 连接断开消息 */
            static final int MESSAGE_BLUETOOTH_DISCONNECTED = 2;
//            /** 数据接收异常消息 */
//            static final int MESSAGE_BLUETOOTH_INPUT_EXCEPTION = 3;

            /**
             * 默认构造
             * @param looper
             */
            BluetoothInputHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_BLUETOOTH_INPUT_ONINPUT:
                        onInput((byte[]) msg.obj);
                        break;
                    case MESSAGE_BLUETOOTH_DISCONNECTED:
                        onDisconnected((IOException) msg.obj);
                        break;
//                    case MESSAGE_BLUETOOTH_INPUT_EXCEPTION:
//                        onBluetoothClientException((IOException) msg.obj);
//                        break;
                }
            }
        }

        /**
         * 通知回调数据输入回调方法
         * @param buffer 字节缓存
         */
        void notifyInput(byte[] buffer) {
            mHandler.sendMessage(mHandler.obtainMessage(BluetoothInputHandler.MESSAGE_BLUETOOTH_INPUT_ONINPUT, buffer));
        }

        /**
         * 通知回调断开连接回调方法
         * @param e 异常信息
         */
        void notifyDisconnected(IOException e) {
            mHandler.sendMessage(mHandler.obtainMessage(BluetoothInputHandler.MESSAGE_BLUETOOTH_DISCONNECTED, e));
        }

//        /**
//         * 通知回调蓝牙终端异常回调方法
//         * @param e 异常信息
//         */
//        void notifyBluetoothClientException(IOException e) {
//            mHandler.obtainMessage(BluetoothInputHandler.MESSAGE_BLUETOOTH_INPUT_EXCEPTION, e).sendToTarget();
//        }

        /**
         * 数据输入回调
         * @param buffer 字节缓存
         */
        public abstract void onInput(byte[] buffer);

        /**
         * 断开连接消息回调
         * @param e 异常信息
         */
        public abstract void onDisconnected(IOException e);

//        /**
//         * 蓝牙终端异常消息回调
//         * @param e 异常信息
//         */
//        public abstract void onBluetoothClientException(IOException e);
    }
}
