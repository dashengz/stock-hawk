package com.sam_chordas.android.stockhawk.ui;

import android.graphics.Paint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.db.chart.model.LineSet;
import com.db.chart.view.ChartView;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;

public class StockDetailActivity extends AppCompatActivity {

    private LineChartView mChart;

    private String[] mLabels = {"1", "2", "3", "4", "5"};
    private float[] mFloats = {3.2f, 1.2f, 5.9f, 4.3f, 9.5f};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);
        getSupportActionBar().show();

        mChart = (LineChartView) findViewById(R.id.lineChart);

        Paint paint = new Paint();
        paint.setColor(getResources().getColor(android.R.color.white));
        mChart.setGrid(ChartView.GridType.FULL, paint);

        LineSet dataSet = new LineSet(mLabels, mFloats);
        dataSet.setDotsColor(getResources().getColor(android.R.color.holo_red_dark));
        dataSet.setDotsRadius(10);
        dataSet.setColor(getResources().getColor(android.R.color.holo_green_light));
        mChart.addData(dataSet);

        mChart.show();
    }
}
