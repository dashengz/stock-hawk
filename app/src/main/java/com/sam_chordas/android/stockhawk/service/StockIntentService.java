package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.R;

/**
 * Created by sam_chordas on 10/1/15.
 */
public class StockIntentService extends IntentService {

    public StockIntentService() {
        super(StockIntentService.class.getName());
    }

    public StockIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(StockIntentService.class.getSimpleName(), "Stock Intent Service");
        StockTaskService stockTaskService = new StockTaskService(this);
        Bundle args = new Bundle();
        if (intent.getStringExtra("tag").equals("add")) {
            args.putString("symbol", intent.getStringExtra("symbol"));
        }

        // We can call OnRunTask from the intent service to force it to run immediately instead of
        // scheduling a task.

        // Show toast msgs to inform users of the possible outcomes
        if (stockTaskService.onRunTask(new TaskParams(intent.getStringExtra("tag"), args))
                == GcmNetworkManager.RESULT_FAILURE) {
            // Not valid stock input
            final String input = intent.getStringExtra("symbol").toUpperCase();
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), input +
                            getApplicationContext().getResources()
                                    .getString(R.string.invalid_toast), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Valid input
            if (intent.getStringExtra("tag").equals("add")) {
                final String input = intent.getStringExtra("symbol").toUpperCase();
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), input +
                                getApplicationContext().getResources()
                                        .getString(R.string.valid_toast), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }
}
