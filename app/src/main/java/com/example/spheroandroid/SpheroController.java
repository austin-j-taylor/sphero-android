package com.example.spheroandroid;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

// API for communicating with the sphero.
// Create a SpheroController object to start setting up communication with it.
// Creates a worker thread to handle Bluetooth communication off of the main UI thread.
// (my sphero's name is Orbi, who is blue and beloved by the cats.)
public class SpheroController {

    public static final String TAG = "SpheroController";
    public final static String ACTION_BATTERY_AVAILABLE =
            "com.example.controlapp.SpheroController.ACTION_BATTERY_AVAILABLE";
    public final static String ACTION_SPHERO_CONNECTION_STATE_CHANGE =
            "com.example.controlapp.SpheroController.ACTION_SPHERO_CONNECTION_STATE_CHANGE";
    public final static String ACTION_SCAN_FAILED =
            "com.example.controlapp.SpheroController.ACTION_SCAN_FAILED";
    public final static String EXTRA_BATTERY_VALUE =
            "com.example.controlapp.SpheroController.EXTRA_BATTERY_VALUE";
    public final static String EXTRA_SPHERO_CONNECTION_STATE =
            "com.example.controlapp.SpheroController.EXTRA_SPHERO_CONNECTION_STATE";
    // Time to scan for a sphero before giving up.
    private static final long SCAN_PERIOD = 8000;

    // Default arguments for normal robust communication
    public final static boolean DEFAULT_WAIT_FOR_RESPONSE = true;
    public final static int DEFAULT_RESEND_ATTEMPTS = 3;
    public final static int DEFAULT_MESSAGE_TIMEOUT_ms = 200;

    public enum ConnectionState {DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING}

    private final String macAddress;
    private final boolean waitForResponse;
    private final int resendAttempts;
    private final int messageTimeout_ms;
    private BluetoothThread btThread;
    private final BluetoothLeScanner bluetoothLeScanner;
    private boolean scanning;
    private Handler scanHandler;
    // Context of the parent activity. Be very careful with accessing this, as it can be used
    // to get to the main thread from the BluetoothThread.
    // Only used to send broadcasts, which is how the user can check responses/data from the sphero.
    private final Context parentContext;

    // Context context: The parent context (e.g. the activity you're creating the object in), which
    //      is needed to start Bluetooth communication.
    //      Intents will be broadcasted this context when the connection state changes and when data
    //      is received from the sphero, allowing the user to process them in a broadcast receiver.
    // boolean waitForResponse: If true, the program will wait after sending a command until either
    //      a) The sphero responds with an acknowledgement message, or
    //      b) The message times out.
    // boolean resendAttempts: The number of times to resend messages if they time out.
    //      Set to 0 to only ever send messages once.
    // int messageTimeout_ms: Time in milliseconds to wait to receive a response before giving up and resending a message.
    //      Ignored if waitForResponse is true.
    //      Won't resend messages if resendAttempts is 0.
    public SpheroController(Context context, String macAddress, boolean waitForResponse, int resendAttempts, int messageTimeout_ms) {
        this.macAddress = macAddress;
        this.waitForResponse = waitForResponse;
        this.resendAttempts = resendAttempts;
        this.messageTimeout_ms = messageTimeout_ms;
        this.parentContext = context;

        scanning = false;
        btThread = new BluetoothThread();
        btThread.start();

        BluetoothManager bluetoothManager = context.getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    public SpheroController(Context context, String macAddress) {
        this(context, macAddress, DEFAULT_WAIT_FOR_RESPONSE, DEFAULT_RESEND_ATTEMPTS, DEFAULT_MESSAGE_TIMEOUT_ms);
    }

    // Clean up this sphero controller. Close the bluetooth server.
    public void destroy() {
        btThread.messageHandler.sendEmptyMessage(BluetoothThread.MSG_STOP);
        btThread = null;
        if (scanHandler != null)
            scanHandler.removeCallbacksAndMessages(null);
        scanning = false;
    }

    public void connect() {
        // In the main thread.
        // Start scanning for the sphero to connect to it.
        // scanCallback will finish up connecting to the sphero.
        if (!scanning) {
            // Stops scanning after a predefined scan period.
            Log.i(TAG, "Starting scan for Sphero...");
            scanHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (scanning) {
                        if (ActivityCompat.checkSelfPermission(parentContext, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        Intent intent = new Intent(ACTION_SCAN_FAILED);
                        parentContext.sendBroadcast(intent);

                        scanning = false;
                        bluetoothLeScanner.stopScan(scanCallback);
//                        activity.setEnabled_connectToSphero(false);
                    }
                }
            }, SCAN_PERIOD);
            scanning = true;
            bluetoothLeScanner.startScan(scanCallback);
        } else {
            // already scanning
        }
//        btThread.messageHandler.sendEmptyMessage(BluetoothThread.MSG_CMD_CONNECT);
    }

    // Device scan callback. Called when a device is found over BLE.
    private final ScanCallback scanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    if (ActivityCompat.checkSelfPermission(parentContext, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    if(result.getDevice().getAddress().equals(macAddress)) {
                        // Sphero has been located.
                        // Start connecting to it.
                        scanning = false;
                        bluetoothLeScanner.stopScan(scanCallback);
                        scanHandler.removeCallbacksAndMessages(null);

                        btThread.messageHandler.sendEmptyMessage(BluetoothThread.MSG_CMD_CONNECT);
                    }
                }
            };

