package com.example.spheroandroid;

import static com.example.spheroandroid.SpheroController.ConnectionState;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.example.spheroandroid.dialog.BluetoothPermissionsDialogue;
import com.example.spheroandroid.dialog.BluetoothSupportDialogue;
import com.example.spheroandroid.dialog.FailedScanDialogue;

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
    private int lastHeading;
    private boolean displayingPermissionsRejectionDialogue;

    // Views
    private ConstraintLayout constraintLayout_connected;
    private ToggleButton button_connect;
    private TextView text_battery, text_connetionStatus;

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
                case SpheroController.ACTION_GATT_CONNECTED:
                    text_connetionStatus.setText(R.string.discovering_servies);
                    break;
                case SpheroController.ACTION_GATT_SERVICES_DISCOVERED:
                    text_connetionStatus.setText(R.string.initializing);
                    break;
                case SpheroController.ACTION_BATTERY_AVAILABLE:
                    double vbatt = intent.getDoubleExtra(SpheroController.EXTRA_BATTERY_VALUE, 0);
                    if(vbatt < SPHERO_BATTERY_MIN)
                        vbatt = SPHERO_BATTERY_MIN;
                    if(vbatt > SPHERO_BATTERY_MAX)
                        vbatt = SPHERO_BATTERY_MAX;
                    // Convert into approximate percentage
                    vbatt = 100 * (vbatt - SPHERO_BATTERY_MIN) / (SPHERO_BATTERY_MAX - SPHERO_BATTERY_MIN);
                    text_battery.setText(String.format("~%d%%", (int)vbatt));
                    break;
                case SpheroController.ACTION_SPHERO_CONNECTION_STATE_CHANGE:
                    // Set view model state to the new state
                    ConnectionState state = (ConnectionState)intent.getSerializableExtra(SpheroController.EXTRA_SPHERO_CONNECTION_STATE);
                    viewModel.setConnectionState(state);
                    break;
                case SpheroController.ACTION_SCAN_FAILED:
                    // Show dialog for failing to find sphero.
                    // Dismissing the box will return us to the DISCONNECTED state.
                    DialogFragment info = new FailedScanDialogue();
                    info.show(getSupportFragmentManager(), "FailedScanDialogue");
                    break;
            }
        }
    };

    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog.
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
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
        lastHeading = 90;
        displayingPermissionsRejectionDialogue = false;

        initViewModel();
        initUI(savedInstanceState);
        initBroadcasts();
        if (checkBluetoothPermissions()) {
            initBluetooth();
        }

    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(SpheroMiniViewModel.class);

        // Set observers
        viewModel.getConnectionState().observe(this, connectionState -> observeChangeConnectionState());
        viewModel.getSpeed().observe(this, speed -> observeChangeSpeed());
        viewModel.getLedBrightness().observe(this, ledBrightness -> observeChangeLedBrightness());
        viewModel.getLedColor().observe(this, ledColor -> observeChangeLedColor());
        viewModel.getAwake().observe(this, awake -> observeChangeAwake());
        viewModel.getResettingHeading().observe(this, resettingHeading -> observeChangeResettingHeading());
        viewModel.getRollY().observe(this, rollY -> observeChangeRoll());
    }

    private void initUI(Bundle savedInstanceState) {

        constraintLayout_connected = findViewById(R.id.constraintLayout_connected);
        button_connect = findViewById(R.id.button_connect);
        text_connetionStatus = findViewById(R.id.text_connectionStatus);
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
        // And hide it until we connect to the sphero
        MainActivity.setViewAndChildrenEnabled(constraintLayout_connected, false);

        button_connect.setOnCheckedChangeListener(this::onCheckedChanged_connect);
    }

    private void initBroadcasts() {
        // Register for broadcasts when bluetooth devices are discovered/connected/etc.,
        // and for receiving data sent by the sphero (like battery updates)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(SpheroController.ACTION_GATT_CONNECTED);
        filter.addAction(SpheroController.ACTION_GATT_SERVICES_DISCOVERED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_UUID);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(SpheroController.ACTION_BATTERY_AVAILABLE);
        filter.addAction(SpheroController.ACTION_SPHERO_CONNECTION_STATE_CHANGE);
        filter.addAction(SpheroController.ACTION_SCAN_FAILED);
        registerReceiver(receiver, filter);
    }

    private boolean checkBluetoothPermissions() {
        // Request bluetooth permissions
        boolean permissionsReady = true;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            permissionsReady = false;
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            permissionsReady = false;
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
            DialogFragment info = new BluetoothSupportDialogue();
            info.show(getSupportFragmentManager(), "BluetoothSupportDialogue");
            return; // callback will finish() up
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
        sphero = new SpheroController(this, deviceAddress, SpheroController.DEFAULT_WAIT_FOR_RESPONSE, SpheroController.DEFAULT_RESEND_ATTEMPTS, SpheroController.DEFAULT_MESSAGE_TIMEOUT_ms);
        button_connect.setEnabled(true);
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
        if(!displayingPermissionsRejectionDialogue) {
            displayingPermissionsRejectionDialogue = true;
            DialogFragment info = new BluetoothPermissionsDialogue();
            info.show(getSupportFragmentManager(), "BluetoothPermissionsDialogue");
        }
    }
    // Called when the BluetoothPermissionsDialogue is dismissed
    public void onPermissionsDismissListener() {
        finish();
    }
    // Called when the BluetoothSupportDialogue is dismissed
    public void onSupportDismissListener() {
        finish();
    }
    public void onScanDismissListener() {
        // No longer connecting. The scan failed.
        viewModel.setConnectionState(ConnectionState.DISCONNECTED);
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

    // Observers for when the control fragments change the view model data.
    // Always get called once at the start when they are initialized with their default values.
    // So, make sure to skip them if sphero is null or isn't connected yet.
    private void observeChangeConnectionState() {
        if(sphero == null)
            return;
        // Hide/display control layouts
        ConnectionState connectionState = viewModel.getConnectionState().getValue();
        if(connectionState == null)
            return;
        // Temporarily disable connect button listener so it doesn't fire when we change it here
        button_connect.setOnCheckedChangeListener(null);
        switch(connectionState) {
            case DISCONNECTED:
//                constraintLayout_connected.setVisibility(View.INVISIBLE);
                MainActivity.setViewAndChildrenEnabled(constraintLayout_connected, false);
                constraintLayout_connected.setEnabled(false);
//                button_connect.setTextOff(getString(R.string.connect));
                button_connect.setEnabled(true);
                button_connect.setChecked(false);
                text_connetionStatus.setText(R.string.disconnected);
                viewModel.setAwake(false); // This will call observeChangeAwake, but because the state is disconnected, it won't send a command to the sphero.
                break;
            case CONNECTING:
//                constraintLayout_connected.setVisibility(View.INVISIBLE);
                MainActivity.setViewAndChildrenEnabled(constraintLayout_connected, false);
                constraintLayout_connected.setEnabled(false);
//                button_connect.setTextOn(getString(R.string.connecting));
                button_connect.setEnabled(false);
                button_connect.setChecked(true);
                text_connetionStatus.setText(R.string.connecting);
                // Communicate with sphero API to connect to it
                sphero.connect();

                break;
            case CONNECTED:
                // TODO Enter Reset Heading upon connecting to the sphero. Have some UI element indicating to the user what to do
//                constraintLayout_connected.setVisibility(View.VISIBLE);
                MainActivity.setViewAndChildrenEnabled(constraintLayout_connected, true);
                constraintLayout_connected.setEnabled(true);
//                button_connect.setTextOn(getString(R.string.disconnect));
                button_connect.setEnabled(true);
                button_connect.setChecked(true);
                text_connetionStatus.setText(R.string.connected);
                // If we just connected, we also want to wake up the sphero
                viewModel.setAwake(true);
                // Send configuration commands
                sphero.resetHeading();
                sphero.checkBattery();

                break;
            case DISCONNECTING:
//                constraintLayout_connected.setVisibility(View.INVISIBLE);
                MainActivity.setViewAndChildrenEnabled(constraintLayout_connected, false);
                constraintLayout_connected.setEnabled(false);
//                button_connect.setTextOff(getString(R.string.disconnecting));
                button_connect.setEnabled(false);
                button_connect.setChecked(false);
                text_connetionStatus.setText(R.string.disconnecting);
                // Communicate with sphero API to disconnect from it
                sphero.disconnect();
                break;
        }
        // Reactivate the listener
        button_connect.setOnCheckedChangeListener(this::onCheckedChanged_connect);
    }
    private void observeChangeSpeed() {
        // Control fragment changed the speed.
    }
    private void observeChangeLedBrightness() {
        // Control fragment changed LED brightness. Send it to the sphero.
        observeChangeLedColor();
    }
    private void observeChangeLedColor() {
        if(sphero == null || viewModel.getConnectionState().getValue() != ConnectionState.CONNECTED)
            return;
        int hue = viewModel.getLedColor().getValue();
        float brightness = viewModel.getLedBrightness().getValue() / 100f;
        if(hue == 100) {
            sphero.setColor((int)(255 * brightness), (int)(255 * brightness), (int)(255 * brightness));
        } else {
            float[] hsv = { hue / 100f * 360, 1, 1};
            int argb = Color.HSVToColor(hsv);
            int red = (int) (Color.red(argb) * brightness);
            int green = (int) (Color.green(argb) * brightness);
            int blue = (int) (Color.blue(argb) * brightness);
            sphero.setColor(red, green, blue);
        }
    }
    private void observeChangeAwake() {
        if(sphero == null || viewModel.getConnectionState().getValue() != ConnectionState.CONNECTED)
            return;
        if(viewModel.getAwake().getValue()) {
            sphero.wakeSphero();
        } else {
            sphero.sleepSphero();
        }
    }
    private void observeChangeResettingHeading() {
        if(sphero == null || viewModel.getConnectionState().getValue() != ConnectionState.CONNECTED)
            return;
        if(Boolean.TRUE.equals(viewModel.getResettingHeading().getValue())) { // null check
            // Start aiming.
            sphero.setColor(0,0,0);
            sphero.setBackLEDBrightness(255);
        } else {
            // Stop aiming. Reset heading.
//            int argb = viewModel.getLedColor().getValue();
//            viewModel.setLedColor(argb);
            observeChangeLedColor(); // Return the LED color to normal
            lastHeading = 0;
            sphero.setBackLEDBrightness(0);
            sphero.resetHeading();
        }
    }
    private void observeChangeRoll() {
        if(sphero == null || viewModel.getConnectionState().getValue() != ConnectionState.CONNECTED)
            return;
        // Send new command to sphero.
        // Stop sending commands after a [0, 0] is received (an implicit "stop" command).
        // Commands received from the view model are in range [-1, +1] and a radius of 1
        // sphero.stopMovingSphero

        double x = viewModel.getRollX().getValue() * 256;
        double y = viewModel.getRollY().getValue() * 256;
        double maxSpeed = viewModel.getSpeed().getValue() / 100f;
        if(viewModel.getResettingHeading().getValue()) {
            // Aiming.
            int heading = (int)Math.toDegrees(Math.atan2(y, x)) + 90;
            if(heading < 0 )
                heading += 360;
            sphero.rollSphero(0, heading);
        } else {
            // Driving normally.
            int speed = (int) ((int) Math.sqrt(x * x + y * y) * maxSpeed);
            if(speed > 255)
                speed = 255;

            // If receiving a speed of 0, stop now.
            if(speed == 0) {
                sphero.stopRollSphero(lastHeading);
            } else {
                int heading = (int)Math.toDegrees(Math.atan2(y, x)) + 90;
                if(heading < 0 )
                    heading += 360;
                sphero.rollSphero(speed, heading);
                lastHeading = heading;
            }
        }
    }

    public void onClickButton_battery(View view) {
        sphero.checkBattery();
    }

    private void onCheckedChanged_connect(CompoundButton compoundButton, boolean isChecked) {
        if (isChecked) {
            // Start connecting
            viewModel.setConnectionState(ConnectionState.CONNECTING);
        } else {
            // Start disconnecting
            viewModel.setConnectionState(ConnectionState.DISCONNECTING);
        }
    }
}