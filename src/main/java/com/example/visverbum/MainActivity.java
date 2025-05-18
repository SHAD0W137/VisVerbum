package com.example.visverbum;

// import android.content.ComponentName; // Если убираем логику с ToolbarActivity
// import android.content.pm.PackageManager; // Если убираем логику с ToolbarActivity
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log; // Для логов
// import android.view.View; // Не используется

// import com.example.visverbum.service.ToolbarActivity; // Если не используется
import com.example.visverbum.ui.dashboard.DashboardFragment;
import com.example.visverbum.ui.home.HomeFragment;
import com.example.visverbum.ui.wordtest.WordtestFragment;
// import com.google.android.gms.ads.identifier.AdvertisingIdClient; // Не используется в этом коде
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
// import androidx.navigation.NavController; // Не используется, т.к. ручное управление
// import androidx.navigation.Navigation; // Не используется
// import androidx.navigation.ui.AppBarConfiguration; // Не используется
// import androidx.navigation.ui.NavigationUI; // Не используется

// import com.example.visverbum.databinding.ActivityMainBinding; // Не используется в этом коде
import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity"; // Тег для логов

    // Имя для SharedPreferences, используйте его везде
    public static final String SHARED_PREFS_NAME = "VisVerbumPrefs";
    public static final String KEY_FIREBASE_ID = "FirebaseId";
    // Ключ для состояния сервисов, как в HomeFragment
    // public static final String KEY_FLOATING_SERVICE_USER_ACTIVE = "FLOATING_SERVICE_USER_ACTIVE";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Убедитесь, что R.layout.activity_main существует и корректен

        BottomNavigationView navView = findViewById(R.id.nav_view);
        final ActionBar actionBar = getSupportActionBar(); // Сделаем final, если используется только в лямбде
        FragmentManager fragmentManager = getSupportFragmentManager();

        // Устанавливаем начальный фрагмент (HomeFragment) и заголовок
        if (savedInstanceState == null) { // Чтобы не пересоздавать фрагмент при повороте, если он уже есть
            Fragment initialFragment = new HomeFragment();
            if (actionBar != null) {
                actionBar.setTitle(getString(R.string.title_home)); // Предполагая, что у вас есть такой ресурс
            }
            fragmentManager.beginTransaction()
                    .replace(R.id.frame_layout, initialFragment) // Убедитесь, что R.id.frame_layout - это ваш контейнер для фрагментов
                    .commit();
            navView.setSelectedItemId(R.id.navigation_home); // Устанавливаем выбранный элемент
        }


        navView.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            CharSequence title = item.getTitle(); // Получаем заголовок из элемента меню

            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                fragment = new HomeFragment();
            } else if (id == R.id.navigation_dashboard) {
                fragment = new DashboardFragment();
            } else if (id == R.id.navigation_wordtest) {
                fragment = new WordtestFragment();
            } else if (id == R.id.navigation_options) {
                // fragment = new OptionsFragment(); // Раскомментируйте, если есть
                // title = "Options"; // Если у элемента нет заголовка или нужно кастомное
            }

            if (fragment != null) {
                if (actionBar != null) {
                    actionBar.setTitle(title);
                }
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.frame_layout, fragment);
                // fragmentTransaction.addToBackStack(null); // Рассмотрите, если нужна навигация "назад" по фрагментам
                fragmentTransaction.commit();
                return true; // Важно вернуть true, если элемент обработан
            }
            return false; // Если элемент не обработан
        });

        // Получение Firebase ID
        new Thread(() -> {
            // super.run(); // Не нужно для анонимного Thread, если не переопределяем run() из Runnable
            try {
                FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(getApplicationContext());
                String userId = mFirebaseAnalytics.getFirebaseInstanceId(); // Это getAppInstanceId(), а не getFirebaseInstanceId() для Analytics
                // Или если вам нужен Firebase Installation ID:
                // FirebaseInstallations.getInstance().getId().addOnCompleteListener(task -> {
                // if (task.isSuccessful() && task.getResult() != null) {
                // String installationId = task.getResult();
                // SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
                // SharedPreferences.Editor editor = sharedPref.edit();
                // editor.putString(KEY_FIREBASE_ID, installationId);
                // editor.apply();
                // Log.d(TAG, "Firebase Installation ID saved: " + installationId);
                // } else {
                // Log.e(TAG, "Failed to get Firebase Installation ID", task.getException());
                // }
                // });

                // Для Firebase Analytics App Instance ID:

            } catch (Exception e) {
                Log.e(TAG, "Error getting Firebase ID: " + e.getMessage());
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
        // Логика, связанная с ToolbarActivity, если она все еще нужна, должна быть здесь.
        // Но сброс состояния FLOATING_SERVICE_USER_ACTIVE здесь не нужен.
    }
}