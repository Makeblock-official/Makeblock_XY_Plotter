package com.makeblock.bluetooth;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 13-7-22
 * Time: 下午5:03
 * To change this template use File | Settings | File Templates.
 */

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;
import android.os.Handler;

public class BluetoothConnect extends Thread {
    private BluetoothDevice _device;
    private BluetoothSocket _socket;
    private boolean _isConnect;
    private Runnable _runnableUi;
    private Handler _handler = new Handler();
    public BluetoothConnect(BluetoothDevice device){
        BluetoothSocket tmp = null;
        _device = device;
        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
        } catch (IOException e) { }
        _socket = tmp;
        _runnableUi=new  Runnable(){
            @Override
            public void run() {
                    BluetoothManager.sharedManager().onFinishConnect(_socket);
            }

        };
    }
    @Override
    public void run(){
        try {
            _socket.connect();
           _handler.post(_runnableUi);
        } catch (Exception e) {
            BluetoothManager.sharedManager().onFinishConnect(null);
            Log.d("mb", "----------error--------:" + e);
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

}

