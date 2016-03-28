package com.sam_chordas.android.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.db.chart.listener.OnEntryClickListener;
import com.db.chart.model.ChartEntry;
import com.db.chart.model.LineSet;
import com.db.chart.model.Point;
import com.db.chart.view.ChartView;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;

public class StockDetailActivity extends AppCompatActivity {

    static Quote currentQuote;
    TextView dateView;
    TextView priceView;
    TextView changeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);
        getSupportActionBar().show();

        // get symbol
        Intent intent = getIntent();
        String symbol = intent.getStringExtra(MyStocksActivity.INTENT_SYMBOL);

        // init
        ((TextView) findViewById(R.id.detail_symbol)).setText(symbol);
        dateView = (TextView) findViewById(R.id.detail_date);
        priceView = (TextView) findViewById(R.id.detail_bid);
        changeView = (TextView) findViewById(R.id.detail_change);

        // query for all data of the symbol
        Cursor cursor = getContentResolver().query(
                QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns.BIDPRICE, QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.CREATED, QuoteColumns.ISUP},
                QuoteColumns.SYMBOL + "= ?",
                new String[]{symbol},
                null
        );

        final int COLUMN_PRICE = 0;
        final int COLUMN_PERCENT = 1;
        final int COLUMN_CHANGE = 2;
        final int COLUMN_CREATED = 3;
        final int COLUMN_ISUP = 4;

        final int DAYS_SHOWN_IN_GRAPH = 7;

        // select last entry of each date in database
        PriorityQueue<Date> dates = new PriorityQueue<>();
        if (cursor != null && cursor.moveToFirst())
            do dates.add(Utils.parseStringToDate(cursor.getString(COLUMN_CREATED)));
            while (cursor.moveToNext());

        List<String> select = Utils.selectData(dates);

        // show max 7 days of entries
        if (select.size() > DAYS_SHOWN_IN_GRAPH)
            select = select.subList(select.size() - DAYS_SHOWN_IN_GRAPH, select.size());

        // create Quote objects for display
        final ArrayList<Quote> data = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst())
            do if (select.contains(cursor.getString(COLUMN_CREATED)))
                data.add(new Quote(
                        cursor.getString(COLUMN_PRICE),
                        cursor.getString(COLUMN_PERCENT),
                        cursor.getString(COLUMN_CHANGE),
                        cursor.getString(COLUMN_CREATED),
                        cursor.getString(COLUMN_ISUP))
                );
            while (cursor.moveToNext());

        // set current quote to the last entry which is the current date
        // (before touching points in the graph)
        currentQuote = data.get(data.size() - 1);
        showQuoteInfo(currentQuote);

        // collect data for the line chart
        String[] labels = new String[data.size()];
        float[] floats = new float[data.size()];

        // retain max price to calc the appropriate step
        float maxPrice = 0f;

        for (int i = 0; i < data.size(); i++) {
            labels[i] = data.get(i).getDate(DateFormat.SHORT);
            floats[i] = data.get(i).getFloatPrice();
            if (floats[i] > maxPrice) maxPrice = floats[i];
        }

        // customize chart
        LineChartView chart = (LineChartView) findViewById(R.id.lineChart);
        Paint paint = new Paint();
        paint.setColor(getResources().getColor(android.R.color.white));
        chart.setGrid(ChartView.GridType.FULL, paint);
        chart.setStep(
                ((int) maxPrice / 10) / 10 * 10 == 0
                        ? 10
                        : ((int) maxPrice / 10) / 10 * 10
        );

        // customize line
        LineSet dataSet = new LineSet(labels, floats);
        dataSet.setDotsColor(getResources().getColor(android.R.color.holo_red_dark));
        dataSet.setDotsRadius(10);
        dataSet.setColor(getResources().getColor(android.R.color.holo_green_light));
        chart.addData(dataSet);

        // show chart
        chart.show();

        // add contentDescription for the data points
        ArrayList<Point> points = new ArrayList<>();
        for (ChartEntry c : dataSet.getEntries()) points.add((Point) c);

        FrameLayout frame = (FrameLayout) findViewById(R.id.frame);
        for (int i = 0; i < points.size(); i++) {
            int x = (int) points.get(i).getX();
            int y = (int) points.get(i).getY();
            Rect r = new Rect(x - 1, y + 1, x + 1, y - 1);
            PointView v = new PointView(StockDetailActivity.this, r);
            v.setContentDescription(data.get(i).toString());
            v.setFocusable(true);
            frame.addView(v);
        }

        // set entry onclick
        chart.setOnEntryClickListener(new OnEntryClickListener() {
            @Override
            public void onClick(int setIndex, int entryIndex, Rect entryRect) {
                currentQuote = data.get(entryIndex);
                showQuoteInfo(currentQuote);
            }
        });
    }

    private void showQuoteInfo(Quote q) {
        dateView.setText(q.getDate(DateFormat.MEDIUM));
        priceView.setText(q.getPrice());
        setChangeView(q, Utils.showPercent);
    }

    private void setChangeView(Quote q, boolean isPercent) {
        if (isPercent) changeView.setText(q.getPercent());
        else changeView.setText(q.getChange());
        if (q.isUp()) changeView.setBackgroundResource(R.drawable.percent_change_pill_green);
        else changeView.setBackgroundResource(R.drawable.percent_change_pill_red);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_change_units) {
            // change units
            Utils.showPercent = !Utils.showPercent;
            setChangeView(currentQuote, Utils.showPercent);
        }

        return super.onOptionsItemSelected(item);
    }

    private class Quote {
        String price;
        String percent;
        String change;
        String date;
        boolean isUp;

        public Quote(String price, String percent, String change, String date, String isUp) {
            this.price = price;
            this.percent = percent;
            this.change = change;
            this.date = date;
            this.isUp = isUp.equals("1");
        }

        public String getPrice() {
            return price;
        }

        public String getPercent() {
            return percent;
        }

        public boolean isUp() {
            return isUp;
        }

        public String getChange() {
            return change;
        }

        public float getFloatPrice() {
            return Float.parseFloat(price);
        }

        public float getFloatChange() {
            return Float.parseFloat(change);
        }

        public String getDate(int length) {
            return DateFormat.getDateInstance(length, Locale.getDefault())
                    .format(Utils.parseStringToDate(date));
        }

        // for contentDescription
        @Override
        public String toString() {
            return getResources().getString(R.string.point_date)
                    + getDate(DateFormat.MEDIUM) + ", "
                    + getResources().getString(R.string.point_price)
                    + price + ", "
                    + (isUp ? getResources().getString(R.string.point_up)
                    : getResources().getString(R.string.point_down))
                    + Math.abs(getFloatPrice())
                    + getResources().getString(R.string.point_which_is)
                    + getPercent().substring(1);
        }
    }

    private class PointView extends View {
        Rect rect;
        Paint paint;

        public PointView(Context context, Rect r) {
            super(context);
            rect = r;
            paint = new Paint();
            paint.setColor(Color.TRANSPARENT);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(Color.TRANSPARENT);
            canvas.drawRect(rect, paint);
        }
    }

}
