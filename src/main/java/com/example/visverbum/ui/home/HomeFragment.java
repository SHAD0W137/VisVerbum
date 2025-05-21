package com.example.visverbum.ui.home;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.visverbum.R;
import com.example.visverbum.databinding.FragmentHomeBinding;
import com.example.visverbum.service.FloatingButtonService;
import com.example.visverbum.service.TextAccessibilityService;

import java.util.List;
import java.util.Objects;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private static final int OVERLAY_PERMISSION_REQ_CODE = 1234;

    private SharedPreferences sharedPref;
    private Button mainButton;
    private FragmentHomeBinding binding;
    ComponentName componentName;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPref = requireContext().getSharedPreferences("VisVerbumPrefs", Context.MODE_PRIVATE);

        componentName = new ComponentName("com.example.visverbum",
                "com.example.visverbum.service.ToolbarActivity");
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        mainButton = root.findViewById(R.id.main_button);

        updateButtonState();

        mainButton.setOnClickListener(v -> toggleServices());

        return root;
    }

    private void toggleServices() {
        boolean shouldBeActive = !sharedPref.getBoolean("FLOATING_SERVICE_USER_ACTIVE", false);
        PackageManager packageManager = requireActivity().getPackageManager();
        if (shouldBeActive) {
            if (!checkOverlayPermission()) {
                requestOverlayPermission();
                return;
            }
            if (!isAccessibilityServiceEnabled(requireContext(), TextAccessibilityService.class)) {
                Toast.makeText(requireContext(), "Пожалуйста, включите VisVerbum Accessibility Service", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
                return;
            }
            if (!NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
                Toast.makeText(requireContext(), "Пожалуйста, включите уведомления для VisVerbum", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
                startActivity(intent);
                return;
            }

            startFloatingService();
            packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            sharedPref.edit().putBoolean("FLOATING_SERVICE_USER_ACTIVE", true).apply();
        } else {
            stopFloatingService();
            packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            sharedPref.edit().putBoolean("FLOATING_SERVICE_USER_ACTIVE", false).apply();
        }
        updateButtonState();
    }

    private void updateButtonState() {
        if (sharedPref.getBoolean("FLOATING_SERVICE_USER_ACTIVE", false) &&
                isAccessibilityServiceEnabled(requireContext(), TextAccessibilityService.class) &&
                checkOverlayPermission() &&
                NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
            mainButton.setText("VISVERBUM IS ACTIVE");
        } else {
            mainButton.setText("VISVERBUM IS INACTIVE");
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        updateButtonState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void startFloatingService() {
        Intent serviceIntent = new Intent(requireContext(), FloatingButtonService.class);
        ContextCompat.startForegroundService(requireContext(), serviceIntent);
    }

    private void stopFloatingService() {
        Intent serviceIntent = new Intent(requireContext(), FloatingButtonService.class);
        requireContext().stopService(serviceIntent);
    }

    private boolean checkOverlayPermission() {
        return Settings.canDrawOverlays(requireContext());
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + requireContext().getPackageName()));
        startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
    }

    public static boolean isAccessibilityServiceEnabled(Context context, Class<?> accessibilityServiceClass) {
        ComponentName expectedComponentName = new ComponentName(context, accessibilityServiceClass);
        String enabledServicesSetting = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

        if (enabledServicesSetting == null) {
            return false;
        }

        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        colonSplitter.setString(enabledServicesSetting);

        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            ComponentName enabledService = ComponentName.unflattenFromString(componentNameString);
            if (enabledService != null && enabledService.equals(expectedComponentName)) {
                AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
                if (am != null) {
                    List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
                    for (AccessibilityServiceInfo serviceInfo : enabledServices) {
                        if (serviceInfo.getId().equals(expectedComponentName.flattenToString())) {
                            return true;
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Settings.canDrawOverlays(requireContext())) {
                updateButtonState();
            } else {
                Toast.makeText(requireContext(), "Разрешение наложения не предоставлено.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}