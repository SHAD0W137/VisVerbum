package com.example.visverbum;

// import android.content.ComponentName; // Если убираем логику с ToolbarActivity
// import android.content.pm.PackageManager; // Если убираем логику с ToolbarActivity
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log; // Для логов
// import android.view.View; // Не используется

// import com.example.visverbum.service.ToolbarActivity; // Если не используется
import com.example.visverbum.auth.LoginActivity;
import com.example.visverbum.ui.history.HistoryFragment;
import com.example.visverbum.ui.home.HomeFragment;
import com.example.visverbum.ui.wordtest.WordtestFragment;
// import com.google.android.gms.ads.identifier.AdvertisingIdClient; // Не используется в этом коде
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity"; // Тег для логов
    private String userId; // Поле класса

    public static final String SHARED_PREFS_NAME = "VisVerbumPrefs";
    public static final String KEY_FIREBASE_ID = "FirebaseId";
    public static final String KEY_FLOATING_SERVICE_USER_ACTIVE = "FLOATING_SERVICE_USER_ACTIVE";

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
                fragment = new HistoryFragment();
            } else if (id == R.id.navigation_wordtest) {
                fragment = new WordtestFragment();
            } else if (id == R.id.navigation_options) {
                // fragment = new OptionsFragment();
                // title = "Options";
            }

            if (fragment != null) {
                if (actionBar != null) {
                    actionBar.setTitle(title);
                }
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.frame_layout, fragment);
                fragmentTransaction.commit();
                return true;
            }
            return false;
        });


        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            userId = currentUser.getUid();
            Log.d(TAG, "Firebase Auth User ID: " + userId);
            SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(MainActivity.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(MainActivity.KEY_FIREBASE_ID, userId);
            editor.apply();
        } else {
            Log.w(TAG, "Firebase Auth: User not authenticated.");
            userId = "";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Log.d(TAG, "Google Sign-Out complete.");
            // Очищаем сохраненный ID
            SharedPreferences sharedPref = getSharedPreferences(MainActivity.SHARED_PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.remove(MainActivity.KEY_FIREBASE_ID); // Удаляем ID
            editor.putBoolean(MainActivity.KEY_FLOATING_SERVICE_USER_ACTIVE, false);
            editor.apply();

            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}