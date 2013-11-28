package com.makeblock.xystage.adapters;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.Visibility;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import com.makeblock.xystage.DetailActivity;
import com.makeblock.xystage.R;
import com.makeblock.xystage.models.ExampleListItem;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;


/**
 * Created by indream on 13-6-7.
 */
public class ExampleListAdapter extends ArrayAdapter<ExampleListItem> {

    private ListView _listView;
    private JSONArray _list;
    private Activity _activity;
    private ProgressDialog _loadingDialog;
    private static boolean isLoaded;
    public ExampleListAdapter(Activity activity,ListView listView) {

        super(activity, 0);
        _activity = activity;
        _listView = listView;
        AssetManager assetManager = activity.getResources().getAssets();

        InputStream inputStream = null;
        try {
            inputStream = assetManager.open("examples.json");
        } catch (IOException e) {
            Log.e("tag", e.getMessage());
        }
        String s = readTextFile(inputStream);
        try{
            JSONObject jsonObject = new JSONObject(s);
            if(_loadingDialog!=null){
                _loadingDialog.dismiss();
                _loadingDialog = null;
            }
            if(jsonObject!=null){
                isLoaded = true;
                 _list = jsonObject.getJSONArray("list");
                Log.d("mb","len:"+(int)(Math.ceil(_list.length() / 3)+1));
            }
        }catch (Exception e){

        }
    }
    private String readTextFile(InputStream inputStream) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
        }
        return outputStream.toString();
    }
    private void showLoadingDialog(){
        _loadingDialog = ProgressDialog.show(_activity, // context
                "", // title
                "Loading. Please wait...", // message
                true);
    }


    @Override
    public int getCount(){
        return (int)(Math.ceil(_list.length() / 3)+1);
    }
    public View getView(int position, View convertView, ViewGroup parent) {

        Activity activity = (Activity) getContext();
        View rowView;
        LayoutInflater inflater;
        rowView = convertView;
        ViewCache viewCache;
        if (rowView == null) {
            inflater = activity.getLayoutInflater();
            rowView = inflater.inflate(R.layout.example_list_view_item, null);
            viewCache = new ViewCache(rowView);
            rowView.setTag(viewCache);
        } else {
            if(convertView.getTag().getClass()!=ViewCache.class){
                inflater = activity.getLayoutInflater();
                rowView = inflater.inflate(R.layout.example_list_view_item, null);
                viewCache = new ViewCache(rowView);
                rowView.setTag(viewCache);
            }else{
                viewCache = (ViewCache) rowView.getTag();
            }
        }

        updateNormal(rowView, position);

        return rowView;
    }
    private void updateNormal(View rowView, final int position){
        ImageButton button_left = (ImageButton)rowView.findViewById(R.id.button_left);
        ImageButton button_middle = (ImageButton)rowView.findViewById(R.id.button_middle);
        ImageButton button_right = (ImageButton)rowView.findViewById(R.id.button_right);

        if(position==Math.ceil(_list.length()/3)){
            button_middle.setVisibility(_list.length()%3>1? View.VISIBLE :View.INVISIBLE);
            button_right.setVisibility(_list.length()%3>2? View.VISIBLE :View.INVISIBLE);
            try{
                showThumb(_list.getJSONObject(position*3).getString("thumb"),button_left);
                if(_list.length()%3>1){
                    showThumb(_list.getJSONObject(position*3+1).getString("thumb"),button_middle);
                }
                if(_list.length()%3>2){
                    showThumb(_list.getJSONObject(position*3+2).getString("thumb"),button_right);
                }
            }catch (Exception e){

            }
        }else{
            try{
                showThumb(_list.getJSONObject(position*3).getString("thumb"),button_left);

                showThumb(_list.getJSONObject(position*3+1).getString("thumb"),button_middle);
                showThumb(_list.getJSONObject(position*3+2).getString("thumb"),button_right);
            }catch (Exception e){

            }
        }
        button_left.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDetail(position*3);
            }
        });
        button_middle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDetail(position*3+1);
            }
        });
        button_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDetail(position*3+2);

            }
        });
    }

    private void showThumb(String file,ImageButton imageView){

        InputStream assetFile = null;
        AssetManager assets = _activity.getAssets();
        try{
            //打开指定资源对应的输入流
            assetFile = assets.open(file);

        }catch(IOException e){
            e.printStackTrace();
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inSampleSize = 1;

        Bitmap temp = BitmapFactory.decodeStream(assetFile);
        Bitmap finalImage = temp .copy(temp .getConfig(), true);
        temp.recycle();
        temp=null;
        imageView.setImageBitmap(finalImage);
    }
    private void openDetail(int postion){
        try{
            Intent detailIntent = new Intent(_activity, DetailActivity.class);
            detailIntent.putExtra("ImageURL",_list.getJSONObject(postion).getString("source"));
            _activity.startActivity(detailIntent);
        }catch(Exception e){

        }
    }
}
class ViewCache{
    private View baseView;
    public ViewCache(View baseView) {

        this.baseView = baseView;

    }
}
class FeatureViewCache{
    private View baseView;
    public FeatureViewCache(View baseView) {

        this.baseView = baseView;

    }
}