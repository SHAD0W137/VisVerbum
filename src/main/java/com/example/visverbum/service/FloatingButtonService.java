package com.example.visverbum.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.visverbum.MainActivity;
import com.example.visverbum.R;

public class FloatingButtonService extends Service {

    private WindowManager windowManager;
    private View floatingView; // FrameLayout
    private ImageButton actionButton;

    private static final String CHANNEL_ID = "FloatingButtonChannel";
    private static final int NOTIFICATION_ID = 123;
    private static final String TAG = "FloatingClickService";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

        if (inflater == null || windowManager == null) {
            stopSelf();
            return;
        }

        floatingView = inflater.inflate(R.layout.floating_button_layout, null);
        actionButton = floatingView.findViewById(R.id.floating_button);

        if (actionButton == null) {
            stopSelf();
            return;
        }

        int layoutFlag;
        layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.BOTTOM | Gravity.END;
        params.x = 30;
        params.y = 120;

        try {
            windowManager.addView(floatingView, params);
            Log.d(TAG, "Floating view added.");
        } catch (Exception e) {
            Log.e(TAG, "Error adding view to WindowManager: " + e.getMessage(), e);
            stopSelf();
            return;
        }

        actionButton.setOnClickListener(v -> {
            Log.d(TAG, "Action Button Clicked!");

            if (currentSelectedWordFromAccessibility != null && !currentSelectedWordFromAccessibility.isEmpty()) {
                Toast.makeText(FloatingButtonService.this, "Отправка: " + currentSelectedWordFromAccessibility, Toast.LENGTH_LONG).show();

                Intent definitionIntent = new Intent(FloatingButtonService.this, WordDefinitionService.class);
                definitionIntent.putExtra("selectedWord", currentSelectedWordFromAccessibility);
                try {
                    startService(definitionIntent);
                } catch (Exception e) {
                    Toast.makeText(FloatingButtonService.this, "Не удалось запустить сервис определений.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Не удалось запустить сервис определений.");
                }
            } else {
                Log.d(TAG, "Слово не выделено (или сервис доступности неактивен).");
                Toast.makeText(FloatingButtonService.this, "Слово не выделено (или сервис доступности неактивен).", Toast.LENGTH_LONG).show();
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(textSelectedReceiver,
                new IntentFilter(TextAccessibilityService.ACTION_TEXT_SELECTED));

        startMyOwnForeground();
    }

    private void startMyOwnForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(CHANNEL_ID,
                    "Floating Button Service",
                    NotificationManager.IMPORTANCE_LOW);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            Intent notificationIntent = new Intent(this, MainActivity.class); // Замените на ваше главное Activity
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Floating Button активен")
                    .setContentText("Нажмите, чтобы открыть приложение")
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setContentIntent(pendingIntent)
                    .build();
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(textSelectedReceiver);
        super.onDestroy();
        Log.d(TAG, "onDestroy in FloatingButtonService");
        if (floatingView != null && windowManager != null) {
            try {
                windowManager.removeView(floatingView);
                Log.d(TAG, "Floating view removed.");
            } catch (Exception e) {
                Log.e(TAG, "Error removing view: " + e.getMessage());
            }
            floatingView = null;
        }
        stopForeground(true);
    }

    private String currentSelectedWordFromAccessibility = ""; // Для хранения слова от AccessibilityService

    private BroadcastReceiver textSelectedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && TextAccessibilityService.ACTION_TEXT_SELECTED.equals(intent.getAction())) {
                currentSelectedWordFromAccessibility = intent.getStringExtra(TextAccessibilityService.EXTRA_SELECTED_TEXT);
                if (currentSelectedWordFromAccessibility != null && !currentSelectedWordFromAccessibility.isEmpty()) {
                    Log.d(TAG, "Received from Accessibility: " + currentSelectedWordFromAccessibility);
                    actionButton.setEnabled(true); // Пример
                    actionButton.setAlpha(1.0f); // Пример
                } else {
                    // Если пришел пустой текст, можно деактивировать кнопку
                    actionButton.setEnabled(false);
                    actionButton.setAlpha(0.5f);
                }
            }
        }
    };
}