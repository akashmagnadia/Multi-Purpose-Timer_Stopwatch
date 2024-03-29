package com.armcomptech.akash.simpletimer4.stopwatch;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.armcomptech.akash.simpletimer4.R;

import java.util.Objects;

import static com.App.TIMER_STOPWATCH_ID;

public class stopwatchWithService extends Service {

    BroadcastReceiver broadcastReceiver2;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        IntentFilter intentFilter2 = new IntentFilter("stopwatchPlayer");
        broadcastReceiver2 = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getStringExtra("notification");
                switch (Objects.requireNonNull(action)) {
                    case "updateNotification":
                        String timeLeft = intent.getStringExtra("timeLeft");
                        String name = intent.getStringExtra("name");
                        if (name == null) {
                            name = "";
                        }
                        showNotification(timeLeft, name);
                        break;

                    case "cancelNotification":
                        cancelNotification();
                        break;
                }
            }
        };

        registerReceiver(broadcastReceiver2, intentFilter2);
        return START_NOT_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        cleanUp();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanUp();
    }

    public void cleanUp() {
        if (broadcastReceiver2 != null) {
            try {
                unregisterReceiver(broadcastReceiver2);
            } catch (IllegalArgumentException e) {
                Log.d("Timer with Service", "unregisterReceiver");
            }
        }
        NotificationManagerCompat.from(this).cancel(2);
        stopSelf();
    }

    public void cancelNotification() {
        NotificationManagerCompat.from(this).cancel(2);
        stopSelf();
    }

    public void showNotification(String timeLeft, String currentTimerName) {

        PackageManager client = this.getPackageManager();
        final Intent notificationIntent = client.getLaunchIntentForPackage("com.armcomptech.akash.simpletimer4");

        final PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        String content;
        int timeLeftSecondsInt = (Integer.parseInt(timeLeft))/1000;
        String timeLeftFormatted = String.format("%02d:%02d:%02d", timeLeftSecondsInt / 3600,
                (timeLeftSecondsInt % 3600) / 60, (timeLeftSecondsInt % 60));
        if (currentTimerName.equals("General")) {
            content = "Stopwatch: " + timeLeftFormatted;
        } else {
            content = "Stopwatch: " + currentTimerName + " - " + timeLeftFormatted;
        }

        Notification notification = new NotificationCompat.Builder(this, TIMER_STOPWATCH_ID)
                .setSmallIcon(R.drawable.ic_baseline_timelapse_24)
                .setContentTitle(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setOngoing(false)
                .setOnlyAlertOnce(true)
                .setSound(null)
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(2, notification);
    }
}
