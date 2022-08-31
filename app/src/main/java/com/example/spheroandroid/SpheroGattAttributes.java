package com.example.spheroandroid;

import java.util.HashMap;

// List of the Sphero Mini GATT characteristics to know where to send messages.
// Most of the communication happens over the API_V2 characteristic.
public class SpheroGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String API_V2_characteristic =    "00010002-574f-4f20-5370-6865726f2121";
    public static String AntiDOS_characteristic =   "00020005-574f-4f20-5370-6865726f2121";
    public static String DFU_characteristic =       "00020002-574f-4f20-5370-6865726f2121";
    public static String DFU2_characteristic =      "00020004-574f-4f20-5370-6865726f2121";
    public static String Client_characteristic_config = "00002902-0000-1000-8000-00805f9b34fb";

    static {
        // Sample Services.
        // Sample Characteristics.
        attributes.put(API_V2_characteristic, "API_V2_characteristic");
        attributes.put(AntiDOS_characteristic, "AntiDOS_characteristic");
        attributes.put(DFU_characteristic, "DFU_characteristic");
        attributes.put(DFU2_characteristic, "DFU2_characteristic");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
    public static String lookup(String uuid) {
        String name = attributes.get(uuid);
        return name == null ? "Unknown" : name;
    }
}