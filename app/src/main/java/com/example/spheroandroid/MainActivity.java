package com.example.spheroandroid;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.material.shape.InterpolateOnScrollPositionChangeHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AppCompatActivity";
    private static final String FILENAME_ADDRESSES = "addresses.txt";
    private static final int NUM_DEVICES = 2;

    String[] deviceNames, deviceStatuses, deviceAddresses;
    RecyclerView recyclerView_main;

    public ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent intent = result.getData();
                        String address = intent.getStringExtra(DeviceRecyclerAdapter.EXTRA_DEVICE_ADDRESS);
                        int index = intent.getIntExtra(DeviceRecyclerAdapter.EXTRA_DEVICE_INDEX, 0);
                        deviceAddresses[index] = address;
                        deviceStatuses[index] = "Address: " + address;
                        ((DeviceRecyclerAdapter.DeviceViewHolder)recyclerView_main.findViewHolderForAdapterPosition(index)).refresh();
                        Log.i(TAG, "Got RESULT_OK from AddressActivity with address " + address);
                        // Save address list.
                        try {
                            File file = new File(getFilesDir(), FILENAME_ADDRESSES);
                            FileOutputStream fos = new FileOutputStream(file);
                            try {
                                OutputStreamWriter writer = new OutputStreamWriter(fos);
                                for(int i = 0; i < NUM_DEVICES; i++) {
                                    writer.write(deviceAddresses[i]);
                                    if( i < NUM_DEVICES - 1)
                                        writer.write('\n');
                                }
                                writer.close();
                                fos.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceNames = getResources().getStringArray(R.array.sphero_devices);
        deviceStatuses = getResources().getStringArray(R.array.sphero_statuses);
        deviceAddresses = new String[] {"", ""};
        recyclerView_main = findViewById(R.id.recyclerView_main);

        // Get saved MAC addresses from file storage
        try {
            File file = new File(getFilesDir(), FILENAME_ADDRESSES);
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader inputStreamReader = new InputStreamReader(fis, StandardCharsets.UTF_8);
            try(BufferedReader reader = new BufferedReader(inputStreamReader)) {
                String line = reader.readLine();
                int i = 0;
                while(line != null && i < NUM_DEVICES) {
                    deviceAddresses[i] = line;
                    if(line.length() > 0) {
                        deviceStatuses[i] = "Address: " + line;
                    }
                    line = reader.readLine();
                    i++;
                }
                fis.close();
            } catch (IOException e) {
                Log.e(TAG, "IOException occurred during reading MAC addresses from address file.");
            }
        } catch (FileNotFoundException e) {
            // File does not exist. Use empty addresses.
        }

        DeviceRecyclerAdapter adapter = new DeviceRecyclerAdapter(this, deviceNames, deviceStatuses, deviceAddresses);
        recyclerView_main.setAdapter(adapter);
        recyclerView_main.setLayoutManager(new LinearLayoutManager(this));

        setSupportActionBar(findViewById(R.id.toolbar));

    }

    public void onClickButton_reset(View view) {
        try {
            File file = new File(getFilesDir(), FILENAME_ADDRESSES);
            FileOutputStream fos = new FileOutputStream(file);
            try {
                OutputStreamWriter writer = new OutputStreamWriter(fos);

                String[] statuses = getResources().getStringArray(R.array.sphero_statuses);
                for(int i = 0; i < NUM_DEVICES; i++) {
                    deviceStatuses[i] = statuses[i];
                    deviceAddresses[i] = "";
                    ((DeviceRecyclerAdapter.DeviceViewHolder)recyclerView_main.findViewHolderForAdapterPosition(i)).refresh();

                    writer.write(deviceAddresses[i]);
                    if( i < NUM_DEVICES - 1)
                        writer.write('\n');
                }
                writer.close();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        // Update and save MAC addresses
//        String deviceAddress = data.getStringExtra(DeviceRecyclerAdapter.EXTRA_DEVICE_ADDRESS);
//        int deviceIndex = data.getIntExtra(DeviceRecyclerAdapter.EXTRA_DEVICE_INDEX, -1);
//        if(deviceIndex < 0 || deviceIndex >= NUM_DEVICES) {
//            Log.e(TAG, "Activity result that did not have an index in range: " + deviceIndex);
//        } else {
//            deviceAddresses[deviceIndex] = deviceAddress;
//            deviceStatuses[deviceIndex] = "MAC Address: " + deviceAddress;
//        }
//    }
}