package com.example.spheroandroid;

// API for communicating with the sphero.
// Create a SpheroController object to start setting up communication with it.
// Creates a worker thread to handle Bluetooth communication off of the main UI thread.
public class SpheroController {

    public static final String TAG = "SpheroController";
    public final static String ACTION_BATTERY_AVAILABLE =
            "com.example.controlapp.SpheroController.ACTION_BATTERY_AVAILABLE";
    public final static String EXTRA_BATTERY_VALUE =
            "com.example.controlapp.SpheroController.EXTRA_BATTERY_VALUE";

    public SpheroController() {

    }

    // Clean up this sphero controller. Close the bluetooth server.
    public void destroy() {

    }

}
