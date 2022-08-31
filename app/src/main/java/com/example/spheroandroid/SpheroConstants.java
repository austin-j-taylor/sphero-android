package com.example.spheroandroid;

// Contains the constant byte codes used in sending and receiving commands over BLE to the sphero.
// Excludes several unknown values.
// Values borrowed from from https://github.com/MProx/Sphero_mini/blob/master/sphero_constants.py
// From
        /*
        Packet structure has the following format (in order):

                - Start byte: always 0x8D
                - Flags byte: indicate response required, etc
        - Virtual device ID: see sphero_constants.py
        - Command ID: specifies the exact command for the sphero to run. See below.
        - Sequence number: Seems to be arbitrary. I suspect it is used to match commands to response packets (in which the number is echoed).
                - Payload: Could be varying number of bytes (incl. none), depending on the command
        - Checksum: See below for calculation
        - End byte: always 0xD8
        */
public class SpheroConstants {

    // Device IDs
    public final static byte apiProcessor = 0x10;
    public final static byte systemInfo = 0x11;
    public final static byte powerInfo = 0x13;
    public final static byte driving = 0x16;
    public final static byte animatronics = 0x17;
    public final static byte sensor = 0x18;
    public final static byte userIO = 0x1a;

    // System info commands

    public final static byte mainApplicationVersion = 0x00;
    public final static byte bootloaderVersion = 0x01;

    // Start/end of packet constants
    public final static byte start = (byte)0x8d;
    public final static byte end = (byte)0xd8;

    // User IO command IDs
    public final static byte allLEDs = 0x0e;

    // Flags
    public final static byte isResponse = 0x01;
    public final static byte requestsResponse = 0x02;
    public final static byte requestsOnlyErrorResponse = 0x04;
    public final static byte resetsInactivityTimeout = 0x08;

    // Power command IDs
    public final static byte deepSleep = 0x00;
    public final static byte sleep = 0x01;
    public final static byte batteryVoltage = 0x03;
    public final static byte wake = 0x0D;

    // Driving commands
    public final static byte rawMotor = 0x01;
    public final static byte resetHeading = 0x06;
    public final static byte driveAsSphero = 0x04;
    public final static byte driveAsRc = 0x02;
    public final static byte driveWithHeading = 0x07;
    public final static byte stabilization = 0x0C;

    // Sensor commands
    public final static byte sensorMask = 0x00;
    public final static byte sensorResponse = 0x02;
    public final static byte configureCollision = 0x11;
    public final static byte collisionDetectedAsync = 0x12;
    public final static byte resetLocator = 0x13;
    public final static byte enableCollisionAsync = 0x14;
    public final static byte sensor1 = 0x0F;
    public final static byte sensor2 = 0x17;
    public final static byte configureSensorStream = 0x0C;


}
