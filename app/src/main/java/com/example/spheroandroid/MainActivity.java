package com.example.spheroandroid;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.spheroandroid.dialog.BluetoothPermissionsDialogue;
import com.example.spheroandroid.dialog.DeleteConfigDialogue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

// Entry point for the application.
// Handles sphero device selection.
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AppCompatActivity";
    private static final String FILENAME_ADDRESSES = "addresses.txt";
    private static final int NUM_DEVICES = 2;

    String[] deviceNames, deviceStatuses, deviceAddresses;
    RecyclerView recyclerView_main;

    // Callback for when the MAC Address selection activity finishes, giving this activity
    // the new address.
    // Write it to the address file and let the recycler view know that a change has occured.
    public ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent intent = result.getData();
                        if(intent == null)
                            return;
                        int index = intent.getIntExtra(DeviceRecyclerAdapter.EXTRA_DEVICE_INDEX, 0);
                        String address = intent.getStringExtra(DeviceRecyclerAdapter.EXTRA_DEVICE_ADDRESS);
                        deviceAddresses[index] = address;
                        deviceStatuses[index] = "Address: " + address;
                        RecyclerView.ViewHolder thisHolder = recyclerView_main.findViewHolderForAdapterPosition(index);
                        if(thisHolder != null)
                            ((DeviceRecyclerAdapter.DeviceViewHolder)thisHolder).refresh();
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

    // Reset the MAC address configurations. Wipe the address file.
    public void onClickButton_reset(View view) {
        DialogFragment info = new DeleteConfigDialogue();
        info.show(getSupportFragmentManager(), "DeleteConfigDialogue");
    }

    // Called when the DeleteConfigDialogue returns, confirming to delete the address configuration.
    public void onConfigDeleteListener() {
        try {
            File file = new File(getFilesDir(), FILENAME_ADDRESSES);
            FileOutputStream fos = new FileOutputStream(file);
            try {
                OutputStreamWriter writer = new OutputStreamWriter(fos);

                String[] statuses = getResources().getStringArray(R.array.sphero_statuses);
                for(int i = 0; i < NUM_DEVICES; i++) {
                    deviceStatuses[i] = statuses[i];
                    deviceAddresses[i] = "";
                    RecyclerView.ViewHolder thisHolder = recyclerView_main.findViewHolderForAdapterPosition(i);
                    if(thisHolder != null)
                        ((DeviceRecyclerAdapter.DeviceViewHolder)thisHolder).refresh();

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