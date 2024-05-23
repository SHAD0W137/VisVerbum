package com.example.visverbum;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;

import com.example.visverbum.service.ToolbarActivity;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.visverbum.databinding.ActivityMainBinding;
import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new Thread(){
            @Override
            public void run() {
                super.run();
                FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(getApplicationContext());
                String userId = mFirebaseAnalytics.getFirebaseInstanceId();
                SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("FirebaseId", Context.MODE_PRIVATE);
                SharedPreferences.Editor SPeditor = sharedPref.edit();
                SPeditor.putString("FirebaseId", userId);
                SPeditor.apply();
            }
        }.start();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        String packageName = "com.example.visverbum";  // Get your app's package name
        String className = "com.example.visverbum.service.ToolbarActivity";  // Replace with your activity class name
        ComponentName componentName = new ComponentName(packageName, className);
        PackageManager packageManager = getPackageManager();
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        SharedPreferences sharedPref = getSharedPreferences("SERVICE_ACTIVE", Context.MODE_PRIVATE);
        SharedPreferences.Editor SPeditor = sharedPref.edit();
        SPeditor.putBoolean("SERVICE_ACTIVE", false);
        SPeditor.apply();
    }
}