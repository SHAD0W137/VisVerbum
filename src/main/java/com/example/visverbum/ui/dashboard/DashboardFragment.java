package com.example.visverbum.ui.dashboard;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.visverbum.R;
import com.example.visverbum.databinding.FragmentDashboardBinding;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DashboardFragment extends Fragment {
    Fragment f = this;
    static RecyclerView recyclerView;
    Handler h;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);
        Button clear_but = (Button) view.findViewById(R.id.clear_button);
        clear_but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    clearList(getContext(), view);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        h = new Handler(getContext().getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == 1){
                    update(view);
                }
            }
        };

        recyclerView = (RecyclerView) view.findViewById(R.id.rv);

        List<String> dataList = null;
        try {
            dataList = readList(getContext());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        RecyclerAdapter adapter = new RecyclerAdapter(dataList);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        h.sendEmptyMessage(1);
    }

    public static List<String> readList(Context context) throws IOException {
        List<String> words = new ArrayList<>();
        File file = new File(context.getCacheDir(), "wordlist.txt");

        if (!file.exists()) {
            return words;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                words.add(line);
            }
        }
        return words;
    }

    public static void update(View view){
        recyclerView = (RecyclerView) view.findViewById(R.id.rv);

        List<String> dataList = null;
        try {
            dataList = readList(view.getContext());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        RecyclerAdapter adapter = new RecyclerAdapter(dataList);

        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        recyclerView.setAdapter(adapter);
    }

    public static void clearList(Context context, View view) throws IOException {
        File file = new File(context.getCacheDir(), "wordlist.txt");
        if (file.exists()) {
            file.delete();
        }
        update(view);
    }
}