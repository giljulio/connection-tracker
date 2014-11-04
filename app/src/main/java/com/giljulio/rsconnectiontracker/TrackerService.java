package com.giljulio.rsconnectiontracker;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;

public class TrackerService extends Service implements Response.ErrorListener, Response.Listener<Integer> {

    public static final String KEY_USERNAME = "KEY_USERNAME";
    private static final String TAG = TrackerService.class.getSimpleName();

    private NotificationManager nm;
    private Timer timer = new Timer();
    private String mUsername;
    private int mXP = -1;


    private static int ONGOING_NOTIFICATION_ID = 9202;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);

        mUsername = intent.getStringExtra(KEY_USERNAME);
        showNotification();
        timer.scheduleAtFixedRate(new TimerTask(){
            public void run() {
                onTimerTick();
            }
        }, 0, 60 * 1000L);

        return START_STICKY; // run until explicitly stopped.
    }

    private void showNotification() {
        Notification notification = new Notification(R.drawable.ic_stat_play, getText(R.string.ticker_text),
                System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, getText(R.string.notification_title),
                getText(R.string.notification_message), pendingIntent);
        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    private void onTimerTick() {
        Log.d(TAG, "onTimerTick");
        App.getInstance().addToRequestQueue(new Request<Integer>(Request.Method.GET, App.API_ROOT_URL + "index_lite.ws?player=" + mUsername, this) {

            @Override
            protected Response<Integer> parseNetworkResponse(NetworkResponse networkResponse) {
                try {
                    String body = new String(networkResponse.data, HttpHeaderParser.parseCharset(networkResponse.headers));
                    String[] rows = body.split("\n");
                    String[] cols = rows[0].split(",");
                    return Response.success(Integer.valueOf(cols[2]), HttpHeaderParser.parseCacheHeaders(networkResponse));
                } catch (UnsupportedEncodingException e) {
                    return Response.error(new ParseError(e));
                } catch (Exception e) {
                    return Response.error(new ParseError(e));
                }
            }

            @Override
            protected void deliverResponse(Integer o) {
                onResponse(o);
            }
        }, TAG);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        App.getInstance().cancelPendingRequests(TAG);
        if (timer != null) {timer.cancel();}
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onErrorResponse(VolleyError volleyError) {
        Log.d(TAG, volleyError.toString());
    }

    @Override
    public void onResponse(Integer xp) {
        Log.d(TAG, "onResponse " + xp);
        if(mXP == -1)
            mXP = xp;

        if(mXP != xp){
            notifyLoggedOff(xp - mXP);
            mXP = xp;
        }
    }

    private void notifyLoggedOff(int xpChange){
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_play)
                        .setContentTitle("Account Logged off")
                        .setContentText(xpChange + " xp has been gained")
                        .setSound(alarmSound);
        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent =
            PendingIntent.getActivity(
                    this,
                    0,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );

        int mNotificationId = 213;
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());

    }
}
