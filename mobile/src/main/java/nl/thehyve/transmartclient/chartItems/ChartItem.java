package nl.thehyve.transmartclient.chartItems;

import android.content.Context;
import android.view.View;

import com.github.mikephil.charting.data.ChartData;

/**
 * Created by Ward Weistra on 01-12-14.
 * Copyright (c) 2015 The Hyve B.V.
 * This code is licensed under the GNU Lesser General Public License,
 * version 3, or (at your option) any later version.
 */

public abstract class ChartItem {
    
    protected static final int TYPE_BARCHART = 0;
    protected static final int TYPE_LINECHART = 1;
    protected static final int TYPE_PIECHART = 2;
    
    protected ChartData<?> mChartData;
    
    public ChartItem(ChartData<?> cd) {
        this.mChartData = cd;      
    }
    
    public abstract int getItemType();
    
    public abstract View getView(int position, View convertView, Context c);
}
