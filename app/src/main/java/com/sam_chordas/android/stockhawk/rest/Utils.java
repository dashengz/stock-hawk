package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.util.Log;

import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.TimeZone;

/**
 * Created by sam_chordas on 10/8/15.
 * Utility class
 */
public class Utils {

    public static boolean showPercent = true;
    private static String LOG_TAG = Utils.class.getSimpleName();

    public static ArrayList quoteJsonToContentVals(String JSON) {
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject = null;
        JSONArray resultsArray = null;
        try {
            jsonObject = new JSONObject(JSON);
            if (jsonObject != null && jsonObject.length() != 0) {
                jsonObject = jsonObject.getJSONObject("query");
                int count = Integer.parseInt(jsonObject.getString("count"));
                String created = jsonObject.getString("created");
                if (count == 1) {
                    jsonObject = jsonObject.getJSONObject("results")
                            .getJSONObject("quote");
                    batchOperations.add(buildBatchOperation(jsonObject, created));
                } else {
                    resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

                    if (resultsArray != null && resultsArray.length() != 0) {
                        for (int i = 0; i < resultsArray.length(); i++) {
                            jsonObject = resultsArray.getJSONObject(i);
                            batchOperations.add(buildBatchOperation(jsonObject, created));
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        } catch (Exception e) {
            return null;
        }
        return batchOperations;
    }

    public static String truncateBidPrice(String bidPrice) {
        bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
        return bidPrice;
    }

    public static String truncateChange(String change, boolean isPercentChange) {
        String weight = change.substring(0, 1);
        String ampersand = "";
        if (isPercentChange) {
            ampersand = change.substring(change.length() - 1, change.length());
            change = change.substring(0, change.length() - 1);
        }
        change = change.substring(1, change.length());
        double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
        change = String.format("%.2f", round);
        StringBuffer changeBuffer = new StringBuffer(change);
        changeBuffer.insert(0, weight);
        changeBuffer.append(ampersand);
        change = changeBuffer.toString();
        return change;
    }

    public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject, String created) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.Quotes.CONTENT_URI);
        try {
            String change = jsonObject.getString("Change");
            builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
            builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
            builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
                    jsonObject.getString("ChangeinPercent"), true));
            builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
            builder.withValue(QuoteColumns.ISCURRENT, 1);
            if (change.charAt(0) == '-') {
                builder.withValue(QuoteColumns.ISUP, 0);
            } else {
                builder.withValue(QuoteColumns.ISUP, 1);
            }
            builder.withValue(QuoteColumns.CREATED, created);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return builder.build();
    }

    public static Date parseStringToDate(String created) {

        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz", Locale.getDefault());
        Date date = null;
        try {
            date = format.parse(created.replace("Z", "UTC"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static String convertDateToString(Date date) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz", Locale.ENGLISH);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(date).replace("UTC", "Z");
    }

    public static ArrayList<String> selectData(PriorityQueue<Date> dates) {
        ArrayList<String> selected = new ArrayList<>();
        while (dates.size() != 0) {
            Date date = dates.poll();
            while (dates.size() != 0 && dates.peek().getDate() == (date.getDate()))
                date = dates.poll();
            selected.add(convertDateToString(date));
        }
        return selected;
    }
}
