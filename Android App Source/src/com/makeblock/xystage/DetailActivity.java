package com.makeblock.xystage;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import com.makeblock.bluetooth.BluetoothDelegate;
import com.makeblock.bluetooth.BluetoothManager;
import com.makeblock.xystage.views.DrawImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 13-10-5
 * Time: 下午1:30
 * To change this template use File | Settings | File Templates.
 */
public class DetailActivity extends BluetoothDelegate {
    /**
     * Called when the activity is first created.
     */
    private String _imageURL;
    private Button _startButton;
    private Button _resetButton;
    private CheckBox _checkBox;
    private boolean _isMoving = false;
    private Bitmap _myBitmap;
    private DrawImageView _imageView;
    private ImageView _pointView;

    private Handler _handler;
    private Runnable _runnable;
    private int xpos[] = new int[200000];
    private int ypos[] = new int[200000];
    private long _pointCount;
    private long _pointIndex;
    private boolean _isDestroy = false;
    private Point _prevPoint = new Point(0,0);
    private TextView _timeView;
    private long _startTime;
    private int _sendCount = 16;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        Intent intent = getIntent();
        _imageURL = intent.getStringExtra("ImageURL");

        _imageView= (DrawImageView)findViewById(R.id.imageSource);
        _pointView = (ImageView)findViewById(R.id.targetView);
        _timeView = (TextView)findViewById(R.id.timeView);
        _checkBox = (CheckBox)findViewById(R.id.checkBox);
        BluetoothManager.sharedManager().addDelegate(this);
        if(_imageURL==null){

            ContentResolver cr = this.getContentResolver();
            try {
                File f = new File(intent.getStringExtra("bitmap"));
                InputStream stream = cr.openInputStream(Uri.fromFile(f));
                _myBitmap = contrastBitmap(resizeBitmap(BitmapFactory.decodeStream(stream), 800), 80);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }else{
            InputStream assetFile = null;
            AssetManager assets = getAssets();
            try{
                //打开指定资源对应的输入流
                assetFile = assets.open(_imageURL);

            }catch(IOException e){
                e.printStackTrace();
            }

            BitmapFactory.Options options = new BitmapFactory.Options();

            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.RGB_565;

            options.inPurgeable = true;

            options.inInputShareable = true;
            options.inSampleSize = 1;
            Bitmap temp = BitmapFactory.decodeStream(assetFile);
            Bitmap finalImage = temp .copy(temp.getConfig(), true);
            temp.recycle();
            temp=null;
            _myBitmap = resizeBitmap(finalImage, 800);
        }
        _imageView.setImageBitmap(_myBitmap);
        _startButton= (Button)findViewById(R.id.start_button);
        _resetButton= (Button)findViewById(R.id.reset_button);
        _startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                _isMoving = !_isMoving;
                if(_isMoving&&_pointIndex==0){
                    _startTime = System.currentTimeMillis();
                    _prevPoint = new Point(0,0);
                    _imageView.setImageResource(R.drawable.blank);
                    _imageView.reset();
                    generalPixels();
                }
                _startButton.setText(_isMoving ? "Pause" : "Start");
            }
        });
        _resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommand(0,0, true);
                _isMoving = false;
                _pointIndex = 0;
                _imageView.reset();
                _prevPoint = new Point(0,0);
                generalPixels();
                _startButton.setText("Start");

            }
        });
        _handler=new Handler();
        _runnable=new Runnable(){
            @Override
            public void run() {
                updatePosition();
                if(!_isDestroy){
                    _handler.postDelayed(this,800);
                }
            }
        };
        //if(!BluetoothManager.sharedManager().isConnected()){
            _handler.postDelayed(_runnable,800);
        //}
        Button reduceButton = (Button)findViewById(R.id.reduce_speed);
        reduceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _sendCount--;
                if(_sendCount<1){
                    _sendCount = 1;
                }
                TextView speedView = (TextView)findViewById(R.id.speedView);
                //BluetoothManager.sharedManager().sendString("G1 F"+_sendCount*100);
                speedView.setText("Speed:"+_sendCount);
            }
        });
        Button addButton = (Button)findViewById(R.id.add_speed);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _sendCount++;
                if(_sendCount>50){
                    _sendCount = 50;
                }
                TextView speedView = (TextView)findViewById(R.id.speedView);
                //BluetoothManager.sharedManager().sendString("G1 F"+_sendCount*100);
                speedView.setText("Speed:"+_sendCount);
            }
        });
    }
    private void sendCommand(int x,int y,boolean isFinish){
        x = Math.min(3600,x);
        y = Math.min(3600,y);
        //Log.d("mb","x:"+x+" y:"+y);
        byte[] buffer = new byte[7];

//        buffer[0] = (byte)(x&0xFF);
//        buffer[1] = (byte)((x&0xFFFF)>>8);
//        buffer[2] = (byte)(y&0xFF);
//        buffer[3] = (byte)((y&0xFFFF)>>8);
        buffer[0] = (byte)(0xFF);
        buffer[1] = (byte)(0xFE);
        buffer[2] = (byte)(x&0xFF);
        buffer[3] = (byte)((x&0xFFFF)>>8);
        buffer[4] = (byte)(y&0xFF);
        buffer[5] = (byte)((y&0xFFFF)>>8);
        buffer[6] = (byte)((isFinish==true)?0xFC:0xFD);
        BluetoothManager.sharedManager().sendBytes(buffer);
    }
    private void updatePosition(){
        if(_isMoving){
            long secs = (System.currentTimeMillis()-_startTime)/1000;
            int min = (int)(Math.ceil(secs/60));
            int sec = (int)(secs%60);
            _timeView.setText("Time "+(min<10?"0"+min:min)+":"+(sec<10?"0"+sec:sec));
            int len = BluetoothManager.sharedManager().isConnected()?_sendCount:14;
            for(int i=0;i<len;i++){
                if(_isMoving){
                    Point point = findNearPixel();
                    if(_prevPoint.x==0&&_prevPoint.y==0){
                        _prevPoint = point;
                    }
                    float scaleX = (float)_imageView.getWidth()/(float)_myBitmap.getWidth();
                    float scaleY = (float)_imageView.getHeight()/(float)_myBitmap.getHeight();
                    int x = (int)(point.x*scaleX);
                    int y = (int)(point.y*scaleY);
                    int prec = _checkBox.isChecked()?3:2;
                    //sendGCode((x-_prevPoint.x)*prec,(_prevPoint.y-y)*prec);

                    _prevPoint.x = x;
                    _prevPoint.y = y;
                    sendCommand(x*prec,y*prec, false);
                    _imageView.addPoint(point.x,point.y);
                    x-=5;
                    y-=5;
                    _pointView.setPadding(x,y,x+11,y+11);
                }else{
                    break;
                }
            }
            _imageView.invalidate();
        }
    }
    private void sendGCode(int x,int y){
        BluetoothManager.sharedManager().sendString("G1 X"+x+" Y"+y);
    }
    public void update(){
        if(BluetoothManager.sharedManager().currentBufferLength>0){
            Log.d("mb","buffer:"+BluetoothManager.sharedManager().currentBuffer[0]);
            if(BluetoothManager.sharedManager().currentBuffer[0]<=64){
                //updatePosition();
            }
        }
    }
    private Point findNearPixel(){
        double dist=1000000.0f;
        double currentDist=0;
        int dx;
        int dy;
        int target = 0;
        int i;
        int gate = 2;
        for( i=0;i<_pointCount;i++){
            if(xpos[i]!=0&&ypos[i]!=0){
                dx = xpos[i]-_prevPoint.x;
                dy = ypos[i]-_prevPoint.y;
                currentDist = Math.sqrt(dx*dx+dy*dy);
                if(currentDist<dist){
                    dist = currentDist;
                    target = i;
                }
                if(currentDist<gate){
                    dist = currentDist;
                    target = i;
                    break;
                }
            }
        }
        if(dist>gate){
            Log.d("mb","dist:"+dist);
        }
        Point nearPoint = new Point(xpos[target],ypos[target]);
        xpos[target]=0;
        ypos[target]=0;
        _pointIndex++;
        if(_pointIndex>=_pointCount){
            sendCommand(0,0, true);
            _isMoving = false;
            _startButton.setText("Start");
            _pointIndex = 0;
        }
        return nearPoint;
    }
    private Bitmap contrastBitmap(Bitmap bitmap,int progress){
        Bitmap bmp = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
                Bitmap.Config.ARGB_8888);
        //0-127
        float contrast = (float) ((progress + 64) / 128.0);
        ColorMatrix cMatrix = new ColorMatrix();
        cMatrix.set(new float[] { contrast, 0, 0, 0, 0, 0,
                contrast, 0, 0, 0,// 改变对比度
                0, 0, contrast, 0, 0, 0, 0, 0, 1, 0 });

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cMatrix));

        Canvas canvas = new Canvas(bmp);
        // 在Canvas上绘制一个已经存在的Bitmap。这样，dstBitmap就和srcBitmap一摸一样了
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return bmp;
    }
    private Bitmap resizeBitmap(Bitmap bitmap, int newWidth) {

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float temp = ((float) height) / ((float) width);
        int newHeight = (int) ((newWidth) * temp);
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        // resize the bit map
        matrix.postScale(scaleWidth, scaleHeight);
        // matrix.postRotate(45);
        Bitmap output = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        bitmap.recycle();
        return output;

    }
    private void generalPixels(){
        int index = 0;
        int i,j,rgb,gray;
        for(i=0;i<_myBitmap.getHeight();i++){
            if(!_checkBox.isChecked()){
                if(i%2==0)continue;
            }
            for(j=0;j<_myBitmap.getWidth();j++){
                if(!_checkBox.isChecked()){
                    if(j%2==0)continue;
                }
                rgb = _myBitmap.getPixel(j, i);
                gray = (int)(Color.red(rgb)*0.3+Color.green(rgb)*0.6+Color.blue(rgb)*0.1);
                if(gray<85){
                    xpos[index] = j;
                    ypos[index] = i;
                    index++;
                }
            }
        }
        _pointCount = index;
        Log.d("mb","count:"+_pointCount);
    }
    @Override
    public void onDestroy(){
        _isDestroy = true;
        if(_imageView != null && _imageView.getDrawable() != null){

            Bitmap oldBitmap = ((BitmapDrawable) _imageView.getDrawable()).getBitmap();

            _imageView.setImageDrawable(null);

            if(oldBitmap != null){

                oldBitmap.recycle();

                oldBitmap = null;

            }

        }

        // Other code.

        System.gc();
        BluetoothManager.sharedManager().removeDelegate(this);
        super.onDestroy();
    }
}