    public void disconnect() {
        btThread.messageHandler.sendEmptyMessage(BluetoothThread.MSG_CMD_DISCONNECT);
    }
    public void wakeSphero() {
        btThread.messageHandler.sendEmptyMessage(BluetoothThread.MSG_CMD_WAKE);
    }
    public void sleepSphero() {
        btThread.messageHandler.sendEmptyMessage(BluetoothThread.MSG_CMD_SLEEP);
    }
    public void resetHeading() {
        btThread.messageHandler.sendEmptyMessage(BluetoothThread.MSG_CMD_RESET_HEADING);
    }
    // Start to move the Sphero at a given direction and speed.
    // heading: integer from 0 - 360 (degrees)
    // speed: Integer from 0 - 255
    public void rollSphero(int speed, int heading) {
        Message message = Message.obtain();
        message.what = BluetoothThread.MSG_CMD_ROLL;
        message.arg1 = speed;
        message.arg2 = heading;
        btThread.messageHandler.sendMessage(message);
    }
    // Immediately stop rolling the sphero, facing the provided last heading.
    // Also wipes the queue of any other rolling commands,
    // ensuring that the sphero will stop as soon as possible.
    public void stopRollSphero(int lastHeading) {
        Message message = Message.obtain();
        message.what = BluetoothThread.MSG_CMD_ROLL_STOP;
        message.arg1 = lastHeading;
        btThread.messageHandler.sendMessage(message);
    }
    public void checkBattery() {
        btThread.messageHandler.sendEmptyMessage(BluetoothThread.MSG_CMD_BATTERY);
    }
    // Sets the RGB color of the top LED of the sphero.
    // 0-255
    public void setColor(int red, int green, int blue) {
        Message message = Message.obtain();
        message.what = BluetoothThread.MSG_CMD_COLOR;
        message.obj = new int[] {red, green, blue};
        btThread.messageHandler.sendMessage(message);
    }
    // Sets the intensity of the back LED.
    // 0-255
    public void setBackLEDBrightness(int intensity) {
        Message message = Message.obtain();
        message.what = BluetoothThread.MSG_CMD_BACK_LED;
        message.arg1 = intensity;
        btThread.messageHandler.sendMessage(message);
    }


    // Thread for performing and BLE communication with the sphero.
    private class BluetoothThread extends Thread {
        // Handler Message.what codes. Used to communicate with the main thread.
        public final static int MSG_CMD_CONNECT = 0;
        public final static int MSG_CMD_DISCONNECT = 1;
        public final static int MSG_CMD_WAKE = 3;
        public final static int MSG_CMD_SLEEP = 4;
        public final static int MSG_STOP = 5;
        public final static int MSG_CMD_RESET_HEADING = 6;
        public final static int MSG_CMD_STABILIZATION = 7;
        public final static int MSG_CMD_ROLL = 8;
        public final static int MSG_CMD_ROLL_STOP = 9;
        public final static int MSG_RESEND = 13;
        public final static int MSG_CMD_COLOR = 14;
        public final static int MSG_CMD_BACK_LED = 15;
        public final static int MSG_CMD_BATTERY = 17;
        // Command processing constants
        private final static int BUFFER_CAPACITY = 64;
        private final static int COMMAND_MIN_SIZE = 7;

        // When sending commands to quickly, the queue may build up.
        // If the message count exceeds this, it will dump the older messages and replace them with
        // the new command.
        // if waitForResponse is true, the program will still wait for the most recently-sent command
        // to get a response or time out before sending the new command.
        // The CONTINUOUS capacity is used for commands that generally arrive very quickly but
        // it doesn't matter if we lose a single command. This applies for roll.
        // The SINGLE capacity is used for commands that generally arrive one-at-a-time, and each
        // command is usually quite important, so the capacity is bigger. This applies for all other
        // commands, like LED setting.
        private static final int COMMAND_QUEUE_CAPACITY_CONTINUOUS = 3;
        private static final int COMMAND_QUEUE_CAPACITY_SINGLE = 8;

        private BluetoothSpheroController spheroService;
        // Handler for communicating with the main thread.
        public Handler messageHandler;
        // Timer for when the sphero is initializing. We don't want to send commands while it's setting up.
        private Timer handshakeTimer;
        private boolean initialized;
        // Queue of commands waiting to receive a response or be timed out.
        private ArrayDeque<ResponseCommand> commandQueue;
        // Current sequence number of the last sent command.
        private byte sequenceNumber;
        // Command buffer fields for building response commands read over time
        private byte[] buffer = new byte[BUFFER_CAPACITY];
        private int bufferLength = 0;

        private BluetoothGattCharacteristic characteristic_API_V2;
        private BluetoothGattCharacteristic characteristic_AntiDOS;
        private BluetoothGattCharacteristic characteristic_DFU;
        private BluetoothGattCharacteristic characteristic_DFU2;

        public BluetoothThread() {

            scanHandler = new Handler();
            initialized = false;
            sequenceNumber = 0;
            commandQueue = new ArrayDeque<>();
        }

        public void run() {

            Looper.prepare();
            messageHandler = new Handler(Looper.myLooper()) {
                public void handleMessage(Message msg) {
                    // Handle messages here...
//                    Log.i(TAG, "Processing SpheroController message: " + msg.what);
                    switch (msg.what) {
                        case MSG_CMD_CONNECT:
                            command_connect();
                            break;
                        case MSG_CMD_DISCONNECT:
                            command_disconnect();
                            break;
                        case MSG_CMD_WAKE:
                            command_wake();
                            break;
                        case MSG_CMD_SLEEP:
                            command_sleep();
                            break;
                        case MSG_STOP:
                            stopThread();
                            break;
                        case MSG_CMD_RESET_HEADING:
                            reset_heading();
                            break;
                        case MSG_CMD_STABILIZATION:
                            stabilization(msg.arg1 != 0);
                            break;
                        case MSG_CMD_ROLL:
                            command_roll(msg.arg1, msg.arg2);
                            break;
                        case MSG_CMD_ROLL_STOP:
                            command_roll_stop(msg.arg1);
                            break;
                        case MSG_RESEND:
                            resend_command((ResponseCommand) msg.obj);
                            break;
                        case MSG_CMD_COLOR:
                            int[] colors = (int[])msg.obj;
                            command_LEDColor(colors[0], colors[1], colors[2]);
                            break;
                        case MSG_CMD_BACK_LED:
                            int intensity = msg.arg1;
                            command_backLEDIntensity(intensity);
                            break;
                        case MSG_CMD_BATTERY:
                            command_battery();
                            break;
                        default:
                            Log.e(TAG, "Invalid Message received");
                            break;
                    }
                }
            };

            Looper.loop();
            Log.i(TAG, "Ending SpheroController BLE thread.");
        }
        private void stopThread() {
            if(initialized)
                command_disconnect();
            if (handshakeTimer != null)
                handshakeTimer.cancel();
            if (Looper.myLooper() != null)
                Looper.myLooper().quit();
            messageHandler.removeCallbacksAndMessages(null);
            try{
                parentContext.unregisterReceiver(gattUpdateReceiver);
            } catch (IllegalArgumentException e) {
                Log.v(TAG, "Unregistered receiver that had not actually been registered while stopping thread.");
            }
            if(spheroService != null)
                spheroService.close();
            spheroService = null;
            messageHandler = null;
            Log.i(TAG, "BLE Thread canceled/destroyed/stopped.");
        }


