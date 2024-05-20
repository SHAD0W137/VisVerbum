package com.example.visverbum.service;

import android.app.Activity;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class ToolbarActivity extends Activity {
    boolean is_active;
    Activity tbActivity = this;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CharSequence text = getIntent().getStringExtra(Intent.EXTRA_PROCESS_TEXT);

        if (text != null) {
            startWordDefinitionService(text.toString());
        } else {
            Toast.makeText(this, "No text selected", Toast.LENGTH_SHORT).show();
        }

        finish();
    }
    private void startWordDefinitionService(String selectedText){
        Intent serviceIntent = new Intent(this, WordDefinitionService.class);
        serviceIntent.putExtra("selectedWord", selectedText);
        startService(serviceIntent);
    }
}

