package com.makeblock.bluetooth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.os.AsyncTask;
import com.makeblock.bluetooth.BluetoothDelegate;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

public class BluetoothManager {
    enum ProtocolType{
        TYPE_DIGITAL,
        TYPE_ANALOG,
        TYPE_DIGITAL_PWM,
        TYPE_PORT
    }

    private BluetoothSocket _btSocket;
    private BluetoothAdapter _btAdapter;
    private InputStream _inStream;
    private OutputStream _outStream;
    private Handler _handler;
    private Runnable _runnable;
   	private int _currentDeviceIndex = 0;
    public byte[] currentBuffer = new byte[256];
    public int currentBufferLength;

    {
        currentBufferLength = 0;
    }

    private boolean _isConnected = false;
    public ArrayList<BluetoothDevice> devices;
    private ArrayList<BluetoothDelegate> _delegates;
    private Boolean _isCancelDiscovery = false;
    private Runnable _readRunnable;
    private Runnable _writeRunnable;
    private int _delayWriteTime = 1;
    private int _delayReadTime = 20;
    private long _currentTimestamp = 0;
    private ArrayList<byte[]> _bufferList = new ArrayList<byte[]>();


    int _bufferIndex =0;
    boolean _startParse = false;
    char[] _buffer = new char[10];

    public BluetoothManager(){
        _btAdapter = BluetoothAdapter.getDefaultAdapter();
        _delegates = new ArrayList<BluetoothDelegate>();
        devices = new ArrayList<BluetoothDevice>();
        _handler=new Handler();
        _runnable=new Runnable() {
            @Override
            public void run() {
//                Set<BluetoothDevice> list = _btAdapter.getBondedDevices();
//                Iterator<BluetoothDevice> deviceIterator =  list.iterator();
//                while(deviceIterator.hasNext()){
//                    BluetoothDevice device = deviceIterator.next();
//                    if(device!=null){
//                        addDevice(device);
//                    }
//                }
                update();
                if(_isCancelDiscovery==false){
                    _handler.postDelayed(this, 2000);
                }
            }
        };
        _readRunnable = new Runnable(){
            @Override
            public void run() {
                recvBuffer();
                if(_isConnected==true){
                    _handler.postDelayed(this, _delayReadTime);
                }
            }
        };
        _handler.postDelayed(_runnable, 2000);

        _writeRunnable = new Runnable(){
            @Override
            public void run() {
                writeBuffer();
                if(_isConnected==true){
                    if(_bufferList.size()<2){
                        _delayWriteTime = 4;
                    }else if(_bufferList.size()<10){
                        _delayWriteTime = 3;
                    }else{
                        _delayWriteTime = 2;
                    }
                    _handler.postDelayed(this, _delayWriteTime);
                }
            }
        };
    }
    public String[] getBondedDevices(){
        Set<BluetoothDevice> list = _btAdapter.getBondedDevices();
        Iterator<BluetoothDevice> deviceIterator =  list.iterator();
        String[] output_list = new String[list.size()];
        int i=0;
        while(deviceIterator.hasNext()){
            BluetoothDevice device = deviceIterator.next();
            if(device!=null){
                output_list[i] = device.getName();
                i++;
            }
        }
        return output_list;
    }
    public void addDelegate(BluetoothDelegate activity){
        if(!_delegates.contains(activity)){
            _delegates.add(activity);
        }
    }
    public void removeDelegate(BluetoothDelegate activity){
        if(_delegates.contains(activity)){
            _delegates.remove(activity);
        }
    }
    public String getName(){
        return _btAdapter.getName();
    }
    public void addDevice(BluetoothDevice device){
        if(!devices.contains(device)){
            devices.add(device);
        }
    }
    private void update(){
        if(_btSocket!=null){
//            Log.d("mb","--------connected:"+_btSocket.isConnected());
        }
        int i;
        for(i=0;i<_delegates.size();i++){
            _delegates.get(i).update();
        }
    }
    public void startDiscovery(){
        if(_btAdapter.isEnabled()){
            if(!_btAdapter.isDiscovering()){
                _btAdapter.startDiscovery();
            }
        }
    }
    public void cancelDiscovery(){
        _handler.removeCallbacks(_runnable);
        _isCancelDiscovery = true;
        if(_btAdapter.isEnabled()){
            if(_btAdapter.isDiscovering()){
                _btAdapter.cancelDiscovery();
            }
        }
    }
    public void enable(){
        if (! _btAdapter.isEnabled()) {
            _btAdapter.enable();
            _handler.postDelayed(_runnable, 1000);
        }
    }
    public boolean isEnabled(){
        return _btAdapter.isEnabled();
    }
    public void disable(){
        if(_btAdapter.isEnabled()){
            disconnectDevice();
            _btAdapter.disable();
        }
    }

