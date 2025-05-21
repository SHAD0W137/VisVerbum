package com.example.visverbum.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.visverbum.LocaleHelper;
import com.example.visverbum.MainActivity;
import com.example.visverbum.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private TextInputEditText etRegisterEmail, etRegisterPassword, etRegisterConfirmPassword;
    private ProgressBar progressBarRegister;

    private FirebaseAuth mAuth;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.setLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        etRegisterEmail = findViewById(R.id.etRegisterEmail);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etRegisterConfirmPassword = findViewById(R.id.etRegisterConfirmPassword);
        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvGoToLogin = findViewById(R.id.tvGoToLogin);
        progressBarRegister = findViewById(R.id.progressBarRegister);

        btnRegister.setOnClickListener(v -> registerUser());
        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            // finish();
        });
    }

    private void registerUser() {
        String email = Objects.requireNonNull(etRegisterEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(etRegisterPassword.getText()).toString().trim();
        String confirmPassword = Objects.requireNonNull(etRegisterConfirmPassword.getText()).toString().trim();

        if (TextUtils.isEmpty(email)) {
            etRegisterEmail.setError(getString(R.string.error_email_is_required));
            etRegisterEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etRegisterEmail.setError(getString(R.string.error_enter_a_valid_email));
            etRegisterEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etRegisterPassword.setError(getString(R.string.error_password_is_required));
            etRegisterPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etRegisterPassword.setError(getString(R.string.error_min_password));
            etRegisterPassword.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(confirmPassword)) {
            etRegisterConfirmPassword.setError(getString(R.string.error_confirm_password));
            etRegisterConfirmPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            etRegisterConfirmPassword.setError(getString(R.string.error_passwords_do_not_match));
            etRegisterConfirmPassword.requestFocus();
            return;
        }

        progressBarRegister.setVisibility(View.VISIBLE);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBarRegister.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification()
                                    .addOnCompleteListener(verifyTask -> {
                                        if (verifyTask.isSuccessful()) {
                                            Toast.makeText(RegisterActivity.this, getString(R.string.registration_successful_email), Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(RegisterActivity.this, getString(R.string.registration_successful_no_email), Toast.LENGTH_LONG).show();
                                        }
                                    });

                        }
                        updateUI(user);
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(RegisterActivity.this, getString(R.string.registration_failed) + Objects.requireNonNull(task.getException()).getMessage(),
                                Toast.LENGTH_LONG).show();
                        updateUI(null);
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            SharedPreferences sharedPref = getSharedPreferences(MainActivity.SHARED_PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(MainActivity.KEY_FIREBASE_ID, user.getUid());
            editor.apply();
            Log.d(TAG, "User UID " + user.getUid() + " saved to SharedPreferences after registration.");

            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finishAffinity();
        }
    }
}