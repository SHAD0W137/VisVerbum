package com.example.visverbum.ui.history;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent; // Для запуска сервиса
import android.content.SharedPreferences;
import android.os.Bundle;
// import android.os.Handler; // Handler больше не нужен для обновления UI RecyclerView напрямую
// import android.os.Message; // Handler больше не нужен
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.visverbum.MainActivity; // Для SHARED_PREFS_NAME
import com.example.visverbum.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HistoryFragment extends Fragment implements HistoryAdapter.OnWordInteractionListener {
    private static final String TAG = "HistoryFragment";

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private List<HistoryAdapter.WordEntry> wordEntryList = new ArrayList<>();
    private Button editModeButton;
    // private Button clearAllButton; // Если решите добавить

    private String userId;
    private DatabaseReference userWordsRef;
    private ValueEventListener wordsValueEventListener; // Для прослушивания изменений в реальном времени

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPref = requireContext().getSharedPreferences(MainActivity.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        userId = sharedPref.getString(MainActivity.KEY_FIREBASE_ID, "");

        if (userId.isEmpty()) {
            Log.e(TAG, "User ID is empty. Cannot load or save words.");
            Toast.makeText(getContext(), "User ID not found. History may not work.", Toast.LENGTH_LONG).show();
            // Здесь можно предпринять действия, например, запросить логин или показать сообщение
        } else {
            userWordsRef = FirebaseDatabase.getInstance().getReference("wordlist").child(userId);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false); // Используем новый layout

        recyclerView = view.findViewById(R.id.rv_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HistoryAdapter(getContext(), wordEntryList, this);
        recyclerView.setAdapter(adapter);

        editModeButton = view.findViewById(R.id.button_edit_mode);
        editModeButton.setOnClickListener(v -> toggleEditMode());


        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Начинаем слушать изменения только после создания View
        if (userWordsRef != null) {
            attachFirebaseListener();
        }
    }

    private void toggleEditMode() {
        boolean newEditMode = !adapter.isInEditMode();
        adapter.setEditMode(newEditMode);
        editModeButton.setText(newEditMode ? "Done" : "Edit");
        // clearAllButton.setVisibility(newEditMode ? View.VISIBLE : View.GONE); // Если есть кнопка очистки
    }

    private void attachFirebaseListener() {
        wordsValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                wordEntryList.clear();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String key = snapshot.getKey();
                        String word = snapshot.getValue(String.class);
                        if (key != null && word != null) {
                            wordEntryList.add(new HistoryAdapter.WordEntry(key, word));
                        }
                    }
                    Collections.reverse(wordEntryList); // Показать последние добавленные сверху
                    Log.d(TAG, "Words loaded: " + wordEntryList.size());
                } else {
                    Log.d(TAG, "No words found in Firebase for user: " + userId);
                }
                if (adapter != null) { // Убедимся, что адаптер уже создан
                    adapter.notifyDataSetChanged();
                }
                // Обновляем состояние кнопки Edit, если список пуст
                if (editModeButton != null) {
                    editModeButton.setEnabled(!wordEntryList.isEmpty());
                    if (wordEntryList.isEmpty() && adapter.isInEditMode()) { // Если список стал пустым в режиме редактирования
                        toggleEditMode(); // Выйти из режима редактирования
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Firebase load cancelled: ", databaseError.toException());
                Toast.makeText(getContext(), "Failed to load words.", Toast.LENGTH_SHORT).show();
            }
        };
        userWordsRef.addValueEventListener(wordsValueEventListener);
    }


    @Override
    public void onDeleteClicked(HistoryAdapter.WordEntry wordEntry, int position) {
        if (userWordsRef == null || wordEntry.key == null) {
            Toast.makeText(getContext(), "Error deleting word.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Word")
                .setMessage("Are you sure you want to delete \"" + wordEntry.word + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    userWordsRef.child(wordEntry.key).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Word deleted: " + wordEntry.word);
                                // Список обновится автоматически благодаря ValueEventListener
                                // adapter.removeItem(position); // Это не нужно, если есть ValueEventListener
                                Toast.makeText(getContext(), "\"" + wordEntry.word + "\" deleted.", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to delete word: " + wordEntry.word, e);
                                Toast.makeText(getContext(), "Failed to delete.", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void confirmClearAllWords() {
        if (userWordsRef == null) return;
        if (wordEntryList.isEmpty()) {
            Toast.makeText(getContext(), "History is already empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Clear All Words")
                .setMessage("Are you sure you want to delete all saved words? This action cannot be undone.")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    userWordsRef.removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "All words cleared for user: " + userId);
                                // Список обновится автоматически благодаря ValueEventListener
                                Toast.makeText(getContext(), "All words cleared.", Toast.LENGTH_SHORT).show();
                                if (adapter.isInEditMode()) { // Выйти из режима редактирования
                                    toggleEditMode();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to clear all words for user: " + userId, e);
                                Toast.makeText(getContext(), "Failed to clear words.", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    @Override
    public void onStop() {
        super.onStop();
        // Отсоединяем слушатель, когда фрагмент не виден, чтобы избежать утечек и лишних обновлений
        if (userWordsRef != null && wordsValueEventListener != null) {
            userWordsRef.removeEventListener(wordsValueEventListener);
            Log.d(TAG, "Firebase listener removed.");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // recyclerView = null; // binding = null в вашем коде это делает для ViewBinding
        // adapter = null;
        // editModeButton = null;
        // wordEntryList.clear(); // Можно очистить, если не хотите хранить при пересоздании View
    }
}