    /*
     * connect device
     * */
    public boolean isConnected(){
        if(_btSocket!=null){
            return _isConnected;
        }
        return false;
    }
    public void connectDevice(int index){
		_currentDeviceIndex = index;
        new BluetoothConnect(devices.get(_currentDeviceIndex)).start();
    }
    public void onFinishConnect(BluetoothSocket socket){
        if(socket!=null){
            try{
                _btSocket = socket;
                _outStream = _btSocket.getOutputStream();
                _inStream = _btSocket.getInputStream();
                enableReadWrite();
                _isConnected = true;
            }catch (IOException e){
                _isConnected = false;
            }
        }else{
            _isConnected = false;
        }
        for(int i=0;i<_delegates.size();i++){
            _delegates.get(i).onFinishConnect(_isConnected);
        }
    }
    public void disconnectDevice(){
        try{
            if(_btSocket!=null){
                _inStream.close();
                _outStream.close();
                _btSocket.close();
                _isConnected = false;
            }
        }catch(IOException err){

        }
    }
    /*
     * send
     * */
    public void sendString(String value){
        try{
            byte[] buffer = (value+"\n").getBytes("UTF-8");
            if(_outStream!=null){
                _bufferList.add(buffer);
            }
        }catch(Exception err){

        }
    }
    public void sendBytes(byte[] buffer){

        if(_isConnected){
            if(_outStream!=null){
                try{
                    _outStream.write(buffer);
                }catch (Exception e){

                }
                    //&&_bufferList.size()<40){
           //     _bufferList.add(buffer);
            }
        }
    }
    public byte[] concatBytes(byte[] bytes1,byte[] bytes2) {
        List<byte[]> bytes = new ArrayList<byte[]>();
        int len = 0;
        len+= bytes1.length;
        len+= bytes2.length;
        byte[] destArray = new byte[len];
        int destLen = 0;
        System.arraycopy(bytes1, 0, destArray, destLen, bytes1.length);
        destLen += bytes2.length;
        System.arraycopy(bytes2, 0, destArray, destLen, bytes2.length);
        destLen += bytes2.length;
        return destArray;
    }
    public void sendPinCommand(int type,int mode,int pin,int value){
        /*
        0xff
        high 0-3 digital analog digital_pwm port
        low 0-3 mode(output,input,pwm,servo,analog)
        0 reserve
        0-100 number
        0-1 +/-
        0-100 xx00
        0-100 00xx
        0xfe
         */


            byte[] bytes = new byte[8];

            bytes[0]=(byte)0xff;
            bytes[1]=(byte)((type<<4)+0);
            bytes[2]=(byte)mode;
            bytes[3]=(byte)pin;
            value = Math.abs(value);
            bytes[4]=(byte)(value>=0?1:2);
            bytes[5]=(byte)Math.floor(value/100);
            bytes[6]=(byte)(value-bytes[5]*100);
            bytes[7]=(byte)0xfe;
            sendBytes(bytes);

    }
    public void sendPortCommand(int type,int device,int mode,int portnumber,int value){
        /*
        0xff
        high 0-7 servo,motor,ultrasonic...
        low 0-3 mode(begin,write,read)
        1-2 device
        0-100 port number
        0-1 +/-
        0-100 xx00
        0-100 00xx
        0xfe
         */

        byte[] bytes = new byte[8];
        bytes[0]=(byte)0xff;
        bytes[1]=(byte)((type<<4)+device);
        bytes[2]=(byte)mode;
        bytes[3]=(byte)portnumber;
        value = Math.abs(value);
        bytes[4]=(byte)(value>=0?1:2);
        bytes[5]=(byte)Math.floor(value/100);
        bytes[6]=(byte)(value-bytes[5]*100);
        bytes[7]=(byte)0xfe;
        sendBytes(bytes);

    }
    private void writeBuffer(){
        try{
            if(_outStream!=null){
                if(_bufferList.size()>0){
                    if(System.currentTimeMillis()-_currentTimestamp>5){
                        byte[] buffer = _bufferList.get(0);
                        _outStream.write(buffer);
                        _bufferList.remove(0);
                        _currentTimestamp = System.currentTimeMillis();
                    }
                }
            }
        }catch(IOException err){

        }
    }
    /*
     * read
     * */
    private void enableReadWrite(){
        _handler.postDelayed(_readRunnable, 10);
        _handler.postDelayed(_writeRunnable, _delayWriteTime);
    }
    private void recvBuffer(){
        if(_inStream!=null){
            try{

                if (_inStream.available() > 0) {
                    //Log.d("mb","available:"+_inStream.available());
                    currentBufferLength = _inStream.read(currentBuffer);
                    if(currentBufferLength>0){
                        parseCommand();
                        update();
                    }
                }
                currentBufferLength = 0;
            }catch(IOException err){

            }
        }
    }
    private void parseCommand(){
        int inDat;
        int j;
        if(BluetoothManager.sharedManager().currentBufferLength>0){
            for(int i=0;i<BluetoothManager.sharedManager().currentBufferLength;i++){

                int x = 0;

                x |= BluetoothManager.sharedManager().currentBuffer[i]&0xff;
                inDat = x;
                if(inDat==0xff){
                    _bufferIndex=0;
                    _startParse = true;
                }

                if(_startParse){
                    _buffer[_bufferIndex]=(char)(inDat&0xff);
                }
                if(inDat==0xfe&&_startParse){
                    _startParse = false;

                    for(j=0;j<_delegates.size();j++){
                        _delegates.get(j).parseCommand(_buffer,_bufferIndex+1);
                    }
                }
                _bufferIndex++;
                if(_bufferIndex>9){
                    _bufferIndex=0;
                }
            }
        }
    }
    public int findDeviceIndexByAddress(String address){
        for(int i=0;i<devices.size();i++){
            if(address.equals(devices.get(i).getAddress())){
                return i;
            }
        }
        return 0;
    }
    private static BluetoothManager _instance;
    public static BluetoothManager sharedManager(){
        if(_instance==null){
            _instance = new BluetoothManager();
        }
        return _instance;
    }
}
