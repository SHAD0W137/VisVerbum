package com.example.visverbum.ui.dashboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class DashboardFragment extends Fragment {
    Fragment f = this;
    static RecyclerView recyclerView;

    Handler h;
    static String userId;
    static List<String> dataList = null;
    Handler getIdHandler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        SharedPreferences sharedPref = getContext().getSharedPreferences("FirebaseId", Context.MODE_PRIVATE);
        userId = sharedPref.getString("FirebaseId", "");


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
        h = new Handler(getContext().getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == 1) {
                    update(view);
                }
            }
        };


        update(view);
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

    public interface StringCallback {
        void onStringFetch(List<String> s);

    }

    public static void readList(Context context, StringCallback callback) throws IOException {

        DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference();
        List<String> words;


        mDatabase.child("wordlist").child(userId).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e("firebase", "Error getting data", task.getException());
                } else {
                    try{
                    HashMap<String, String> hashMap = (HashMap<String, String>) task.getResult().getValue();
                    Log.d("firebase", hashMap.toString());
                    List<String> tmp = new ArrayList<>(hashMap.values());
                    callback.onStringFetch(tmp);
                    } catch (Exception e){ callback.onStringFetch(new ArrayList<>());};
                }
            }
        });

    }

    public static void update(View view) {
        recyclerView = (RecyclerView) view.findViewById(R.id.rv);
        try {
            readList(view.getContext(), s -> {
                dataList = s;
                RecyclerAdapter adapter = new RecyclerAdapter(dataList);

                recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
                recyclerView.setAdapter(adapter);
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void clearList(Context context, View view) throws IOException {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("wordlist").child(userId);
        myRef.setValue(null);
        update(view);
    }
}