package com.luoye.bluetoothsimple;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.luoye.bluetooth.Bluetooth;
import com.luoye.bluetooth.BluetoothClient;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, DiscoverDialog.OnBluetoothDeviceClickListener {
    /** 页面请求码：开启蓝牙设备请求 */
    private static final int REQUEST_CODE_ENABLE_BLUETOOTH = 1;
    /** 连接按钮 */
    Button mBtnConnect;
    /** 断开连接按钮 */
    Button mBtnDisconnect;
    /** 蓝牙模块 */
    Bluetooth mBluetooth;
    /** 蓝牙通信终端 */
    BluetoothClient mBluetoothClient;
    /** 检索蓝牙设备弹出框 */
    DiscoverDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnConnect = (Button) findViewById(R.id.btn_connect);
        mBtnConnect.setOnClickListener(this);
        mBtnDisconnect = (Button) findViewById(R.id.btn_disconnect);
        mBtnDisconnect.setOnClickListener(this);

        mBluetooth = new Bluetooth(this);
        if (!mBluetooth.isEnabled()) {
            mBluetooth.enable();
        }
    }

    @Override
    protected void onDestroy() {
        if (null != mBluetoothClient && mBluetoothClient.status() < BluetoothClient.BLUETOOTH_CLIENT_STATUS_CLOSED) {
            try {
                mBluetoothClient.shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_connect:
                if (!mBluetooth.isEnabled()) {
                    mBluetooth.enable(this, REQUEST_CODE_ENABLE_BLUETOOTH);
                    return;
                }
                scanDevices();
                break;
            case R.id.btn_disconnect:
                try {
                    mBluetoothClient.shutdown();
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                mBtnDisconnect.setEnabled(false);
                Toast.makeText(this, "断开连接成功！", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * 扫描蓝牙
     */
    private void scanDevices() {
        if (null != mBluetoothClient && mBluetoothClient.status() < BluetoothClient.BLUETOOTH_CLIENT_STATUS_CLOSED) {
            try {
                mBluetoothClient.shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 连接蓝牙
        if (null == mDialog) {
            mDialog = new DiscoverDialog(this, mBluetooth);
            mDialog.setOnBluetoothDeviceClickListener(this);
        }
        mDialog.show();
    }

    @Override
    public void onBluetoothClick(BluetoothDevice device) {
        try {
            mBluetoothClient = mBluetooth.newBluetoothClient(device, Bluetooth.UUID_SECURE, true);
            mBluetoothClient.asyncConnect(new BluetoothClient.OnBluetoothConnectCallback() {
                @Override
                public void onConnectSuccess(BluetoothClient client) {
                    Toast.makeText(MainActivity.this, "连接成功！", Toast.LENGTH_SHORT).show();
                    mBtnDisconnect.setEnabled(true);
                    try {
                        client.asyncRead(new BluetoothClient.BluetoothInputCallback() {
                            @Override
                            public void onInput(byte[] buffer) {
                            }

                            @Override
                            public void onDisconnected(IOException e) {
                                Toast.makeText(MainActivity.this, "蓝牙连接已自动断开！", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConnectFailure(Exception e) {
                    Toast.makeText(MainActivity.this, "连接失败！", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (RESULT_OK == resultCode) {
            if (REQUEST_CODE_ENABLE_BLUETOOTH == requestCode) {
                scanDevices();
            }
        }
    }
}
