package com.example.visverbum.ui.options;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import com.example.visverbum.LocaleHelper;
import com.example.visverbum.MainActivity;
import com.example.visverbum.R;

public class OptionsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "OptionsFragment";
    public static final String KEY_APP_LANGUAGE = "app_language";
    public static final String KEY_API_LANGUAGE = "api_language";
    public static final String KEY_AUTO_SAVE_WORDS = "auto_save_words";
    public static final String KEY_LOGOUT = "logout";

    private SharedPreferences sharedPreferences;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

        updateListPreferenceSummary(findPreference(KEY_APP_LANGUAGE));
        updateListPreferenceSummary(findPreference(KEY_API_LANGUAGE));
    }

    @Override
    public boolean onPreferenceTreeClick(@NonNull Preference preference) {
        if (preference.getKey() != null && preference.getKey().equals(KEY_LOGOUT)) {
            showLogoutConfirmationDialog();
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).logoutUser();
                    } else {
                        Toast.makeText(getContext(), getString(R.string.logout_failed_could_not_access_main_activity), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (key == null) return;

        Preference preference = findPreference(key);
        switch (key) {
            case KEY_API_LANGUAGE:
                String apiLang = sharedPreferences.getString(key, "en");
                Log.d(TAG, "API Language changed to: " + apiLang);
                if (preference instanceof ListPreference) {
                    updateListPreferenceSummary((ListPreference) preference);
                }
                break;
            case KEY_AUTO_SAVE_WORDS:
                boolean autoSave = sharedPreferences.getBoolean(key, false);
                Log.d(TAG, "Auto-save words changed to: " + autoSave);
                break;
            case KEY_APP_LANGUAGE:
                String appLang = sharedPreferences.getString(key, "en");
                Log.d(TAG, "App Language chosen: " + appLang);
                if (preference instanceof ListPreference) {
                    updateListPreferenceSummary(preference);
                }
                LocaleHelper.setNewLocale(requireActivity(), appLang);
                requireActivity().recreate();
                break;
        }
    }

    private void updateListPreferenceSummary(Preference preference) {
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            listPreference.setSummary(listPreference.getEntry());
        }
    }
}