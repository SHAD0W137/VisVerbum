package com.example.visverbum.ui.wordtest;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.visverbum.MainActivity;
import com.example.visverbum.R;
import com.example.visverbum.service.WordDefinitionService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WordtestFragment extends Fragment {

    private static final String TAG = "WordtestFragment";

    private TextView tvCurrentWord;
    private Button btnNextWord;
    private Button btnShowDefinition;
    private TextView tvNoWordsMessage;

    private final List<String> savedWordsList = new ArrayList<>();
    private String currentTestWord = null;
    private final Random random = new Random();

    private String userId;
    private DatabaseReference userWordsRef;
    private ValueEventListener wordsValueEventListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPref = requireContext().getSharedPreferences(MainActivity.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        userId = sharedPref.getString(MainActivity.KEY_FIREBASE_ID, "");

        if (userId.isEmpty()) {
            Log.e(TAG, "User ID is empty. Cannot load words for test.");
        } else {
            userWordsRef = FirebaseDatabase.getInstance().getReference("wordlist").child(userId);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wordtest, container, false);

        tvCurrentWord = view.findViewById(R.id.tvCurrentWord);
        btnNextWord = view.findViewById(R.id.btnNextWord);
        btnShowDefinition = view.findViewById(R.id.btnShowDefinition);
        tvNoWordsMessage = view.findViewById(R.id.tvNoWordsMessage);

        btnNextWord.setOnClickListener(v -> showNextRandomWord());

        btnShowDefinition.setOnClickListener(v -> {
            if (currentTestWord != null && !currentTestWord.isEmpty()) {
                Intent definitionIntent = new Intent(getContext(), WordDefinitionService.class);
                definitionIntent.putExtra("selectedWord", currentTestWord);
                try {
                    requireContext().startService(definitionIntent);
                } catch (Exception e) {
                    Log.e(TAG, "Could not start WordDefinitionService: " + e.getMessage());
                    Toast.makeText(getContext(), "Error showing definition.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        updateUIBasedOnWordList();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (userWordsRef != null) {
            attachFirebaseListener();
        } else {
            updateUIBasedOnWordList();
        }
    }

    private void attachFirebaseListener() {
        if (wordsValueEventListener != null && userWordsRef != null) {
            userWordsRef.removeEventListener(wordsValueEventListener);
        }

        wordsValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                savedWordsList.clear();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String word = snapshot.getValue(String.class);
                        if (word != null && !word.isEmpty()) {
                            savedWordsList.add(word);
                        }
                    }
                    Log.d(TAG, "Words loaded for test: " + savedWordsList.size());
                } else {
                    Log.d(TAG, "No words found in Firebase for user: " + userId);
                }
                updateUIBasedOnWordList();
                if (savedWordsList.isEmpty()) {
                    currentTestWord = null;
                    tvCurrentWord.setText(R.string.press_next_word);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Firebase load cancelled for test: ", databaseError.toException());
                Toast.makeText(getContext(), getString(R.string.failed_to_load_words_for_test), Toast.LENGTH_SHORT).show();
                savedWordsList.clear();
                updateUIBasedOnWordList();
            }
        };
        userWordsRef.addValueEventListener(wordsValueEventListener);
    }

    private void showNextRandomWord() {
        if (savedWordsList.isEmpty()) {
            currentTestWord = null;
            updateUIBasedOnWordList();
            Toast.makeText(getContext(), "No saved words to test.", Toast.LENGTH_SHORT).show();
            return;
        }

        int randomIndex = random.nextInt(savedWordsList.size());
        currentTestWord = savedWordsList.get(randomIndex);
        tvCurrentWord.setText(currentTestWord);
        btnShowDefinition.setVisibility(View.VISIBLE);
        tvNoWordsMessage.setVisibility(View.GONE);
    }

    private void updateUIBasedOnWordList() {
        if (isAdded() && getContext() != null) {
            if (savedWordsList.isEmpty()) {
                tvCurrentWord.setText(R.string.no_saved_words_yet);
                btnNextWord.setEnabled(false);
                btnShowDefinition.setVisibility(View.GONE);
                tvNoWordsMessage.setVisibility(View.VISIBLE);
            } else {
                btnNextWord.setEnabled(true);
                if (currentTestWord == null) {
                    tvCurrentWord.setText(R.string.press_next_word);
                    btnShowDefinition.setVisibility(View.GONE);
                }
                tvNoWordsMessage.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (userWordsRef != null && wordsValueEventListener != null) {
            userWordsRef.removeEventListener(wordsValueEventListener);
            Log.d(TAG, "WordtestFragment: Firebase listener removed.");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        tvCurrentWord = null;
        btnNextWord = null;
        btnShowDefinition = null;
        tvNoWordsMessage = null;
    }
}