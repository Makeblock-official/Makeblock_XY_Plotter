package com.makeblock.xystage.views;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

import com.makeblock.xystage.adapters.ExampleListAdapter;

/**
 * Created by indream on 13-6-7.
 */

public class ExampleListView extends ListView {
    private Context _context;
    ExampleListAdapter _adapter;
    public ExampleListView(Context context,AttributeSet attributeSet){
        super(context,attributeSet);
        _context = context;
        init();
    }
    private void init(){
        if (isInEditMode()) {
            return;
        }

        this.setItemsCanFocus(true);
        _adapter = new ExampleListAdapter((Activity)_context,this);
        this.setAdapter(_adapter);

    }
}
