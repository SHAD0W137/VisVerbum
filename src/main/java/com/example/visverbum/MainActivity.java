package com.example.visverbum;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.example.visverbum.auth.LoginActivity;
import com.example.visverbum.service.FloatingButtonService;
import com.example.visverbum.ui.history.HistoryFragment;
import com.example.visverbum.ui.home.HomeFragment;
import com.example.visverbum.ui.options.OptionsFragment;
import com.example.visverbum.ui.wordtest.WordtestFragment;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    public static final String SHARED_PREFS_NAME = "VisVerbumPrefs";
    public static final String KEY_FIREBASE_ID = "FirebaseId";
    public static final String KEY_FLOATING_SERVICE_USER_ACTIVE = "FLOATING_SERVICE_USER_ACTIVE";
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        final ActionBar actionBar = getSupportActionBar();
        FragmentManager fragmentManager = getSupportFragmentManager();

        if (savedInstanceState == null) {
            Fragment initialFragment = new HomeFragment();
            if (actionBar != null) {
                actionBar.setTitle(getString(R.string.title_home));
            }
            fragmentManager.beginTransaction()
                    .replace(R.id.frame_layout, initialFragment)
                    .commit();
            navView.setSelectedItemId(R.id.navigation_home);
        }

        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        navView.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            CharSequence title = item.getTitle();

            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                fragment = new HomeFragment();
            } else if (id == R.id.navigation_dashboard) {
                fragment = new HistoryFragment();
            } else if (id == R.id.navigation_wordtest) {
                fragment = new WordtestFragment();
            } else if (id == R.id.navigation_options) {
                fragment = new OptionsFragment();
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

        String userId;
        if (currentUser != null) {
            userId = currentUser.getUid();
            Log.d(TAG, "Firebase Auth User ID: " + userId);
            SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(MainActivity.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(MainActivity.KEY_FIREBASE_ID, userId);
            editor.apply();
        } else {
            Log.w(TAG, "Firebase Auth: User not authenticated.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");

        ComponentName componentName = new ComponentName("com.example.visverbum",
                "com.example.visverbum.service.ToolbarActivity");
        PackageManager packageManager = getPackageManager();
        SharedPreferences sharedPref = getSharedPreferences("VisVerbumPrefs", Context.MODE_PRIVATE);
        Intent serviceIntent = new Intent(this, FloatingButtonService.class);
        stopService(serviceIntent);
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        sharedPref.edit().putBoolean("FLOATING_SERVICE_USER_ACTIVE", false).apply();
    }

    public void logoutUser() {
        Log.d(TAG, "logoutUser: Attempting to logout.");
        mAuth.signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Google Sign-Out successful.");
            } else {
                Log.w(TAG, "Google Sign-Out failed.", task.getException());
            }
            SharedPreferences sharedPref = getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.remove(KEY_FIREBASE_ID);
            editor.putBoolean(KEY_FLOATING_SERVICE_USER_ACTIVE, false);
            editor.apply();
            Log.d(TAG, "User specific SharedPreferences cleared.");

            Intent floatingButtonIntent = new Intent(this, com.example.visverbum.service.FloatingButtonService.class);
            stopService(floatingButtonIntent);
            Intent wordDefinitionIntent = new Intent(this, com.example.visverbum.service.WordDefinitionService.class);
            stopService(wordDefinitionIntent);
            Log.d(TAG, "Stopped active services.");

            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            Log.d(TAG, "Redirected to LoginActivity.");
        });
    }
}