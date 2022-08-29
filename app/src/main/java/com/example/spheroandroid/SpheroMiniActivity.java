package com.example.spheroandroid;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.google.android.material.navigation.NavigationBarView;

public class SpheroMiniActivity extends AppCompatActivity {

    public static final String TAG = "SpheroMiniActivity";

    private String deviceAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sphero_mini);

        deviceAddress = getIntent().getStringExtra(DeviceRecyclerAdapter.EXTRA_DEVICE_ADDRESS);

        // Set control fragment dropdown options
        Spinner spinner = findViewById(R.id.spinner_controlFragment);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.control_options_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.i(TAG, (String)spinner.getItemAtPosition(i));

                if(adapter.getItem(0).equals(spinner.getItemAtPosition(i).toString())) {
                    displayFragmentTouchscreen();
                } else {
                    displayFragmentGamepad();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.i(TAG, "Nothing selected");
            }
        });

        // Display initial fragment
        // Only create the initial fragment when the activity is first created
        if(savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.fragmentContainerView, TouchscreenControlFragment.class, null)
                    .commit();
        }
    }

    private void displayFragmentTouchscreen() {
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragmentContainerView, TouchscreenControlFragment.class, null)
                .commit();
    }
    private void displayFragmentGamepad() {
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragmentContainerView, GamepadControlFragment.class, null)
                .commit();
    }
}