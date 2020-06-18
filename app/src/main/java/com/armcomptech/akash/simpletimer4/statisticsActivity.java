package com.armcomptech.akash.simpletimer4;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Objects;

public class statisticsActivity extends AppCompatActivity{
    RecyclerView recyclerView;

    ArrayList<String> timerName = new ArrayList<>();
    ArrayList<Integer> count = new ArrayList<>();
    ArrayList<Integer> timeInSeconds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics_activity);
        Objects.requireNonNull(getSupportActionBar()).setTitle("   Statistics");
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.drawable.ic_data_usage_white);

        loadData();

        recyclerView = findViewById(R.id.timersRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new Adapter(this, timerName, count, timeInSeconds));
    }

    private void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        Gson gson = new Gson();

        String timerNameJson = sharedPreferences.getString("timer name", null);
        Type timerNameType = new TypeToken<ArrayList<String>>() {}.getType();
        timerName = gson.fromJson(timerNameJson, timerNameType);

        String countJson = sharedPreferences.getString("count", null);
        Type countType = new TypeToken<ArrayList<Integer>>() {}.getType();
        count = gson.fromJson(countJson, countType);

        String timeInSecondsJson = sharedPreferences.getString("timeInSeconds", null);
        Type timeInSecondsType = new TypeToken<ArrayList<Integer>>() {}.getType();
        timeInSeconds = gson.fromJson(timeInSecondsJson, timeInSecondsType);
    }
}
