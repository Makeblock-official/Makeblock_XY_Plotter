package com.makeblock.xystage;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.*;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import com.makeblock.bluetooth.BluetoothDelegate;
import com.makeblock.bluetooth.BluetoothManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class StageActivity extends BluetoothDelegate {
    /**
     * Called when the activity is first created.
     */
    private Spinner _spinnerDevices;
    private ImageButton _buttonConnect;
    private Handler _connect_handler;
    private Runnable _connect_runnable;
    private boolean _isConnected = false;
    private boolean _isLoading = false;
    private ProgressDialog _loadingDialog;
    private Handler _handler;
    private Runnable _runnable;
    private int _currentDeviceIndex = 0;
    private BluetoothReciever _bluetoothReceive;


    private boolean isexit = false;
    private boolean hastask = false;
    private Timer texit = new Timer();
    private TimerTask task;
    private int _currentTabId=-1;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothManager.sharedManager().addDelegate(this);
        if(!BluetoothManager.sharedManager().isEnabled()){
            BluetoothManager.sharedManager().enable();
        }
        ImageButton infoButton = (ImageButton)findViewById(R.id.info_button);
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent infoIntent = new Intent(StageActivity.this, InfoActivity.class);
                startActivity(infoIntent);
            }
        });
        _handler=new Handler();
        _runnable=new Runnable(){
            @Override
            public void run() {
                BluetoothManager.sharedManager().connectDevice(_currentDeviceIndex);

            }
        };
        _spinnerDevices = (Spinner)findViewById(R.id.spinner_devices);
        _buttonConnect = (ImageButton)findViewById(R.id.button_connect);
        _buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(_isLoading){
                    return;
                }
                if(!BluetoothManager.sharedManager().isConnected()){
                    showLoadingDialog();
                    _handler.postDelayed(_runnable, 200);

                }else{
                    _isConnected = false;
                    _buttonConnect.setImageResource(R.drawable.connector_disable);
                    BluetoothManager.sharedManager().disconnectDevice();
                }
            }
        });
        ArrayList<String>allItems = new ArrayList<String>();
        allItems.add("Searching...");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(StageActivity.this,android.R.layout.simple_spinner_item, allItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        _spinnerDevices.setAdapter(adapter);
        _spinnerDevices.setEnabled(false);
        _spinnerDevices.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1,
                                       int arg2, long arg3) {
                _currentDeviceIndex = arg2;
                if(_spinnerDevices.getSelectedItem().equals("Searching...")){
                    return;
                }
                SharedPreferences.Editor sharedObject = StageActivity.this.getSharedPreferences("bluetooth", 0).edit();
                sharedObject.putString("deviceAddress", BluetoothManager.sharedManager().devices.get(arg2).getAddress());
                sharedObject.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });
        _bluetoothReceive = new BluetoothReciever();
        IntentFilter intentFoundFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(_bluetoothReceive, intentFoundFilter);
        IntentFilter intentDisconnectFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(_bluetoothReceive, intentDisconnectFilter);

        task = new TimerTask() {
            public void run() {
                isexit = false;
                hastask = true;
            }
        };
        Button galleryButton = (Button)findViewById(R.id.gallery_button);
        galleryButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction()!=2){
                    Log.d("mb","touch state:"+motionEvent.getAction());
                }
                if(motionEvent.getAction()==0){
                    LinearLayout layout = (LinearLayout)findViewById(R.id.gallery_button_bg);
                    layout.setBackgroundColor(0xffddefff);
                }
                if(motionEvent.getAction()==1){

                    LinearLayout layout = (LinearLayout)findViewById(R.id.gallery_button_bg);
                    layout.setBackgroundColor(0xffffffff);
                }
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }
        });
        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                                    /* 开启Pictures画面Type设定为image */
                intent.setType("image/*");
                                    /* 使用Intent.ACTION_GET_CONTENT这个Action */
                intent.setAction(Intent.ACTION_GET_CONTENT);
                                    /* 取得相片后返回本画面 */
                startActivityForResult(intent, 2);
            }
        });
    }
    private void showLoadingDialog(){
        _isLoading = true;
        _loadingDialog = ProgressDialog.show(this, // context
                "", // title
                "Connecting. Please wait...", // message
                true);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ContentResolver contentResolver  = getContentResolver();
        if (resultCode == RESULT_OK&&requestCode==2) {
            Uri uri = data.getData();
            Log.e("uri", uri.toString());

            try{
                Intent detailIntent = new Intent(StageActivity.this.getBaseContext(), DetailActivity.class);

                String[] proj = { MediaStore.Images.Media.DATA };

                Cursor actualimagecursor = managedQuery(uri,proj,null,null,null);

                int actual_image_column_index = actualimagecursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

                actualimagecursor.moveToFirst();

                String img_path = actualimagecursor.getString(actual_image_column_index);
                detailIntent.putExtra("bitmap", img_path);
                startActivity(detailIntent);

            }catch(Exception e){

            }
        }
        super.onActivityResult(requestCode, resultCode, data);

    }
    @Override
    public void onDestroy(){
        unregisterReceiver(_bluetoothReceive);
        try{
            BluetoothManager.sharedManager().removeDelegate(this);
        }catch(Exception ex){}
        super.onDestroy();
    }
    @Override
    public void onFinishConnect(boolean state){
        if(state==true){
            _buttonConnect.setImageResource(R.drawable.connector_enable);
        }
        if(_loadingDialog!=null){
            _isLoading = false;
            _loadingDialog.dismiss();
        }
    }
    @Override
    public void update(){
        BluetoothManager.sharedManager().startDiscovery();

    }
    private class BluetoothReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getName()!=null){
                    BluetoothManager btManager = BluetoothManager.sharedManager();
                    btManager.addDevice(device);
                }
                ArrayList<BluetoothDevice> list = BluetoothManager.sharedManager().devices;

                if(list.size()!=_spinnerDevices.getCount()||_spinnerDevices.getSelectedItem().equals("Searching...")){
                    ArrayList<String>allItems = new ArrayList<String>();
                    for(int i=0;i<list.size();i++){
                        allItems.add(list.get(i).getName());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(StageActivity.this,android.R.layout.simple_spinner_item, allItems);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    _spinnerDevices.setAdapter(adapter);
                    _spinnerDevices.setEnabled(true);
                    SharedPreferences sharedObject = StageActivity.this.getSharedPreferences("bluetooth", 0);
                    _currentDeviceIndex = BluetoothManager.sharedManager().findDeviceIndexByAddress(sharedObject.getString("deviceAddress",""));
                    if(_currentDeviceIndex<allItems.size()){
                        _spinnerDevices.setSelection(_currentDeviceIndex);
                    }
                    //BluetoothManager.sharedManager().cancelDiscovery();
                }
            }
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                _isConnected = false;
                _buttonConnect.setImageResource(R.drawable.connector_disable);
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.showcase, menu);
        return true;
    }
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN
                && event.getRepeatCount() == 0) {
            if(isexit == false){
                isexit = true;
                Toast.makeText(getApplicationContext(), "Press back again to quit", Toast.LENGTH_SHORT).show();
                if(!hastask) {
                    texit.schedule(task, 2000);
                }
            }else{
                BluetoothManager.sharedManager().disable();
                finish();
                System.exit(0);
            }
            return false;
        }
        return super.dispatchKeyEvent(event);
    }
}
