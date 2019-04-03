package com.example.eric.wishare.model;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;

import com.example.eric.wishare.R;

public abstract class WiNotification{
    protected String mTitle;
    protected String mText;
    protected NotificationChannel mChannel;
    protected NotificationManager mNotificationManager;
    protected NotificationCompat.Builder mNotification;
    protected NotificationCompat.Builder mOldBuilder;
    protected String mChannelID;
    protected int mNotificationType;
    protected Context mContext;

    public static final int SILENT_NOTIFICATION = 0;
    public static final int REGULAR_NOTIFICATION = 1;


    public WiNotification(Context context, String title, String text, int notificationType){
        mTitle = title;
        mText = text;
        mContext = context;
        mNotificationType = notificationType;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            mChannelID = "defaultChannel";
            mChannel = new NotificationChannel(mChannelID, "defaultName",
            NotificationManager.IMPORTANCE_HIGH);
            mChannel.setDescription("This is the default channel description.");
            mChannel.setSound(null, null);
            mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.createNotificationChannel(mChannel);

            mNotification = buildSilentNotification(title, text);
            if(notificationType != SILENT_NOTIFICATION) {
                mNotification.setDefaults(Notification.DEFAULT_ALL);
            }

        } else {
            mOldBuilder = buildSilentNotification(title, text);
            if(notificationType != SILENT_NOTIFICATION) {
                mOldBuilder.setDefaults(Notification.DEFAULT_ALL);
            }
            mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }
    }

    private NotificationCompat.Builder buildSilentNotification(String title, String text) {
        return new NotificationCompat.Builder(mContext, mChannelID)
                .setSmallIcon(R.drawable.ic_wifi_black_48dp)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(null)
                .setSound(null, 0);
    }

    public String getTitle(){
        return mTitle;
    }
    public String getText(){
        return mText;
    }

    public abstract void onNotificationClick();
    public void show() {
        onNotificationClick();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            mNotificationManager.notify(1, mNotification.build());
        } else {
            mNotificationManager.notify(1, mOldBuilder.build());
        }
    }
}