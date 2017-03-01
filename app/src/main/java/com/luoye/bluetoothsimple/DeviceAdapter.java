package com.luoye.bluetoothsimple;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * Created by LUOYE on 2017/2/26.
 */

public class DeviceAdapter extends BaseAdapter {
    /** 上下文 */
    private Context mContext;
    /** 蓝牙设备列表 */
    private ArrayList<Device> mDevices = new ArrayList<>();

    public DeviceAdapter(Context context, ArrayList<Device> devices) {
        mContext = context;
        if (null != devices)
            mDevices = devices;
    }

    public void add(Device device) {
        mDevices.add(device);
        notifyDataSetChanged();
    }

    public boolean add(Collection<Device> devices) {
        boolean bSuccess = mDevices.addAll(devices);
        notifyDataSetChanged();
        return bSuccess;
    }

    public void clear() {
        mDevices.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mDevices.size();
    }

    @Override
    public Object getItem(int position) {
        return mDevices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (null == convertView) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.view_listview_item_discover, null);
            holder.mTxtName = (TextView) convertView.findViewById(R.id.txt_name);
            holder.mTxtState = (TextView) convertView.findViewById(R.id.txt_state);
            holder.mTxtTime = (TextView) convertView.findViewById(R.id.txt_time);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Device device = mDevices.get(position);
        holder.mTxtName.setText(TextUtils.isEmpty(device.device.getName()) ? device.device.getAddress() : device.device.getName());
        if (device.device.getBondState() == BluetoothDevice.BOND_BONDED) {
            holder.mTxtState.setText("已配对");
        } else if (device.device.getBondState() == BluetoothDevice.BOND_BONDING) {
            holder.mTxtState.setText("配对中...");
        } else {
            holder.mTxtState.setText("未配对");
        }
        holder.mTxtTime.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(device.time)));
        return convertView;
    }

    class ViewHolder {
        TextView mTxtName;
        TextView mTxtState;
        TextView mTxtTime;
    }
}
