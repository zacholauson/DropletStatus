package com.zacholauson.dropletstatus;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LiveCardService extends Service {

    private static final String LIVE_CARD_TAG = "DropletLiveCardService";

    private LiveCard    mLiveCard;
    private RemoteViews mLiveCardView;
    private final Handler mHandler = new Handler();
    private static final long DELAY_MILLIS = 300000;
    private final UpdateLiveCardRunnable mUpdateLiveCardRunnable =
              new UpdateLiveCardRunnable();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mLiveCard == null) {
            mLiveCard = new LiveCard(this, LIVE_CARD_TAG);
            mLiveCardView = new RemoteViews(getPackageName(), R.layout.live_card);
            mLiveCard.setViews(mLiveCardView);
            setMenuIntent(new Intent(this, LiveCardMenuActivity.class), mLiveCard);
            mLiveCard.publish(PublishMode.REVEAL);
            displayMessage(mLiveCard, mLiveCardView, "Fetching Droplet Data");
            mHandler.post(mUpdateLiveCardRunnable);

        } else {
            mLiveCard.navigate();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mLiveCard != null && mLiveCard.isPublished()) {
            mUpdateLiveCardRunnable.setStop(true);
            mLiveCard.unpublish();
            mLiveCard = null;
        }
        super.onDestroy();
    }

    private void setMenuIntent(Intent menuIntent, LiveCard liveCard) {
        menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK);
        liveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));
    }

    private void displayMessage(LiveCard liveCard, RemoteViews liveCardView, String message) {
        liveCardView.setTextViewText(R.id.messages, message);
        liveCard.setViews(liveCardView);
    }

    private class UpdateLiveCardRunnable implements Runnable {

        private boolean mIsStopped = false;

        public void run() {
            if (!isStopped()) {
                new DropletFetcher().execute();
                mHandler.postDelayed(mUpdateLiveCardRunnable, DELAY_MILLIS);
            }
        }

        public boolean isStopped() {
            return mIsStopped;
        }

        public void setStop(boolean isStopped) {
            this.mIsStopped = isStopped;
        }
    }

    private class DropletFetcher extends AsyncTask<String, Long, JSONArray> {
        protected JSONArray doInBackground(String... urls) {
            try {
                HttpRequest request = HttpRequest.get("https://api.digitalocean.com/v2/droplets").basic(getString(R.string.digital_ocean_token), "");
                JSONArray droplets = null;
                if (request.ok()) {
                    try {
                        JSONObject responseObject = new JSONObject(request.body());
                        droplets = responseObject.getJSONArray("droplets");
                    } catch ( JSONException e) {
                        Log.e("JSON EXCEPTION: ", request.body(), e);
                    }
                }
                return droplets;
            } catch (HttpRequest.HttpRequestException exception) {
                Log.e("JSON EXCEPTION: ", "FAILED", exception);
                return null;
            }
        }

        protected void onPostExecute(JSONArray droplets) {
            if (droplets != null) {
                for (int i = 0; i < droplets.length(); i++) {
                    try {
                        JSONObject dropletObject = droplets.getJSONObject(i);
                        String name   = dropletObject.getString("name");
                        String status = dropletObject.getString("status");
                        mLiveCardView.setTextViewText(R.id.messages, null);
                        switch(i) {
                            case 0:
                                mLiveCardView.setTextViewText(R.id.dropletName_0, name);
                                mLiveCardView.setTextViewText(R.id.dropletStatus_0, status);
                                if (status.equals("active")) {
                                    mLiveCardView.setTextColor(R.id.dropletStatus_0, Color.GREEN);
                                } else {
                                    mLiveCardView.setTextColor(R.id.dropletStatus_0, Color.RED);
                                }
                                break;
                            case 1:
                                mLiveCardView.setTextViewText(R.id.dropletName_1, name);
                                mLiveCardView.setTextViewText(R.id.dropletStatus_1, status);
                                if (status.equals("active")) {
                                    mLiveCardView.setTextColor(R.id.dropletStatus_1, Color.GREEN);
                                } else {
                                    mLiveCardView.setTextColor(R.id.dropletStatus_1, Color.RED);
                                }
                                break;
                            case 2:
                                mLiveCardView.setTextViewText(R.id.dropletName_2, name);
                                mLiveCardView.setTextViewText(R.id.dropletStatus_2, status);
                                if (status.equals("active")) {
                                    mLiveCardView.setTextColor(R.id.dropletStatus_2, Color.GREEN);
                                } else {
                                    mLiveCardView.setTextColor(R.id.dropletStatus_2, Color.RED);
                                }
                                break;
                            case 3:
                                mLiveCardView.setTextViewText(R.id.dropletName_3, name);
                                mLiveCardView.setTextViewText(R.id.dropletStatus_3, status);
                                if (status.equals("active")) {
                                    mLiveCardView.setTextColor(R.id.dropletStatus_3, Color.GREEN);
                                } else {
                                    mLiveCardView.setTextColor(R.id.dropletStatus_3, Color.RED);
                                }
                                break;
                            case 4:
                                mLiveCardView.setTextViewText(R.id.dropletName_4, name);
                                mLiveCardView.setTextViewText(R.id.dropletStatus_4, status);
                                if (status.equals("active")) {
                                    mLiveCardView.setTextColor(R.id.dropletStatus_4, Color.GREEN);
                                } else {
                                    mLiveCardView.setTextColor(R.id.dropletStatus_4, Color.RED);
                                }
                                break;
                        }
                    } catch (JSONException exception) {
                        Log.e("JSON EXCEPTION: ", "FAILED PARSING", exception);
                    }
                }
                mLiveCard.setViews(mLiveCardView);
            } else {
                Log.d("droplets", "Download failed");
            }
        }

        private void colorizeDropletStatus(RemoteViews liveCardView, String status, Integer statusLayout) {
            if (status.equals("active")) {
                liveCardView.setTextColor(statusLayout, Color.GREEN);
            } else if (status.equals("off")) {
                liveCardView.setTextColor(statusLayout, Color.RED);
            } else if (status.equals("new")) {
                liveCardView.setTextColor(statusLayout, Color.GRAY);
            } else {
                liveCardView.setTextColor(statusLayout, Color.YELLOW);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
