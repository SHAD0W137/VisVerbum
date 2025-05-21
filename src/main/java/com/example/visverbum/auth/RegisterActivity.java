package com.example.visverbum.auth;

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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.visverbum.MainActivity;
import com.example.visverbum.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private TextInputEditText etRegisterEmail, etRegisterPassword, etRegisterConfirmPassword;
    private Button btnRegister;
    private TextView tvGoToLogin;
    private ProgressBar progressBarRegister;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        etRegisterEmail = findViewById(R.id.etRegisterEmail);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etRegisterConfirmPassword = findViewById(R.id.etRegisterConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);
        progressBarRegister = findViewById(R.id.progressBarRegister);

        btnRegister.setOnClickListener(v -> registerUser());
        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            // finish(); // Опционально
        });
    }

    private void registerUser() {
        String email = etRegisterEmail.getText().toString().trim();
        String password = etRegisterPassword.getText().toString().trim();
        String confirmPassword = etRegisterConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etRegisterEmail.setError("Email is required.");
            etRegisterEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etRegisterEmail.setError("Enter a valid email.");
            etRegisterEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etRegisterPassword.setError("Password is required.");
            etRegisterPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etRegisterPassword.setError("Password must be at least 6 characters.");
            etRegisterPassword.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(confirmPassword)) {
            etRegisterConfirmPassword.setError("Confirm password is required.");
            etRegisterConfirmPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            etRegisterConfirmPassword.setError("Passwords do not match.");
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
                                            Toast.makeText(RegisterActivity.this, "Registration successful. Verification email sent.", Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(RegisterActivity.this, "Registration successful, but failed to send verification email.", Toast.LENGTH_LONG).show();
                                        }
                                    });

                        }
                        updateUI(user);
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        updateUI(null);
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            // Сохраняем UID
            SharedPreferences sharedPref = getSharedPreferences(MainActivity.SHARED_PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(MainActivity.KEY_FIREBASE_ID, user.getUid());
            editor.apply();
            Log.d(TAG, "User UID " + user.getUid() + " saved to SharedPreferences after registration.");


            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finishAffinity(); // Закрываем все Activity, связанные с аутентификацией
        }
    }
}