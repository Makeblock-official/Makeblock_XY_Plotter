package com.makeblock.bluetooth;

import android.app.Activity;
public class BluetoothDelegate extends Activity {

    public void update(){

    }
    public void parseCommand(char[] buffer,int length){

    }
    public void onDestroy(){
        try{
            BluetoothManager.sharedManager().removeDelegate(this);
        }catch(Exception ex){}
        super.onDestroy();
    }
    public void onFinishConnect(boolean state){

    }
}
