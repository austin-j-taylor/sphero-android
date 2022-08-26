package com.example.spheroandroid;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    String[] deviceNames, deviceStatuses;
    RecyclerView recyclerView_main;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceNames = getResources().getStringArray(R.array.sphero_devices);
        deviceStatuses = getResources().getStringArray(R.array.sphero_statuses);
        recyclerView_main = findViewById(R.id.recyclerView_main);

        DeviceRecyclerAdapter adapter = new DeviceRecyclerAdapter(this, deviceNames, deviceStatuses);
        recyclerView_main.setAdapter(adapter);
        recyclerView_main.setLayoutManager(new LinearLayoutManager(this));

        setSupportActionBar(findViewById(R.id.toolbar));
    }
}