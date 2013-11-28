package com.makeblock.xystage.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import java.util.jar.Attributes;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 13-10-5
 * Time: 下午5:54
 * To change this template use File | Settings | File Templates.
 */
public class DrawImageView extends ImageView {
    private Paint _paint = new Paint();
    private float[] _points;
    private Bitmap _bitmap;

    private int index=0;
    public DrawImageView(Context context) {
        super(context);
        _paint.setColor(Color.BLACK);
        _paint.setStyle(Paint.Style.STROKE);
        _paint.setStrokeWidth(1.0f);
        _points = new float[1000000];
        _bitmap = Bitmap.createBitmap(640,640, Bitmap.Config.ARGB_8888);
    }
    public DrawImageView(Context context,AttributeSet attributes){
        super(context,attributes);
        _paint.setColor(Color.BLUE);
        _paint.setStyle(Paint.Style.STROKE);
        _paint.setStrokeWidth(1.0f);
        _points = new float[1000000];
        _bitmap = Bitmap.createBitmap(640,640, Bitmap.Config.ARGB_8888);
    }
    public void reset(){
        _points = new float[1000000];
        _bitmap.eraseColor(0xffffffff);
        this.setImageBitmap(_bitmap);
    }
    @Override
    protected void onDraw(Canvas canvas)
    {
        try{
            super.onDraw(canvas);
            //画出之前所有的线
            if(index>1){
                canvas.drawPoints(_points,_paint);
            }
        }catch (Exception e){

        }
    }
    public void addPoint(float x, float y){
        if(!_bitmap.isRecycled()){
            _bitmap .setPixel((int)x, (int)y, 0xff00ccff);
        }
        //this.setImageBitmap(_bitmap);
       // return;
//        _points[index++] = x;
//        _points[index++] = y;
    }
}
