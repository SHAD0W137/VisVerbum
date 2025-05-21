package com.example.visverbum.ui.history; // Создайте пакет adapter, если его нет

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.visverbum.R;
import com.example.visverbum.service.WordDefinitionService; // Для запуска сервиса определений

import java.util.List;
import java.util.Map; // Если будете хранить слова с их ключами

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    // Лучше хранить объекты WordEntry (слово + его Firebase ключ) для удобного удаления
    public static class WordEntry {
        public String key; // Firebase key
        public String word;

        public WordEntry(String key, String word) {
            this.key = key;
            this.word = word;
        }
    }

    private List<WordEntry> wordEntries;
    private Context context;
    private boolean isInEditMode = false;
    private OnWordInteractionListener listener;

    public interface OnWordInteractionListener {
        void onDeleteClicked(WordEntry wordEntry, int position);
        // void onDefineClicked(String word); // Можно использовать этот, если хотите обработку в фрагменте
    }

    public HistoryAdapter(Context context, List<WordEntry> wordEntries, OnWordInteractionListener listener) {
        this.context = context;
        this.wordEntries = wordEntries;
        this.listener = listener;
    }

    public void setEditMode(boolean isInEditMode) {
        this.isInEditMode = isInEditMode;
        notifyDataSetChanged(); // Перерисовать все элементы для показа/скрытия кнопки удаления
    }

    public boolean isInEditMode() {
        return isInEditMode;
    }

    public void removeItem(int position) {
        if (position >= 0 && position < wordEntries.size()) {
            wordEntries.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, wordEntries.size()); // Обновить позиции для остальных
        }
    }

    public void updateData(List<WordEntry> newWordEntries) {
        this.wordEntries.clear();
        this.wordEntries.addAll(newWordEntries);
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.dictionary_element, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WordEntry currentEntry = wordEntries.get(position);
        holder.wordTextView.setText(currentEntry.word);

        holder.deleteButton.setVisibility(isInEditMode ? View.VISIBLE : View.GONE);

        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClicked(currentEntry, holder.getAdapterPosition());
            }
        });

        holder.defineButton.setOnClickListener(v -> {
            // Вариант 1: Запуск WordDefinitionService напрямую
            Intent definitionIntent = new Intent(context, WordDefinitionService.class);
            definitionIntent.putExtra("selectedWord", currentEntry.word);
            // Если сервис уже запущен, он получит новый интент в onStartCommand
            try {
                context.startForegroundService(definitionIntent);
            } catch (Exception e) {
                Toast.makeText(context, "Could not start definition service.", Toast.LENGTH_SHORT).show();
            }

            // Вариант 2: Поиск в Google (альтернатива или дополнение)
            /*
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=define+" + Uri.encode(currentEntry.word)));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(context, "Could not open Google search.", Toast.LENGTH_SHORT).show();
            }
            */
        });
    }

    @Override
    public int getItemCount() {
        return wordEntries == null ? 0 : wordEntries.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView wordTextView;
        Button deleteButton;
        Button defineButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            wordTextView = itemView.findViewById(R.id.element_word);
            deleteButton = itemView.findViewById(R.id.element_delete);
            defineButton = itemView.findViewById(R.id.element_define);
        }
    }
}