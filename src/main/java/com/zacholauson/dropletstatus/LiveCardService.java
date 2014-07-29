package com.zacholauson.dropletstatus;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.widget.RemoteViews;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;

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
                new DropletCardUpdater(mLiveCard, mLiveCardView).execute();
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

    protected class DropletCardUpdater extends AsyncTask<String, Long, JSONArray> {

        private LiveCard    liveCard;
        private RemoteViews liveCardView;
        private JSONArray   droplets;

        protected DropletCardUpdater(LiveCard liveCard, RemoteViews liveCardView) {
            this.liveCard = liveCard;
            this.liveCardView = liveCardView;
        }


        protected JSONArray doInBackground(String... urls) {
            try {
                HttpRequest request = HttpRequest.get("https://api.digitalocean.com/v2/droplets").basic(getString(R.string.digital_ocean_token), "");
                if (request.ok()) { droplets = new JSONObject(request.body()).getJSONArray("droplets"); }
                return droplets;
            } catch (JSONException exception) {
                exception.printStackTrace();
                return droplets = null;
            } catch (HttpRequest.HttpRequestException exception) {
                exception.printStackTrace();
                return droplets = null;
            }
        }

        protected void onPostExecute(JSONArray droplets) {
            if (droplets != null) {
                for (int i = 0; i < droplets.length(); i++) {

                    String name = null;
                    String status = null;

                    try {
                        DropletParser dropletParser = new DropletParser(droplets.getJSONObject(i));
                        name   = dropletParser.getName();
                        status = dropletParser.getStatus();
                        resetCardMessages();
                    } catch (JSONException exception) {
                        exception.printStackTrace();
                    }

                    Integer nameLayout   = getNameLayoutId(i);
                    Integer statusLayout = getStatusLayoutId(i);
                    liveCardView.setTextViewText(nameLayout, name);
                    liveCardView.setTextViewText(statusLayout, status);
                    colorizeDropletStatus(statusLayout, status);
                    liveCard.setViews(liveCardView);

                }
            } else {
                liveCardView.setTextViewText(R.id.messages, "Unable to fetch Droplet data");
            }
        }

        private Integer getNameLayoutId(Integer i) {
            return getResources().getIdentifier("dropletName_" + i, "id", getApplicationContext().getPackageName());
        }

        private Integer getStatusLayoutId(Integer i) {
            return getResources().getIdentifier("dropletStatus_" + i, "id", getApplicationContext().getPackageName());
        }

        private void resetCardMessages() {
            liveCardView.setTextViewText(R.id.messages, null);
        }

        private void colorizeDropletStatus(Integer statusLayout, String status) {
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
