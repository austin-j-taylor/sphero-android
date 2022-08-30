package com.example.spheroandroid;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.ToggleButton;

// Hosts the control fragments (GamepadControlFragment and TouchscreenControlFragment) to get
// input information from them and send it to the Sphero.
// Handles connecting to the sphero and reading the battery level.
// Handles calling the sphero API functions (SpheroMiniController).
public class SpheroMiniActivity extends AppCompatActivity {

    public static final String TAG = "SpheroMiniActivity";

    private SpheroMiniViewModel viewModel;
    private String deviceAddress;

    private ConstraintLayout constraintLayout_connected;
    private ToggleButton button_connect;
    private ImageButton button_battery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sphero_mini);

        viewModel = new ViewModelProvider(this).get(SpheroMiniViewModel.class);
        viewModel.getConnectionState().observe(this, connectionState -> observeChangeConnectionState());
        viewModel.getSpeed().observe(this, connectionState -> observeChangeSpeed());
        viewModel.getLedBrightness().observe(this, connectionState -> observeChangeLedBrightness());
        viewModel.getLedHue().observe(this, connectionState -> observeChangeLedHue());
        deviceAddress = getIntent().getStringExtra(DeviceRecyclerAdapter.EXTRA_DEVICE_ADDRESS);

        // Set initial view model fields
        viewModel.setConnectionState(SpheroMiniViewModel.ConnectionState.DISCONNECTED);
        viewModel.setSpeed(100);
        viewModel.setLedBrightness(100);
        viewModel.setLedHue(100);
        viewModel.setAwake(false);
        viewModel.setResettingHeading(false);

        constraintLayout_connected = findViewById(R.id.constraintLayout_connected);

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

        // UI Callbacks
        button_connect = findViewById(R.id.button_connect);
        button_battery = findViewById(R.id.button_battery);

        button_connect.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if(isChecked) {
                // Start connecting
                viewModel.setConnectionState(SpheroMiniViewModel.ConnectionState.CONNECTING);
            } else {
                // Start disconnecting
                viewModel.setConnectionState(SpheroMiniViewModel.ConnectionState.DISCONNECTING);
            }
        });
    }

    private void onClickButton_battery(View view) {

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

    private void observeChangeConnectionState() {
        // Hide/display control layouts
        SpheroMiniViewModel.ConnectionState connectionState = viewModel.getConnectionState().getValue();
        if(connectionState == null)
            return;
        switch(connectionState) {
            case DISCONNECTED:
                constraintLayout_connected.setVisibility(View.INVISIBLE);
                button_connect.setEnabled(true);
                button_connect.setChecked(false);
                break;
            case CONNECTING:
                constraintLayout_connected.setVisibility(View.INVISIBLE);
                button_connect.setEnabled(false);
                button_connect.setChecked(true);
                // Communicate with sphero API to connect to it




                break;
            case CONNECTED:
                constraintLayout_connected.setVisibility(View.VISIBLE);
                button_connect.setEnabled(true);
                button_connect.setChecked(true);
                break;
            case DISCONNECTING:
                constraintLayout_connected.setVisibility(View.INVISIBLE);
                button_connect.setEnabled(false);
                button_connect.setChecked(false);
                break;
        }
    }
    private void observeChangeSpeed() {
        // Control fragment changed the speed. Keep track of it for future movement commands.
    }
    private void observeChangeLedBrightness() {
        // Control fragment changed LED brightness. Send it to the sphero.

    }
    private void observeChangeLedHue() {
        // Control fragment changed LED hue. Send the new color to the sphero.

    }
}