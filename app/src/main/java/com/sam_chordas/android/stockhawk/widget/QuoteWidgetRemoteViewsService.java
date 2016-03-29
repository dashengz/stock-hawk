package com.sam_chordas.android.stockhawk.widget;

import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;

/**
 * Created by Jonathan on 3/28/16.
 * RemoteViews Service for the Collection Widget
 */
public class QuoteWidgetRemoteViewsService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            final int COLUMN_ID = 0;
            final int COLUMN_SYMBOL = 1;
            final int COLUMN_PRICE = 2;
            final int COLUMN_PERCENT = 3;
            final int COLUMN_ISUP = 4;
            private Cursor data = null;

            @Override
            public void onCreate() {
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) data.close();
                final long token = Binder.clearCallingIdentity();
                data = getContentResolver().query(
                        QuoteProvider.Quotes.CONTENT_URI,
                        new String[]{
                                QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                                QuoteColumns.PERCENT_CHANGE, QuoteColumns.ISUP
                        },
                        QuoteColumns.ISCURRENT + "= ?",
                        new String[]{"1"},
                        null
                );
                Binder.restoreCallingIdentity(token);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION || data == null || !data.moveToPosition(position))
                    return null;
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_collection_item);
                String symbol = data.getString(COLUMN_SYMBOL);
                String price = data.getString(COLUMN_PRICE);
                String percent = data.getString(COLUMN_PERCENT);
                boolean isUp = data.getInt(COLUMN_ISUP) == 1;
                views.setTextViewText(R.id.stock_symbol, symbol);
                views.setTextViewText(R.id.change, percent);
                if (isUp)
                    views.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_green);
                else
                    views.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_red);

                final Intent fillInIntent = new Intent();
                fillInIntent.putExtra(MyStocksActivity.INTENT_SYMBOL, symbol);
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);
                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_collection_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position)) return data.getLong(COLUMN_ID);
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
