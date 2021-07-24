package com.example.yink.amadeus;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

public class LaunchActivity extends AppCompatActivity {

    private static final String NOTIFICATION_CHANNEL_ID = "Amadeus Notification Channel";
    private ImageView connect, cancel, logo;
    private TextView status;
    private Boolean isPressed = false;
    private MediaPlayer m;
    private Handler aniHandle;
    private Runnable aniRunnable;

    private int i = 0;

    public boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        connect = findViewById(R.id.imageView_connect);
        cancel = findViewById(R.id.imageView_cancel);
        status = findViewById(R.id.textView_call);
        logo = findViewById(R.id.imageView_logo);

        aniHandle = aniHandle != null ? aniHandle : new Handler(Looper.getMainLooper());

        aniRunnable = () -> {
            final int DURATION = 20;
            if (i < 39) {
                i++;
                String imgName = "logo" + i;
                int id = getResources().getIdentifier(imgName, "drawable", getPackageName());
                logo.setImageDrawable((ContextCompat.getDrawable(LaunchActivity.this, id)));
                aniHandle.postDelayed(aniRunnable, DURATION);
            }
        };

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final Window win = getWindow();

        aniHandle.post(aniRunnable);

        if (!isAppInstalled(LaunchActivity.this, "com.google.android.googlequicksearchbox")) {
            status.setText(R.string.google_app_error);
        }

        if (Alarm.isPlaying()) {
            status.setText(R.string.incoming_call);
            win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }

        if (settings.getBoolean("show_notification", false)) {
            showNotification();
        }

        connect.setOnClickListener(v -> {
            if (!isPressed && isAppInstalled(LaunchActivity.this, "com.google.android.googlequicksearchbox")) {
                isPressed = true;

                connect.setImageResource(R.drawable.connect_select);

                if (!Alarm.isPlaying()) {
                    m = MediaPlayer.create(LaunchActivity.this, R.raw.tone);

                    m.setOnPreparedListener(mp -> {
                        mp.start();
                        status.setText(R.string.connecting);
                    });

                    m.setOnCompletionListener(mp -> {
                        mp.release();
                        Intent intent = new Intent(LaunchActivity.this, MainActivity.class);
                        startActivity(intent);
                    });
                } else {
                    Alarm.cancel(LaunchActivity.this);
                    win.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                    win.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

                    Intent intent = new Intent(LaunchActivity.this, MainActivity.class);
                    startActivity(intent);
                }
            }
        });

        cancel.setOnClickListener(v -> {
            cancel.setImageResource(R.drawable.cancel_select);
            Alarm.cancel(getApplicationContext());
            win.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            win.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        logo.setOnClickListener(v -> startActivity(new Intent(LaunchActivity.this,
                SettingsActivity.class)));
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LangContext.wrap(newBase));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        final Window win = getWindow();

        if (m != null) {
            m.release();
        }

        Alarm.cancel(LaunchActivity.this);
        win.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        win.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        aniHandle.removeCallbacks(aniRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isPressed) {
            status.setText(R.string.disconnected);
        } else if (!isAppInstalled(LaunchActivity.this, "com.google.android.googlequicksearchbox")) {
            status.setText(R.string.google_app_error);
        } else if (Alarm.isPlaying()) {
            status.setText(R.string.incoming_call);
        } else {
            status.setText(R.string.call);
        }

        isPressed = false;
        connect.setImageResource(R.drawable.connect_unselect);
        cancel.setImageResource(R.drawable.cancel_unselect);
    }

    private void showNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW);

            notificationChannel.setDescription(getString(R.string.app_name));
            notificationChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
                NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.xp2)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_text));
        Intent resultIntent = new Intent(LaunchActivity.this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(LaunchActivity.this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, builder.build());
    }
}
