package com.example.visverbum.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.visverbum.MainActivity;
import com.example.visverbum.R; // Убедитесь, что R импортируется из вашего пакета
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WordDefinitionService extends Service {
    private static final String TAG = "WordDefinitionService";

    private WindowManager windowManager;
    private View definitionView;
    private WindowManager.LayoutParams params;

    public String selectedWordGlobal;
    static String userId;

    private Map<String, List<String>> definitionsByPos = new HashMap<>();
    private List<String> partsOfSpeechOrder = new ArrayList<>();

    private Handler uiUpdateHandler;
    private Handler errorHandler;
    private ExecutorService executorService;

    private int initialXInWindow;
    private int initialYInWindow;
    private float initialTouchXOnScreen;
    private float initialTouchYOnScreen;

    private LinearLayout posTabsContainer;
    private HorizontalScrollView posTabsScrollView;
    private TextView definitionTextView;
    private TextView titleTextView;
    private Button currentlySelectedPosButton = null;

    @SuppressLint("ClickableViewAccessibility") // Для OnTouchListener на titleBar
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        executorService = Executors.newSingleThreadExecutor();

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        } else {
            userId = "";
        }

        uiUpdateHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == 1 && definitionView != null) {
                    createPosTabsAndUpdateDefinition();
                }
            }
        };

        errorHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == 1 && definitionView != null) {
                    if (definitionTextView != null && selectedWordGlobal != null) {
                        definitionTextView.setText("No definition found for \"" + selectedWordGlobal + "\" or network error.");
                    } else if (definitionTextView != null) {
                        definitionTextView.setText("An error occurred or word is invalid.");
                    }
                    if (posTabsScrollView != null) {
                        posTabsScrollView.setVisibility(View.GONE);
                    }
                }
            }
        };

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Context themeContext = new ContextThemeWrapper(this, R.style.Theme_VisVerbum);
        LayoutInflater inflater = LayoutInflater.from(themeContext);
        definitionView = inflater.inflate(R.layout.word_definition_layout, null);

        titleTextView = definitionView.findViewById(R.id.titleText);
        definitionTextView = definitionView.findViewById(R.id.definitionText);
        posTabsContainer = definitionView.findViewById(R.id.posTabsContainer);
        posTabsScrollView = definitionView.findViewById(R.id.posTabsScrollView);

        int layoutFlag;
        layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = android.view.Gravity.CENTER;

        try {
            windowManager.addView(definitionView, params);
        } catch (Exception e) {
            Log.e(TAG, "Error adding definitionView to WindowManager: " + e.getMessage());
            stopSelf();
            return;
        }

        ImageButton closeButton = definitionView.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> removeViewAndStopService());

        Button saveButton = definitionView.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            if (selectedWordGlobal != null && !selectedWordGlobal.isEmpty()) {
                saveWordToFirebase(selectedWordGlobal);
            } else {
                Toast.makeText(this, "No word to save", Toast.LENGTH_SHORT).show();
            }
        });

        Button googleSearchButton = definitionView.findViewById(R.id.googleSearchButton);
        googleSearchButton.setOnClickListener(v -> {
            if (selectedWordGlobal != null && !selectedWordGlobal.isEmpty()) {
                searchWordInGoogle(selectedWordGlobal);
            } else {
                Toast.makeText(this, "No word to search", Toast.LENGTH_SHORT).show();
            }
        });

        // Dragging
        View titleBar = definitionView.findViewById(R.id.titleBar);
        titleBar.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialXInWindow = params.x;
                    initialYInWindow = params.y;
                    initialTouchXOnScreen = event.getRawX();
                    initialTouchYOnScreen = event.getRawY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    params.x = initialXInWindow + (int) (event.getRawX() - initialTouchXOnScreen);
                    params.y = initialYInWindow + (int) (event.getRawY() - initialTouchYOnScreen);
                    try {
                        if (definitionView != null) {
                            windowManager.updateViewLayout(definitionView, params);
                        }
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "Error updating view layout during drag: " + e.getMessage());
                    }
                    return true;
            }
            return false;
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand received");
        if (definitionView == null) {
            Log.e(TAG, "definitionView is null in onStartCommand. Service might have been improperly restarted or view removed.");
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent != null) {
            String rawSelectedWord = intent.getStringExtra("selectedWord");
            selectedWordGlobal = preprocessWord(rawSelectedWord);
            Log.d(TAG, "Processed word: " + selectedWordGlobal);

            if (selectedWordGlobal != null && !selectedWordGlobal.isEmpty()) {
                if (titleTextView != null) {
                    titleTextView.setText(selectedWordGlobal.toUpperCase());
                }
                if (definitionTextView != null) {
                    definitionTextView.setText("Loading definition...");
                }
                if (posTabsContainer != null) {
                    posTabsContainer.removeAllViews();
                }
                if (posTabsScrollView != null) {
                    posTabsScrollView.setVisibility(View.GONE);
                }
                currentlySelectedPosButton = null;
                definitionsByPos.clear();
                partsOfSpeechOrder.clear();

                getDefinition(selectedWordGlobal);
            } else {
                Toast.makeText(this, "No valid word to define.", Toast.LENGTH_SHORT).show();
                removeViewAndStopService();
            }
        } else {
            Log.w(TAG, "Service started with null intent.");
            removeViewAndStopService();
        }
        return START_NOT_STICKY;
    }

    private String preprocessWord(String rawWord) {
        if (rawWord == null || rawWord.trim().isEmpty()) {
            return null;
        }
        String cleanedWord = rawWord.trim().replaceAll("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$", "");
        String[] words = cleanedWord.split("\\s+");
        if (words.length > 0 && !words[0].isEmpty()) {
            return words[0].replaceAll("[^a-zA-Z0-9-]", "");
        }
        return null;
    }

    private void createPosTabsAndUpdateDefinition() {
        if (partsOfSpeechOrder.isEmpty() || definitionsByPos.isEmpty()) {
            Log.w(TAG, "No parts of speech or definitions to create tabs.");
            errorHandler.sendEmptyMessage(1);
            return;
        }

        if (posTabsContainer == null || posTabsScrollView == null || definitionView == null) {
            Log.e(TAG, "UI components for tabs are null in createPosTabsAndUpdateDefinition.");
            return;
        }

        posTabsContainer.removeAllViews();
        currentlySelectedPosButton = null;

        Context buttonContext = new ContextThemeWrapper(this, R.style.Theme_VisVerbum);

        for (final String pos : partsOfSpeechOrder) {
            Button posButton = new Button(buttonContext, null, android.R.attr.buttonBarButtonStyle);
            posButton.setText(pos.isEmpty() ? "Other" : pos);
            posButton.setTextColor(Color.DKGRAY);
            posButton.setTypeface(null, Typeface.NORMAL);
            posButton.setAllCaps(false);
            posButton.setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4));
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            buttonParams.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            posButton.setLayoutParams(buttonParams);

            posButton.setOnClickListener(v -> {
                updateDefinitionText(pos);
                highlightSelectedTab((Button) v);
            });
            posTabsContainer.addView(posButton);

            if (currentlySelectedPosButton == null) {
                highlightSelectedTab(posButton);
                updateDefinitionText(pos);
            }
        }
        posTabsScrollView.setVisibility(View.VISIBLE);
    }

    private void highlightSelectedTab(Button selectedButton) {
        if (currentlySelectedPosButton != null) {
            currentlySelectedPosButton.setTextColor(Color.DKGRAY);
            currentlySelectedPosButton.setTypeface(null, Typeface.NORMAL);
        }
        int activeColor;
        try {
            activeColor = ContextCompat.getColor(this, R.color.red_main);
        } catch (Resources.NotFoundException e) {
            activeColor = Color.RED; // Запасной цвет
        }
        selectedButton.setTextColor(activeColor);
        selectedButton.setTypeface(null, Typeface.BOLD);
        currentlySelectedPosButton = selectedButton;
    }

    private void updateDefinitionText(String partOfSpeech) {
        if (definitionTextView == null) return;

        List<String> definitions = definitionsByPos.get(partOfSpeech);
        if (definitions != null && !definitions.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < definitions.size(); i++) {
                sb.append(i + 1).append(". ").append(definitions.get(i));
                if (i < definitions.size() - 1) {
                    sb.append("\n\n");
                }
            }
            definitionTextView.setText(sb.toString());
        } else {
            definitionTextView.setText("No definitions available for this part of speech.");
        }
    }

    private void getDefinition(final String word) {
        executorService.execute(() -> {
            try {
                String url = "https://api.dictionaryapi.dev/api/v2/entries/en/" + Uri.encode(word); // Кодируем слово для URL
                URL obj = new URL(url);
                System.setProperty("java.net.preferIPv4Stack", "true");
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("GET");
                con.setConnectTimeout(10000); // 10 секунд
                con.setReadTimeout(10000);    // 10 секунд

                int responseCode = con.getResponseCode();
                Log.d(TAG, "API for \"" + word + "\" - Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONArray jsonResponse = new JSONArray(response.toString());

                    definitionsByPos.clear();
                    partsOfSpeechOrder.clear();

                    if (jsonResponse.length() > 0) {
                        JSONObject firstEntry = jsonResponse.getJSONObject(0);
                        JSONArray meanings = firstEntry.getJSONArray("meanings");

                        for (int i = 0; i < meanings.length(); i++) {
                            JSONObject meaning = meanings.getJSONObject(i);
                            String partOfSpeech = meaning.optString("partOfSpeech", "Other"); // "Other" если нет или пустая
                            if (partOfSpeech.isEmpty()) partOfSpeech = "Other";

                            JSONArray definitionsArray = meaning.getJSONArray("definitions");

                            if (!partsOfSpeechOrder.contains(partOfSpeech)) {
                                partsOfSpeechOrder.add(partOfSpeech);
                            }

                            List<String> currentDefinitions = definitionsByPos.getOrDefault(partOfSpeech, new ArrayList<>());
                            for (int j = 0; j < definitionsArray.length(); j++) {
                                JSONObject definitionObj = definitionsArray.getJSONObject(j);
                                String defText = definitionObj.getString("definition");
                                currentDefinitions.add(defText);
                            }
                            definitionsByPos.put(partOfSpeech, currentDefinitions);
                        }

                        if (!definitionsByPos.isEmpty()) {
                            uiUpdateHandler.sendEmptyMessage(1);
                        } else {
                            Log.w(TAG, "No 'meanings' or 'definitions' found in JSON for \"" + word + "\"");
                            errorHandler.sendEmptyMessage(1);
                        }
                    } else {
                        Log.w(TAG, "Empty JSON array received for \"" + word + "\"");
                        errorHandler.sendEmptyMessage(1);
                    }
                } else {
                    Log.e(TAG, "API Error for \"" + word + "\" - Response Code: " + responseCode);
                    errorHandler.sendEmptyMessage(1);
                }
            } catch (JSONException e) {
                Log.e(TAG, "JSON Parsing Error for \"" + word + "\": " + e.getMessage(), e);
                errorHandler.sendEmptyMessage(1);
            } catch (IOException e) {
                Log.e(TAG, "Network/IO Error for \"" + word + "\": " + e.getMessage(), e);
                errorHandler.sendEmptyMessage(1);
            } catch (Exception e) {
                Log.e(TAG, "Unexpected Error in getDefinition for \"" + word + "\": " + e.getMessage(), e);
                errorHandler.sendEmptyMessage(1);
            }
        });
    }

    private void saveWordToFirebase(String word) {
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "User ID not available. Cannot save word.", Toast.LENGTH_LONG).show();
            SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("FirebaseId", Context.MODE_PRIVATE);
            userId = sharedPref.getString("FirebaseId", "");
            if (userId == null || userId.isEmpty()){
                Log.e(TAG, "Failed to retrieve User ID for Firebase saving.");
                return;
            }
        }
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("wordlist").child(userId);
        String key = myRef.push().getKey();
        if (key != null) {
            myRef.child(key).setValue(word)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Word \"" + word + "\" saved to Firebase for user " + userId);
                        Toast.makeText(WordDefinitionService.this, "\"" + word + "\" saved!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save word \"" + word + "\" to Firebase for user " + userId, e);
                        Toast.makeText(WordDefinitionService.this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } else {
            Log.e(TAG, "Failed to generate Firebase key for word: " + word);
            Toast.makeText(WordDefinitionService.this, "Failed to generate key for saving.", Toast.LENGTH_SHORT).show();
        }
    }

    private void searchWordInGoogle(String word) {
        try {
            String searchUrl = "https://www.google.com/search?q=define+" + Uri.encode(word);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Log.d(TAG, "Opened Google search for: " + word);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "ActivityNotFoundException for Google search: " + e.getMessage());
            Toast.makeText(this, "No browser found to open Google search.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Exception during Google search: " + e.getMessage());
            Toast.makeText(this, "Could not open Google search.", Toast.LENGTH_LONG).show();
        }
    }

    private void removeViewAndStopService() {
        Log.d(TAG, "removeViewAndStopService called");
        removeView();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        removeView();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
            Log.d(TAG, "ExecutorService shutdown.");
        }
    }

    private void removeView() {
        if (definitionView != null && windowManager != null) {
            try {
                windowManager.removeView(definitionView);
                Log.d(TAG, "Definition view removed from WindowManager.");
            } catch (Exception e) {
                Log.w(TAG, "Error removing definitionView: " + e.getMessage());
            }
            definitionView = null; // Важно!
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}