        // BLE Broadcast receiver.
        // Processes broadcasts sent by the BluetoothSpheroController.
        private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (BluetoothSpheroController.ACTION_GATT_CONNECTED.equals(action)) {
                    // Now connected
                    // Do nothing, since more initialization needs to be done below.
                    // The BluetoothSpheroController is listening to the same ACTION_GATT_CONNECTED
                    // broadcast, where it will call discoverServices(). This will then broadcast
                    // ACTION_GATT_SERVICES_DISCOVERED, which we receive below.
                } else if (BluetoothSpheroController.ACTION_GATT_DISCONNECTED.equals(action)) {
                    // Now disconnected
                    initialized = false;
                    // Let the parent context know that the connection state has changed
                    Intent newIntent = new Intent(ACTION_SPHERO_CONNECTION_STATE_CHANGE);
                    newIntent.putExtra(EXTRA_SPHERO_CONNECTION_STATE, ConnectionState.DISCONNECTED);
                    context.sendBroadcast(newIntent); // send it up

                } else if (BluetoothSpheroController.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                    // Show all the supported services and characteristics on the user interface.
                    identifyGattServices(spheroService.getSupportedGattServices());
                    // Start listening to responses sent by the sphero
                    subscribeToCharacteristics();
                    // Send initial keep-awake command
                    // Can't send this immediately or else the sphero won't properly receive it...
                    // Send it after a delay. Make sure to not use the spheroService until it's done,
                    // since these read/write methods aren't thread-safe
                    Log.i(TAG, "Starting delay for initial configuration...");
                    handshakeTimer = new Timer();
                    handshakeTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            // Send command to prevent sphero from falling asleep after 10 seconds.
                            // The keep-awake command is the UTF_8 encoding of the text
                            // "usetheforce...band" (look up "Sphero Force Band" and guess why).
                            if (!spheroService.writeCharacteristic(characteristic_AntiDOS, "usetheforce...band".getBytes(StandardCharsets.UTF_8))) {
                                Log.i(TAG, "Failed to send keep-awake message.");
                            }
                            handshakeTimer = null;
                        }
                    }, 200);
                } else if (BluetoothSpheroController.ACTION_DATA_AVAILABLE.equals(action)) {
                    // Data available from the sphero.
                    byte[] data = intent.getByteArrayExtra(BluetoothSpheroController.EXTRA_DATA);
                    SpheroGattAttributes.lookup(intent.getStringExtra(BluetoothSpheroController.EXTRA_DATA_UUID));
                    receive_response(data);

                } else if (BluetoothSpheroController.ACTION_WRITE_SUCCESSFUL.equals(action)) {
                    if (intent.getStringExtra(BluetoothSpheroController.EXTRA_DATA_UUID).equals(SpheroGattAttributes.AntiDOS_characteristic)) {
                        // Received when the keep-awake command is confirmed to have been sent
                        initialized = true;
                        Log.i(TAG, "Successfully sent keep-awake. Ready for I/O.");
                        // Let the user know that the connection state is now connected!
                        // Receive this broadcast in your code to know it's time to start sending
                        // commands (such as wakeSphero() and resetHeading()).
                        Intent newIntent = new Intent(ACTION_SPHERO_CONNECTION_STATE_CHANGE);
                        newIntent.putExtra(EXTRA_SPHERO_CONNECTION_STATE, ConnectionState.CONNECTED);
                        context.sendBroadcast(newIntent);
                    }
                }
            }
        };


        private IntentFilter makeGattUpdateIntentFilter() {
            final IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BluetoothSpheroController.ACTION_GATT_CONNECTED);
            intentFilter.addAction(BluetoothSpheroController.ACTION_GATT_DISCONNECTED);
            intentFilter.addAction(BluetoothSpheroController.ACTION_GATT_SERVICES_DISCOVERED);
            intentFilter.addAction(BluetoothSpheroController.ACTION_DATA_AVAILABLE);
            intentFilter.addAction(BluetoothSpheroController.ACTION_WRITE_SUCCESSFUL);
            return intentFilter;
        }

        // Displays the GATT services and characteristics.
        // Identifies the GATT characteristics used for reading/writing to the sphero and saves them.
        private void identifyGattServices(List<BluetoothGattService> gattServices) {
            if (gattServices == null) return;
            String uuid = null;
            String unknownServiceString = "Unknown Service";
//        String unknownServiceString = getResources().getString(R.string.unknown_service);
            String unknownCharaString = "Unknown Characteristic";
            String LIST_NAME = "Sphero";
            String LIST_UUID = "Sphero's UUID";
//        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
            ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
            ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
            ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

            // Loops through available GATT Services.
            for (BluetoothGattService gattService : gattServices) {
                HashMap<String, String> currentServiceData = new HashMap<String, String>();
                uuid = gattService.getUuid().toString();
                currentServiceData.put(LIST_NAME, SpheroGattAttributes.lookup(uuid, unknownServiceString));
                currentServiceData.put(LIST_UUID, uuid);
                gattServiceData.add(currentServiceData);

                ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();
                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    charas.add(gattCharacteristic);
                    HashMap<String, String> currentCharaData = new HashMap<String, String>();
                    uuid = gattCharacteristic.getUuid().toString();
                    String name = SpheroGattAttributes.lookup(uuid, unknownCharaString);
                    currentCharaData.put(LIST_NAME, name);
                    currentCharaData.put(LIST_UUID, uuid);
                    gattCharacteristicGroupData.add(currentCharaData);

                    if (name.equals("API_V2_characteristic")) {
                        characteristic_API_V2 = gattCharacteristic;
                    } else if (name.equals("AntiDOS_characteristic")) {
                        characteristic_AntiDOS = gattCharacteristic;
                    } else if (name.equals("DFU_characteristic")) {
                        characteristic_DFU = gattCharacteristic;
                    } else if (name.equals("DFU2_characteristic")) {
                        characteristic_DFU2 = gattCharacteristic;
                    }
                }
                mGattCharacteristics.add(charas);
                gattCharacteristicData.add(gattCharacteristicGroupData);
            }
        }
        // Enable characteristic notifications for API_V2, where most of the data is read from.
        private void subscribeToCharacteristics() {
            if(characteristic_API_V2 == null) {
                Log.e(TAG, "Unable to find API_V2 characteristic.");
            } else {
                if(!spheroService.setCharacteristicNotification(characteristic_API_V2, true))
                    Log.e(TAG, "Failed to set characteristic notification for characteristic_API_V2");
            }
            if(characteristic_DFU == null) {
                Log.e(TAG, "Unable to find DFU2 characteristic.");
            } else {
                if(!spheroService.setCharacteristicNotification(characteristic_DFU, true))
                    Log.e(TAG, "Failed to set characteristic notification for characteristic_DFU");
            }
        }

        private void command_connect() {
            characteristic_API_V2 = null;
            characteristic_AntiDOS = null;
            characteristic_DFU = null;
            characteristic_DFU2 = null;
            sequenceNumber = 0;
            commandQueue.clear();
            clear_buffer();

            // Start the BLE Sphero Controller, or connect to it if it already exists
            if(spheroService == null) {
                // TODO: think hard about this. All the callbacks are happening on the main thread.
                parentContext.registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter(), null, messageHandler);
                spheroService = new BluetoothSpheroController();
                Log.i(TAG, "Created BLE \"service.\"");
                // call functions on service to check connection and connect to devices
                if (!spheroService.initializeAdapter()) {
                    Log.e(TAG, "Unable to initialize bluetooth adapter for BLE service.");
                } else {
                    Log.i(TAG, "Service connected. Attempting to connect to Sphero...");
                    if (spheroService.connect(macAddress)) {
                    }
                }
            } else {
                // Service already connected
                Log.i(TAG, "Service already exists. Attempting to connect to Sphero...\n");
                if(spheroService.connect(macAddress)) {
                    Log.i(TAG, "Device located. Attempting to connect to its GATT server..");
                }
            }
        }

        private void command_disconnect() {

            if(spheroService != null) {
                // also go to sleep before disconnecting
                command_sleep();
                // Wait for the sleep command to get processed before disconnecting the service
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(handshakeTimer != null)
                    handshakeTimer.cancel();
                if(spheroService != null)
                    spheroService.disconnect();
            }
            initialized = false;
        }

        // Reset the heading zero angle to the current heading (useful during aiming)
        // Note: in order to manually rotate the sphero, you need to call stabilization(false).
        // Once the heading has been set, call stabilization(true).
        private void reset_heading() {
            send_command(SpheroConstants.driving, SpheroConstants.resetHeading, false);
        }
        // Sends command to turn on/off the motor stabilization system (required when manually turning/aiming the sphero)
        private void stabilization(boolean enabled) {
            send_command(SpheroConstants.driving, SpheroConstants.stabilization, (byte)(enabled ? 0x01 : 0x00), false);
        }
        private void command_wake() {
            send_command(SpheroConstants.powerInfo, SpheroConstants.wake, false);
        }
        private void command_sleep() {
            send_command(SpheroConstants.powerInfo, SpheroConstants.sleep, false);
        }

        //        Start to move the Sphero at a given direction and speed.
        //        heading: integer from 0 - 360 (degrees)
        //        speed: Integer from 0 - 255
        //
        //        Note: the zero heading should be set at startup with the resetHeading method.
        //        Otherwise, it may seem that the sphero doesn't honor the heading argument
        private void command_roll(int speed, int heading) {
            if(speed < 0)
                speed = -1 * speed + 256; // speed values > 256 in the send packet make the spero go in reverse
            // Format high and low bytes as needed
            byte speedH = (byte)((speed & 0xFF00) >> 8);
            byte speedL = (byte)(speed & 0xFF);
            byte headingH = (byte)((heading & 0xFF00) >> 8);
            byte headingL = (byte)(heading & 0xFF);


            send_command(SpheroConstants.driving, SpheroConstants.driveWithHeading, new byte[] {speedL, headingH, headingL, speedH}, true);
        }
        // Remove all other roll commands from the queue right now,
        // and place a roll(0, heading) command at the front of the queue.
        private void command_roll_stop(int heading) {
            removeCommandsOfType(SpheroConstants.driving, SpheroConstants.driveWithHeading);

            byte headingH = (byte)((heading & 0xFF00) >> 8);
            byte headingL = (byte)(heading & 0xFF);
            send_command(SpheroConstants.driving, SpheroConstants.driveWithHeading, new byte[] {0, headingH, headingL, 0}, true);
        }
        private void command_LEDColor(int red, int green, int blue) {
            byte red_byte = (byte)(0xff & red);
            byte green_byte = (byte)(0xff & green);
            byte blue_byte = (byte)(0xff & blue);
            send_command(SpheroConstants.userIO, SpheroConstants.allLEDs, new byte[] { 0x00, 0xe, red_byte, green_byte, blue_byte }, false);
        }
        private void command_backLEDIntensity(int intensity) {
            byte intensity_byte = (byte)(intensity & 0xff);
            send_command(SpheroConstants.userIO, SpheroConstants.allLEDs, new byte[] {0x00, 0x01, intensity_byte}, false);
        }
        private void command_battery() {
            send_command(SpheroConstants.powerInfo, SpheroConstants.batteryVoltage, false);
        }


        // Constructs a command to send to the Sphero using its communication protocol.
        // From https://github.com/MProx/Sphero_mini
        /*
        Packet structure has the following format (in order):

        - Start byte: always 0x8D
        - Flags byte: indicate response required, etc
        - Virtual device ID: see SpheroConstants
        - Command ID: see SpheroConstants
        - Sequence number: Seems to be arbitrary. I suspect it is used to match commands to response packets (in which the number is echoed).
                - Payload: Could be varying number of bytes (incl. none), depending on the command
        - Checksum: See below for calculation
        - End byte: always 0xD8
        */
        private byte[] pack_command(byte deviceID, byte commandID, byte[] payload, byte sequence) {

//            // If the same type of command is being sent faster than the orb can
//            // realistically process, skip some of the messages. (example: >30Hz joystick input)
//            if(deviceID == lastMessageDevice && commandID == lastMessageCommand) {
//                // Debugging - get time from UI to change on the fly
//                if(!important && lastMessageTime + Long.valueOf(String.valueOf(((EditText)activity.findViewById(R.id.editTextNumber_time)).getText())) > System.currentTimeMillis()) {
//                    // too many similar commands being sent too quickly. skip this one.
//                    Log.i(TAG, "Skipped likely insignificant command.");
//                    return;
//                }
//            }
//            lastMessageDevice = deviceID;
//            lastMessageCommand = commandID;
//            lastMessageTime = System.currentTimeMillis();

            int checksum = 0;
            byte flag_requestResponse = 0x02;
            byte[] output = new byte[7 + payload.length]; // 2 for start/end, 1 for flag, 2 for ID's, 1 for sequence, 1 for the checksum, and N for the payload
//            byte[] output_unchecked = {SpheroConstants.start, flag_requestResponse, deviceID, commandID, sequence};
//            byte[] output = new byte[output_unchecked.length + 2]; // one more byte for the checksum, one more for the end marker
            output[0] = SpheroConstants.start;
            output[1] = SpheroConstants.requestsResponse;
            output[2] = deviceID;
            output[3] = commandID;
            output[4] = sequence;
            for(int i = 0; i < payload.length; i++) {
                output[5 + i] = payload[i];
            }

            // Checksum is the 1's complement of the (sum of all the previous bytes, excluding start and end markers)
            for(int i = 1; i < output.length - 2; i++) {
                checksum = (checksum + output[i]) & 0xff;
            }
            // Take the 1's complement
            checksum = 0xff - checksum;
            output[output.length - 2] = (byte)checksum;
            output[output.length - 1] = SpheroConstants.end;

            return output;
        }
        private void send_command(byte deviceID, byte commandID, boolean continuous) {
            byte[] payload = new byte[0];
            send_command(deviceID, commandID, payload, continuous);
        }
        private void send_command(byte deviceID, byte commandID, byte payloadByte, boolean continuous) {
            byte[] payload = new byte[1];
            payload[0] = payloadByte;
            send_command(deviceID, commandID, payload, continuous);
        }

        private void send_command(byte deviceID, byte commandID, byte[] payload, boolean continuous) {
            if(!initialized) {
                Log.w(TAG, "Failed to send message (not yet initialized for I/O)");
                return;
            }

            if(waitForResponse) {
                // Check if the queue is full. If it is, remove similar older commands and put this
                // new command at the back of the queue.
                if(commandQueue.size() >= (continuous ? COMMAND_QUEUE_CAPACITY_CONTINUOUS : COMMAND_QUEUE_CAPACITY_SINGLE)) {
                    Log.i(TAG, "Queue getting full (" + commandQueue.size() +"). Dumping similar commands.");
                    removeCommandsOfType(deviceID, commandID);
                }
                
                ResponseCommand command = new ResponseCommand(deviceID, commandID, payload, sequenceNumber, continuous, 0, System.currentTimeMillis());
                commandQueue.add(command);
                Log.v(TAG, "Added fresh command: " + String.format("%02X, %02X", deviceID, commandID) + " | Queue size: " + commandQueue.size() + " | sequence: " + String.format("%02X ", sequenceNumber));
                if(commandQueue.size() > 127)
                    Log.e(TAG, "Queue size is very high. Sequence numbers may overlap, resulting in undefined behavior.");

                // If the queue was previously empty, it is safe to send this command now.
                if(commandQueue.size() == 1) {
                    byte[] output = pack_command(deviceID, commandID, payload, sequenceNumber);
                    spheroService.writeCharacteristic(characteristic_API_V2, output);
                    // Resend the message if it isn't AWK'd soon enough
                    startResendTimer(command);
                    Log.v(TAG, "Sending this fresh command now.");
                } else {
                    // If the queue was previously occupied, there are commands waiting for acknowledgements
                    // (or waited to be timed out to be automatically re-sent).
                    // Wait for this command to be sent by send_nextCommandInQueue() through receive_response() or resend_command()
                }
            } else {
                // Not waiting for responses. Bypass the response queue altogether.
                // Just send commands to the sphero without worrying about if they get received.
                byte[] output = pack_command(deviceID, commandID, payload, sequenceNumber);
                spheroService.writeCharacteristic(characteristic_API_V2, output);
                Log.v(TAG, "Sending this fresh command now. Not awaiting a response.");
            }

            sequenceNumber++;
        }

        private void send_nextCommandInQueue() {
            if(!initialized) {
                Log.w(TAG, "Failed to send message (not yet initialized for I/O)");
                return;
            }
            // The previous command was just acknowledged, or we gave up on resending it.
            // Send the next command in the queue.
            if(commandQueue.size() == 0) {
                return;
            }
            ResponseCommand command = commandQueue.getFirst();

            byte[] output = pack_command(command.deviceID, command.commandID, command.payload, command.sequence);
            Log.v(TAG, "Sending first command in queue: " + String.format("%02X, %02X", command.deviceID, command.commandID) + " | Queue size: " + commandQueue.size() + " | sequence: " + String.format("%02X ", sequenceNumber) + " | times sent: " + command.getSendAttemptsCount());

            spheroService.writeCharacteristic(characteristic_API_V2, output);
            // Resend the message if it isn't AWK'd soon enough
            startResendTimer(command);
        }

        // Resend a command after its acknowledgement wasn't received after the timeout.
        private void resend_command(ResponseCommand command) {
            if(!waitForResponse) {
                Log.e(TAG, "Attempted to resend a command when waitForResponse was false.");
                return;
            }
            if(!initialized) {
                Log.w(TAG, "Failed to send message (not yet initialized for I/O)");
                return;
            }
            // Remove the old message from the queue
            Iterator<ResponseCommand> iter = commandQueue.iterator();
            ResponseCommand removedCommand = null;
            while(iter.hasNext()) {
                ResponseCommand el = iter.next();
                if(el.getSequence() == command.getSequence()) {
                    removedCommand = el;
                    iter.remove();
                    break;
                }
            }
            if(removedCommand == null) {
                Log.e(TAG, "Attempted to send a command from the queue that wasn't in the queue! | sequence: " + String.format("%02X ", command.sequence));
            }

            int sendAttemptsCount = command.getSendAttemptsCount() + 1;

            // Resend the command, if it hasn't been sent too many times already.
            // Also don't resend it if it's been discarded due to too many commands being sent too quickly.
            if((sendAttemptsCount >= resendAttempts || removedCommand.isDiscarded()) || removedCommand == null) {
                Log.w(TAG, "Command failed to send " + sendAttemptsCount + " time(s): " + String.format("%02X %02X", command.deviceID, command.commandID) + " | sequence: " + command.sequence);

                // Give up on this command. send the next one.
                send_nextCommandInQueue();
            } else {
                // Check if the queue is full. If it is, remove similar older commands and put this
                // new command at the back of the queue.
                if(commandQueue.size() >= (command.getContinuous() ? COMMAND_QUEUE_CAPACITY_CONTINUOUS : COMMAND_QUEUE_CAPACITY_SINGLE)) {
                    Log.i(TAG, "Queue getting full (" + commandQueue.size() +") while resend a command. Dumping similar commands.");
                    removeCommandsOfType(command.deviceID, command.commandID);
                }

                byte[] output = pack_command(command.deviceID, command.commandID, command.payload, sequenceNumber);
                ResponseCommand newCommand = new ResponseCommand(command.deviceID, command.commandID, command.payload, sequenceNumber, command.getContinuous(), sendAttemptsCount, System.currentTimeMillis());
                commandQueue.addFirst(newCommand);
                Log.v(TAG, "Resending command. Queue size: " + commandQueue.size() + " | sequence: " + String.format("%02X ", sequenceNumber) + " | times sent: " + newCommand.getSendAttemptsCount());
                if(commandQueue.size() > 127)
                    Log.e(TAG, "Queue size is very high. Sequence numbers may overlap, resulting in undefined behavior.");

                spheroService.writeCharacteristic(characteristic_API_V2, output);
                startResendTimer(newCommand);
                sequenceNumber++;
            }
        }
        private void startResendTimer(ResponseCommand command) {
            long time = System.currentTimeMillis();

            command.setTimeWasSent(time);
            // (TIMEOUT) milliseconds after a command has been sent, check if it has been received.
            // If it hasn't been received, Send it again.
            // Give up after this is attempted N times (decided in resend_message())
            Log.v(TAG, "Starting timer... | sequence: " + String.format("%02X ", command.getSequence()));
            messageHandler.postDelayed(() -> {
                if(command.getReceived()) {
                    // good! Message was received.
                } else {
                    Log.v(TAG, "Timed out... and wasn't received! | sequence: " + String.format("%02X ", command.getSequence()));
                    // No acknowledgement from the sphero... Try again, up to N attempts total.
                    // Send it again with a new sequence number, current time, etc.
                    Message message = Message.obtain();
                    message.what = BluetoothThread.MSG_RESEND;
                    message.obj = command;
                    messageHandler.sendMessage(message);
                }
            }, messageTimeout_ms);
        }

        // Called when the sphero responds to one of our messages.
        // Parses the data sent, processes it, and keeps track of the message it's currently sending
        // (In practice, this is called nearly once for every byte it sends and the data array only
        // contains one byte.)
        private void receive_response(byte[] data) {
            for(byte data_byte : data) {
                buffer[bufferLength] = data_byte;
                bufferLength++;
                if(bufferLength >= buffer.length) {
                    // Buffer is full! Command is definitely malformed. Discard it.
                    Log.e(TAG, "Response command buffer full! a command is unexpectedly long, or an end signifier was missed.");
                    clear_buffer();
                    return;
                }
                // If this is a start byte and the buffer already has data, something went wrong.
                // Dump the previous, malformed command.
                if(bufferLength != 1 && data_byte == SpheroConstants.start) {
                    clear_buffer();
                    buffer[0] = data_byte;
                }

                // If byte is the end of a command, the command is done. Parse it.
                // The end byte may also coincidentally show up as in the payload of other messages.
                // This is a problem.
                else if(data_byte == SpheroConstants.end) {
                    boolean handled = false;

                    // Parse it
                    // Make sure command is long enough
                    if(bufferLength < COMMAND_MIN_SIZE) {
                        // Malformed
                        clear_buffer();
                        return;
                    }

                    if(buffer[0] != SpheroConstants.start) {
                        // Malformed
                        clear_buffer();
                        return;
                    }
                    if(buffer[bufferLength - 1] != SpheroConstants.end) {
                        // Redundant check
                        clear_buffer();
                        return;
                    }
                    byte flags = buffer[1];
                    byte devid = buffer[2];
                    byte comid = buffer[3];
                    byte sequence = buffer[4];
                    byte checksum = buffer[bufferLength - 2]; // TODO verify checksum
                    byte[] payload;
                    // Get payload. Can be size 0.
                    int payloadLength = bufferLength - COMMAND_MIN_SIZE;
                    payload = new byte[payloadLength];
                    for(int i = 0; i < payloadLength; i++)
                        payload[i] = buffer[5 + i]; // buffer[5] would be the start of the payload

                    if((flags & SpheroConstants.isResponse) > 0) { // acknowledgement response
                        switch(devid) {
                            case SpheroConstants.powerInfo:
                                switch(comid) {
                                    case SpheroConstants.wake:
                                        Log.v(TAG, "AWK: wake");
                                        handled = true;
                                        break;
                                    case SpheroConstants.sleep:
                                        Log.v(TAG, "AWK: sleep");
                                        handled = true;
                                        break;
                                    case SpheroConstants.batteryVoltage:
                                        if(payloadLength != 3) {
                                            Log.v(TAG, "AWK: battery voltage, but payload length was not 3: " + payloadLength);
                                            break;
                                        }
                                        int pay2 = Byte.toUnsignedInt(payload[2]);
                                        int pay1 = Byte.toUnsignedInt(payload[1]);
                                        int pay0 = Byte.toUnsignedInt(payload[0]);
                                        double vbatt = pay2 + pay1 * 256 + pay0 * 65536;
                                        vbatt /= 100; // Notification gives V_batt in 10mV increments. Divide by 100 to get to volts.
                                        Log.v(TAG, "AWK: battery voltage: " + vbatt);
                                        Intent intent = new Intent(ACTION_BATTERY_AVAILABLE);
                                        intent.putExtra(EXTRA_BATTERY_VALUE, vbatt);
                                        parentContext.sendBroadcast(intent);
                                        handled = true;

                                        break;
                                }
                                break;
                            case SpheroConstants.driving:
                                switch(comid) {
                                    case SpheroConstants.driveWithHeading:
                                        Log.v(TAG, "AWK: roll");
                                        handled = true;
                                        break;
                                    case SpheroConstants.stabilization:
                                        Log.v(TAG, "AWK: stabilization");
                                        handled = true;
                                        break;
                                    case SpheroConstants.resetHeading:
                                        Log.v(TAG, "AWK: reset heading");
                                        handled = true;
                                        break;
                                }
                                break;
                            case SpheroConstants.userIO:
                                switch(comid) {
                                    case SpheroConstants.allLEDs:
                                        Log.v(TAG, "AWK: LED/backlight color");
                                        handled = true;
                                        break;
                                }
                                break;
                            case SpheroConstants.sensor:
                                switch(comid) {
                                    case SpheroConstants.configureCollision:
                                        Log.v(TAG, "AWK: collision detection configuration");
                                        handled = true;
                                        break;
                                    case SpheroConstants.configureSensorStream:
                                        Log.v(TAG, "AWK: sensor stream configuration");
                                        handled = true;
                                        break;
                                    case SpheroConstants.sensorMask:
                                        Log.v(TAG, "AWK: sensor mask configuration");
                                        handled = true;
                                        break;
                                }
                                break;
                        }

                    }
//                    else { }// collision detection, sensor reading, etc.

                    if(handled) {
                        Log.i(TAG, stringifyBytes(buffer, bufferLength));
                        if(waitForResponse) {

                            // Received acknowledgement command.
                            // Seek the command queue for the command that sent it.
                            // Remove it from the queue.
                            Iterator<ResponseCommand> iter = commandQueue.iterator();
                            ResponseCommand removedCommand = null;
                            while(iter.hasNext()) {
                                ResponseCommand el = iter.next();
                                if(el.getSequence() == sequence) {
                                    removedCommand = el;
                                    removedCommand.markReceived();
                                    iter.remove();
                                    break;
                                }
                            }
                            if(removedCommand != null) {
                                Log.v(TAG, "Found matching command. Queue size: " + commandQueue.size() + " | time taken: " + removedCommand.timeSinceSent() + ", since queued: " + removedCommand.timeSinceQueued());
                            } else {
                                Log.e(TAG, "Found no matching command! Queue size: " + commandQueue.size() + " | sequence: " + String.format("%02X ", sequence));
                                Log.e(TAG, "Perhaps it was resent/discarded after timing out, and this acknowledgement was for the original message.");
                            }
                            // Because the Sphero has acknowledged a command, it is now ready(?) for the next one.
                            // Immediately send the next command, if it exists.
                            if(commandQueue.size() > 0) {
                                send_nextCommandInQueue();
                            }
                        }
                    } else {
                        Log.v(TAG, "Received unprocessed bytes: " + stringifyBytes(buffer, bufferLength));
                    }
                    clear_buffer();
                } else {
                    // Middle of a command. Keep waiting for the command to finish.
                    // however, if the start of the buffer isn't a start byte, something went wrong.
                    if(buffer[0] != SpheroConstants.start) {
                        clear_buffer();
                    }
                }
            }
        }
        private void clear_buffer() {
            for(int i = 0; i < buffer.length; i++) {
                buffer[i] = 0x00;
            }
            bufferLength = 0;
        }

        // Removes commands from the queue that match deviceId.
        // Excludes the command at the front of the queue, which has already been sent
        // and is waiting for a response/timeout. If it times out, don't send it again.
        private void removeCommandsOfType(byte deviceID, byte commandID) {
            Iterator<ResponseCommand> iter = commandQueue.iterator();
            if(iter.hasNext()) {
                // Don't resend this command
                ResponseCommand first = iter.next();
                first.stopResending();
            }
            while(iter.hasNext()) {
                ResponseCommand el = iter.next();

                if(el.getDeviceID() == deviceID && el.getCommandID() == commandID) {
                    el.stopResending();
                    iter.remove();
                }
            }
        }

        // Converts a byte array into a string of hex numbers representing their bytes.
        // Used for debugging to display messages sent/received.
        private String stringifyBytes(final byte[] data, int length) {
            if((data != null && data.length > 0) || length < 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(int i = 0; i < length && i < data.length; i++)
                    stringBuilder.append(String.format("%02X ", data[i]));

                return stringBuilder.toString();
            }
            return "";
        }

        // A command used in the response queue for keeping track of commands that are awaiting responses.
        // Has a timer that resends the message if it times out.
        private class ResponseCommand {
            // Data in the message that was sent
            private byte deviceID;
            private byte commandID;
            private byte[] payload;
            private byte sequence;
            // Set to true when the message is received so that it won't be resent
            private boolean received, discarded;
            // True for roll commands. Used to optimize the right to discard unnecessary messages.
            private boolean continuous;
            // Number of times this command has been sent/resent
            private int sendAttemptsCount;
            // Timestamps of when the message was queued and sent (used for debugging)
            private long timeWasQueued;
            private long timeWasSent;

            public ResponseCommand(byte deviceID, byte commandID, byte[] payload, byte sequence, boolean continuous, int timesSent, long currentTime) {
                this.deviceID = deviceID;
                this.commandID = commandID;
                this.payload = payload.clone();
                this.sequence = sequence;
                this.continuous = continuous;
                received = false;
                discarded = false;
                sendAttemptsCount = timesSent;
                this.timeWasQueued = currentTime;
            }

            public int getDeviceID() { return deviceID; }
            public int getCommandID() { return commandID; }
            public int getSequence() {
                return sequence;
            }
            public boolean getContinuous() {
                return continuous;
            }
            public boolean getReceived() {
                return received;
            }
            public boolean isDiscarded() {
                return discarded;
            }
            public int getSendAttemptsCount() {
                return sendAttemptsCount;
            }
            public void setTimeWasSent(long time) {
                timeWasSent = time;
            }
            public void markReceived() {
                Log.v(TAG, "Marking as received. | sequence: " + String.format("%02X ", sequence));
                received = true;
            }
            // Don't resend this command if it times out
            public void stopResending() {
                discarded = true;
            }
            public long timeSinceSent() {
                return System.currentTimeMillis() - timeWasSent;
            }
            public long timeSinceQueued() {
                return System.currentTimeMillis() - timeWasQueued;
            }
        }

        // Handles the actual connection to the sphero.
        // Reads and writes to the sphero over Bluetooth.
        private class BluetoothSpheroController {
            public static final String TAG = "BluetoothSpheroController";
            public final static String ACTION_GATT_CONNECTED =
                    "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
            public final static String ACTION_GATT_DISCONNECTED =
                    "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
            public final static String ACTION_GATT_SERVICES_DISCOVERED =
                    "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
            public final static String ACTION_DATA_AVAILABLE =
                    "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
            public final static String ACTION_WRITE_SUCCESSFUL =
                    "com.example.bluetooth.le.ACTION_WRITE_SUCCESSFUL";
            public final static String EXTRA_DATA =
                    "com.example.bluetooth.le.EXTRA_DATA";
            public final static String EXTRA_DATA_UUID =
                    "com.example.bluetooth.le.EXTRA_DATA_UUID";

//            private static final int STATE_DISCONNECTED = 0;
//            private static final int STATE_CONNECTED = 2;

            private BluetoothAdapter bluetoothAdapter;
            private BluetoothGatt bluetoothGatt;

            // GATT server callbacks.
            // When messages are successfully sent to or received from the sphero,
            // one of these callback functions will get called.
            private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            // successfully connected to the GATT Server
                            broadcastUpdate(ACTION_GATT_CONNECTED);
                            Log.i(TAG, "Connected to the GATT Server.");
                            // Attempts to discover services after successful connection.
                            if (ActivityCompat.checkSelfPermission(parentContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                return;
                            }
                            bluetoothGatt.discoverServices();
                        } else {
                            Log.e(TAG, "Failed to connect to the GATT Server: error " + status);
                            broadcastUpdate(ACTION_GATT_DISCONNECTED);
                        }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            // disconnected from the GATT Server
                            broadcastUpdate(ACTION_GATT_DISCONNECTED);
                            Log.i(TAG, "Disconnected from the GATT Server.");
                        } else {
                            Log.e(TAG, "Failed to connect to/disconnect from the GATT Server: error " + status);
                            broadcastUpdate(ACTION_GATT_DISCONNECTED);
                        }
                    } else {
                        Log.e(TAG, "Unknown GATT connection state: " + newState);
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                        Log.w(TAG, "onServicesDiscovered broadcasting success...");
                    } else {
                        Log.w(TAG, "onServicesDiscovered received unsuccessful: " + status);
                    }
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                        Log.d(TAG, " <- " + SpheroGattAttributes.lookup(String.valueOf(characteristic.getUuid())));
                    } else {
                        Log.d(TAG, "Characteristic read unsuccessful.");
                    }
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.d(TAG, " -> " + SpheroGattAttributes.lookup(String.valueOf(characteristic.getUuid())));
                    } else {
                        Log.d(TAG, "Characteristic write unsuccessful.");
                    }
                    broadcastUpdate(ACTION_WRITE_SUCCESSFUL, characteristic);
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                }

                // Prepares intents to broadcast the received messages up to the parent context.
                private void broadcastUpdate(final String action) {
                    final Intent intent = new Intent(action);
                    parentContext.sendBroadcast(intent);
                }

                private void broadcastUpdate(final String action,
                                             final BluetoothGattCharacteristic characteristic) {
                    final Intent intent = new Intent(action);

                    intent.putExtra(EXTRA_DATA, characteristic.getValue());
                    intent.putExtra(EXTRA_DATA_UUID, characteristic.getUuid().toString());

                    parentContext.sendBroadcast(intent);
                }
            };


            public BluetoothSpheroController() {
                this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            }

            public boolean initializeAdapter() {
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (bluetoothAdapter == null) {
                    Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
                    return false;
                } else {
                    Log.i(TAG, "Bluetooth adapter found by BluetoothSpheroController.");
                }
                return true;
            }

            public boolean connect(final String address) {
                if (bluetoothAdapter == null || address == null) {
                    Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
                    return false;
                }
                // connect to the GATT server on the device
                try {
                    final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
                    Thread.sleep(500);
                    if (bluetoothGatt == null) {
                        if (ActivityCompat.checkSelfPermission(parentContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            return false;
                        }
                        bluetoothGatt = device.connectGatt(parentContext, false, bluetoothGattCallback);
                        Log.i(TAG, "Connecting to new GATT server.");
                    } else {
                        bluetoothGatt.connect();
                        Log.i(TAG, "Reconnecting to existing GATT server.");
                    }
                    // Now, wait for bluetooth GATT callbacks: ACTION_GATT_CONNECTED
                    return true;
                } catch (IllegalArgumentException | InterruptedException exception) {
                    Log.w(TAG, "Device not found with provided address.");
                    return false;
                }
            }

            public boolean disconnect() {
                if (bluetoothAdapter == null) {
                    Log.w(TAG, "BluetoothAdapter not initialized.");
                    return false;
                }
                if (bluetoothGatt != null) {
                    if (ActivityCompat.checkSelfPermission(parentContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return false;
                    }
                    bluetoothGatt.disconnect();
                }
                return true;
            }

            public List<BluetoothGattService> getSupportedGattServices() {
                if (bluetoothGatt == null) return null;
                return bluetoothGatt.getServices();
            }

            // Read a value from a characteristic. The value will appear in the GATT callback.
            // Not as necessary if you are getting characteristic notifications already,
            // that is, if you've setCharacteristicNotification to true.
            public boolean readCharacteristic(BluetoothGattCharacteristic characteristic) {
                if (bluetoothGatt == null) {
                    Log.w(TAG, "BluetoothGatt not initialized");
                    return false;
                }
                if (characteristic == null) {
                    Log.e(TAG, "Characteristic not initialized");
                    return false;
                }
                if (ActivityCompat.checkSelfPermission(parentContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
                bluetoothGatt.readCharacteristic(characteristic);
                return true;
            }

            // Send a byte payload to a characteristic.
            public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value) {
                if (bluetoothGatt == null) {
                    Log.w(TAG, "BluetoothGatt not initialized");
                    return false;
                }
                if(characteristic == null) {
                    Log.e(TAG, "Characteristic not initialized");
                    return false;
                }
                if (ActivityCompat.checkSelfPermission(parentContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
                characteristic.setValue(value);
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                bluetoothGatt.writeCharacteristic(characteristic);

                return true;
            }

            // Activates characteristic notifications for a given characteristic.
            // Important for getting message acknowledgement responses.
            public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
                if (bluetoothGatt == null) {
                    Log.w(TAG, "BluetoothGatt not initialized");
                    return false;
                }
                if(characteristic == null) {
                    Log.e(TAG, "Characteristic not initialized");
                    return false;
                }
                if (ActivityCompat.checkSelfPermission(parentContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
                if(bluetoothGatt.setCharacteristicNotification(characteristic, enabled)) {
                    if(characteristic.getUuid().toString().equals(SpheroGattAttributes.API_V2_characteristic)) {
                        BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(UUID.fromString(SpheroGattAttributes.Client_characteristic_config));
                        if(clientConfig == null) {
                            Log.e(TAG, "Failed to get clientConfig for API_V2");
                            return false;
                        }
                        clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        bluetoothGatt.writeDescriptor(clientConfig);
                    } else if(characteristic.getUuid().toString().equals(SpheroGattAttributes.DFU_characteristic)) {
                        BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(UUID.fromString(SpheroGattAttributes.Client_characteristic_config));
                        if(clientConfig == null) {
                            Log.e(TAG, "Failed to get clientConfig for DFU");
                            return false;
                        }
                        clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        bluetoothGatt.writeDescriptor(clientConfig);
                    }
                } else {
                    Log.e(TAG, "Set characteristic notification failed.");
                    return false;
                }
                return true;
            }


            private void close() {
                Log.i(TAG, "Closing BLE service.");
                if (bluetoothGatt == null) {
                    return;
                }
                if (ActivityCompat.checkSelfPermission(parentContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothGatt.close();
                    bluetoothGatt = null;
                }
            }
        }
    }
}
