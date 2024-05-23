package com.example.visverbum.service;

import static android.app.PendingIntent.getActivity;
import static android.content.Intent.getIntent;
import static android.content.Intent.getIntentOld;
import static android.content.Intent.parseUri;

import static androidx.core.app.NotificationCompat.getExtras;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.visverbum.R;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import kotlinx.coroutines.ThreadContextElement;

public class WordDefinitionService extends Service{
    private WindowManager windowManager;
    private View definitionView;
    public String selectedWord;
    String definitions_answer;
    static String userId;

    List<String> list = new ArrayList<>();
    Handler mainHandler, h, errorHandler, getIdHandler;

    @Override
    public void onCreate() {
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("FirebaseId", Context.MODE_PRIVATE);
        userId = sharedPref.getString("FirebaseId", "");
        h = new Handler(getApplicationContext().getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == 1){
                    update();
                }
            }
        };

        errorHandler = new Handler(getApplicationContext().getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == 1){
                    Toast.makeText(getApplicationContext(), "No definition found", Toast.LENGTH_SHORT).show();
                }
            }
        };

        mainHandler = new Handler(getApplicationContext().getMainLooper());
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
        definitionView = inflater.inflate(R.layout.word_definition_layout, null);


        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        windowManager.addView(definitionView, params);

        Button closeButton = definitionView.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeView();
                stopSelf();
            }
        });

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        selectedWord = intent.getStringExtra("selectedWord");
        getDefinition(selectedWord);
        return START_NOT_STICKY;
    }


    private void update(){
        TextView definitionTextView = definitionView.findViewById(R.id.definitionText);
        TextView titleTextView = definitionView.findViewById(R.id.titleText);
        titleTextView.setText(selectedWord.toUpperCase());
        definitionTextView.setText(definitions_answer);
    }

    private void getDefinition(String word) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Accessing API
                    String url = "https://api.dictionaryapi.dev/api/v2/entries/en/" + word;

                    URL obj = new URL(url);
                    HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                    con.setRequestMethod("GET");

                    int responseCode = con.getResponseCode();
                    System.out.println("Response Code: " + responseCode);

                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONArray jsonResponse = new JSONArray(response.toString());

                    System.out.println("JSON Response:\n" + jsonResponse.toString());

                    for (int i = 0; i < jsonResponse.getJSONObject(0).getJSONArray("meanings").length(); i++) {
                        list.add(jsonResponse.getJSONObject(0).getJSONArray("meanings").getJSONObject(i).getString("partOfSpeech"));
                        list.add(jsonResponse.getJSONObject(0).getJSONArray("meanings").getJSONObject(i).getJSONArray("definitions").
                                getJSONObject(0).getString("definition"));

                    }
                    new Thread(new Runnable() {
                        // Creating text answer
                        @Override
                        public void run() {
                            definitions_answer = "";
                            for (int i = 0; i < list.size(); i++) {
                                definitions_answer += ("[" + list.get(i) + "]");
                                definitions_answer += "\n";
                                i++;
                                definitions_answer += list.get(i);
                                definitions_answer += "\n";
                            }
                            h.sendEmptyMessage(1);
                            try {
                                putList(selectedWord, getApplicationContext());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }).start();
                } catch (Exception e) {
                    errorHandler.sendEmptyMessage(1);
                    onDestroy();
                    e.printStackTrace();
                }

            }
        }).start();
        return;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeView();
        stopSelf();
    }

    private void removeView() {
        if (definitionView != null) {
            windowManager.removeView(definitionView);
            definitionView = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void putList(String word, Context context) throws IOException {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("wordlist").child(userId);

        String key = myRef.push().getKey();
        myRef.child(key).setValue(word);
    }

}