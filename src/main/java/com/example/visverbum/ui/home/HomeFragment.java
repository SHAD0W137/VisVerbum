package com.example.visverbum.ui.home;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.visverbum.MainActivity;
import com.example.visverbum.R;
import com.example.visverbum.databinding.FragmentHomeBinding;
import com.example.visverbum.service.ToolbarActivity;

public class HomeFragment extends Fragment {
    Boolean SERVICE_ACTIVE = false;
    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        SharedPreferences sharedPref = getContext().getSharedPreferences("SERVICE_ACTIVE", Context.MODE_PRIVATE);
        SharedPreferences.Editor SPeditor = sharedPref.edit();

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        Button main_button = root.findViewById(R.id.main_button);

        if(sharedPref.getBoolean("SERVICE_ACTIVE",false)) {
            main_button.setText("VISVERBUM IS ACTIVE");
        }
        else main_button.setText("VISVERBUM IS INACTIVE");


        Intent serviceIntent = new Intent();

        String packageName = "com.example.visverbum";  // Get your app's package name
        String className = "com.example.visverbum.service.ToolbarActivity";  // Replace with your activity class name

        ComponentName componentName = new ComponentName(packageName, className);
        main_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SERVICE_ACTIVE = sharedPref.getBoolean("SERVICE_ACTIVE", false);
                if (!SERVICE_ACTIVE){
                    SERVICE_ACTIVE = true;
                    main_button.setText("VISVERBUM IS ACTIVE");
                    PackageManager packageManager = getActivity().getPackageManager();
                    packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                }
                else {
                    SERVICE_ACTIVE = false;
                    main_button.setText("VISVERBUM IS INACTIVE");
                    PackageManager packageManager = getActivity().getPackageManager();
                    packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                }
                SPeditor.putBoolean("SERVICE_ACTIVE", SERVICE_ACTIVE);
                SPeditor.apply();
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}