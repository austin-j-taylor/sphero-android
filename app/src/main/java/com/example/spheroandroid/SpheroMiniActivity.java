package com.example.spheroandroid;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.Set;

// Hosts the control fragments (GamepadControlFragment and TouchscreenControlFragment) to get
// input information from them and send it to the Sphero.
// Handles connecting to the sphero and reading the battery level.
// Handles calling the sphero API functions (SpheroMiniController).
public class SpheroMiniActivity extends AppCompatActivity {

    public static final String TAG = "SpheroMiniActivity";
    // Range in battery voltages for the sphero, used to approximate battery charge % from voltage.
    // May need to be tuned for your own sphero, but it's approximating a nonlinear curve anyway.
    private final static double SPHERO_BATTERY_MIN = 3.45;
    private final static double SPHERO_BATTERY_MAX = 4.15;

    private BluetoothAdapter bluetoothAdapter;
    private SpheroController sphero;
    private SpheroMiniViewModel viewModel;
    private String deviceAddress;

    private boolean isStopped;

    // Views
    private ConstraintLayout constraintLayout_connected;
    private ToggleButton button_connect;
    private ImageButton button_battery;
    private TextView text_battery;

    // Broadcast receiver for bluetooth callback actions.
    // If a new action is added, make sure to add it to the filter in onCreate.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
//                case BluetoothDevice.ACTION_ACL_CONNECTED:
//                    if (ActivityCompat.checkSelfPermission(SpheroMiniActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                        finishPermissionsRejected();
//                    }
//                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                    if ("Pro Controller".equals(device.getName())) {
//
//                    }
//                    break;
                case SpheroController.ACTION_BATTERY_AVAILABLE:
                    double vbatt = intent.getDoubleExtra(SpheroController.EXTRA_BATTERY_VALUE, 0);
                    if(vbatt < SPHERO_BATTERY_MIN)
                        vbatt = SPHERO_BATTERY_MIN;
                    if(vbatt > SPHERO_BATTERY_MAX)
                        vbatt = SPHERO_BATTERY_MAX;
                    // Convert into approximate percentage
                    vbatt = 100 * (vbatt - SPHERO_BATTERY_MIN) / (SPHERO_BATTERY_MAX - SPHERO_BATTERY_MIN);
                    text_battery.setText(String.format("~%d", (int)vbatt) + "%");
                    break;
            }
            // TODO: on device connected callback: check if pro controller is now connected...?
        }
    };
    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher, as an instance variable.
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // TODO enable connect button here
                    initBluetooth();
                } else {
                    finishPermissionsRejected();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sphero_mini);

        deviceAddress = getIntent().getStringExtra(DeviceRecyclerAdapter.EXTRA_DEVICE_ADDRESS);
        isStopped = true;

        initViewModel();
        initUI(savedInstanceState);
        initBroadcasts();
        if (checkBluetoothPermissions()) {
            initBluetooth();
        }
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(SpheroMiniViewModel.class);
        viewModel.getConnectionState().observe(this, connectionState -> observeChangeConnectionState());
        viewModel.getSpeed().observe(this, speed -> observeChangeSpeed());
        viewModel.getLedBrightness().observe(this, ledBrightness -> observeChangeLedBrightness());
        viewModel.getLedColor().observe(this, ledColor -> observeChangeLedColor());
        viewModel.getAwake().observe(this, awake -> observeChangeAwake());
        viewModel.getResettingHeading().observe(this, resettingHeading -> observeChangeResettingHeading());
        viewModel.getRollY().observe(this, rollY -> observeChangeRoll());

        // Set initial view model fields
        viewModel.setConnectionState(SpheroMiniViewModel.ConnectionState.DISCONNECTED);
        viewModel.setSpeed(100);
        viewModel.setLedBrightness(100);
        viewModel.setLedColor(100);
        viewModel.setAwake(false);
        viewModel.setResettingHeading(false);
    }

    private void initUI(Bundle savedInstanceState) {

        constraintLayout_connected = findViewById(R.id.constraintLayout_connected);
        button_connect = findViewById(R.id.button_connect);
        button_battery = findViewById(R.id.button_battery);
        text_battery = findViewById(R.id.text_battery);

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
                Log.i(TAG, (String) spinner.getItemAtPosition(i));

                if (adapter.getItem(0).equals(spinner.getItemAtPosition(i).toString())) {
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
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.fragmentContainerView, TouchscreenControlFragment.class, null)
                    .commit();
        }

        button_connect.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                // Start connecting
                viewModel.setConnectionState(SpheroMiniViewModel.ConnectionState.CONNECTING);
            } else {
                // Start disconnecting
                viewModel.setConnectionState(SpheroMiniViewModel.ConnectionState.DISCONNECTING);
            }
        });
    }

    private void initBroadcasts() {
        // Register for broadcasts when bluetooth devices are discovered/connected/etc.,
        // and for receiving data sent by the sphero (like battery updates)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_UUID);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(SpheroController.ACTION_BATTERY_AVAILABLE);
        registerReceiver(receiver, filter);
    }

    private boolean checkBluetoothPermissions() {
        // Request bluetooth permissions
        boolean permissionsReady = true;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            permissionsReady = false;
            // TODO disable connect button here
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            permissionsReady = false;
            // TODO disable connect button here
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN);
        }
        // If we have the permissions ready, set up the bluetooth now.
        // If not, wait for the callback to set up bluetooth.
        return permissionsReady;
    }

    private void initBluetooth() {
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            // TODO dialog "Device doesn't support bluetooth"
            finish();
        }
        if (bluetoothAdapter.isEnabled()) {
            initSphero();
        } else {
            // Bluetooth is disabled. Launch a prompt to enable it, then initialize sphero if done.
            ActivityResultLauncher<Intent> turnOnBTResultLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == Activity.RESULT_OK) {
                                initSphero();
                            }
                        }
                    });
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            turnOnBTResultLauncher.launch(enableBtIntent);
        }
    }

    private void initSphero() {
        sphero = new SpheroController();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(receiver);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            if (bluetoothAdapter != null)
                bluetoothAdapter.cancelDiscovery();
        }
        if (sphero != null) {
            sphero.destroy();
            sphero = null;
        }
    }

    // Called when a permissions check fails, preventing normal operation of the app.
    private void finishPermissionsRejected() {
        // TODO: dialog box saying permissions rejected
        finish();
    }

    private boolean checkIfControllerPaired() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            finishPermissionsRejected();
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
//                String deviceHardwareAddress = device.getAddress(); // MAC address
                if("Pro Controller".equals(deviceName)) {
                    return true;
                }
            }
        }
        return false;
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
                constraintLayout_connected.setVisibility(View.VISIBLE);
                button_connect.setEnabled(false);
                button_connect.setChecked(true);
                // Communicate with sphero API to connect to it




                break;
            case CONNECTED:
                constraintLayout_connected.setVisibility(View.VISIBLE);
                button_connect.setEnabled(true);
                button_connect.setChecked(true);
                // Send configuration commands



                break;
            case DISCONNECTING:
                constraintLayout_connected.setVisibility(View.INVISIBLE);
                button_connect.setEnabled(false);
                button_connect.setChecked(false);
                break;
        }
    }
    private void observeChangeSpeed() {
        // TODO  Control fragment changed the speed. Keep track of it for future movement commands.
    }
    private void observeChangeLedBrightness() {
        // TODO  Control fragment changed LED brightness. Send it to the sphero.
    }
    private void observeChangeLedColor() {
        // TODO  Control fragment changed LED Color. Send the new color to the sphero.
    }
    private void observeChangeAwake() {
        // TODO process awake
    }
    private void observeChangeResettingHeading() {
        // TODO sphero.start/stopaiming
    }
    private void observeChangeRoll() {
        // TODO  Control fragment joystick has been changed, causing rollX and rollY to update as a pair.
        // Send new command to sphero.
        // Stop sending commands after a [0, 0] is received (an implicit "stop" command).
        // Commands received from the view model are in range [-1, +1] and a radius of 1
        // sphero.stopMovingSphero

//                // Send movement event to sphero
//                x = 2 * 256 * x; // a radius of length 256 or a radius of length 1, however you feel
//                y = 2 * 256 * y;
//                int heading = (int) Math.toDegrees(Math.atan2(y, x)) + 90;
//                if (heading < 0)
//                    heading += 360;
//                int speed = (int) Math.sqrt(x * x + y * y);
//                if (speed > 255)
//                    speed = 255;
////            Log.i(TAG, "");
////            Log.i(TAG, x + ", " + y);
////            Log.i(TAG, heading + ", " + speed);
//
//                sphero.rollSphero(speed, heading);
    }


    public void onClickButton_battery(View view) {

    }

}