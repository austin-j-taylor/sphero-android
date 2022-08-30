package com.example.spheroandroid;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

// The list of Sphero devices that are presented on the main activity.
// Clicking on a sphero device opens the activity that controls it.
public class DeviceRecyclerAdapter extends RecyclerView.Adapter<DeviceRecyclerAdapter.DeviceViewHolder> {

    private static final String TAG = "DeviceRecyclerAdapter";
    public static final String EXTRA_DEVICE_ADDRESS =
            "com.example.sphero-android.DeviceRecyclerAdapter.EXTRA_DEVICE_ADDRESS";
    public static final String EXTRA_DEVICE_INDEX =
            "com.example.sphero-android.DeviceRecyclerAdapter.EXTRA_DEVICE_INDEX";

    String[] deviceNames, deviceStatuses, deviceAddresses;
    MainActivity context;

    public DeviceRecyclerAdapter(MainActivity context, String[] deviceNames, String[] deviceStatuses, String[] deviceAddresses) {
        this.context = context;
        this.deviceNames = deviceNames; // shouldn't have to worry about shallow copy
        this.deviceStatuses = deviceStatuses;
        this.deviceAddresses = deviceAddresses;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.device_row, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        holder.text_deviceName.setText(deviceNames[position]);
        holder.text_deviceStatus.setText(deviceStatuses[position]);
        holder.itemView.findViewById(R.id.button_item).setOnClickListener(view -> {
            if(holder.text_deviceName.getText().equals("Sphero Mini")) {

                // If the MAC address is specified, go into the control activity.
                if(deviceAddresses[holder.getAdapterPosition()].length() > 0) {

                    Intent intent = new Intent(context, SpheroMiniActivity.class);
                    intent.putExtra(EXTRA_DEVICE_ADDRESS, deviceAddresses[holder.getAdapterPosition()]);
                    context.startActivity(intent);
                } else {
                    // If the MAC address is not specified, go into the address configuring activity.
                    holder.startAddressActivity();
                }
            } else {
                // Sphero Bolt. Not yet implemented
            }
        });
    }

    @Override
    public int getItemCount() {
        return deviceNames.length;
    }

    public class DeviceViewHolder extends RecyclerView.ViewHolder {

        TextView text_deviceName, text_deviceStatus;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            text_deviceName = itemView.findViewById(R.id.text_deviceName);
            text_deviceStatus = itemView.findViewById(R.id.text_deviceStatus);


            ImageView settingsButton = itemView.findViewById(R.id.imageView_deviceSettings);
            settingsButton.setOnClickListener(view -> {
                // Go into the address configuring activity.
                startAddressActivity();
            });

        }
        // Updates the Holder to reflect the data behind it
        public void refresh() {
            text_deviceName.setText(deviceNames[getAdapterPosition()]);
            text_deviceStatus.setText(deviceStatuses[getAdapterPosition()]);
        }
        // Start the MAC address selection activity to change the address for this device.
        private void startAddressActivity() {
            Intent intent = new Intent(context, AddressActivity.class);
            intent.putExtra(EXTRA_DEVICE_ADDRESS, deviceAddresses[getAdapterPosition()]);
            intent.putExtra(EXTRA_DEVICE_INDEX, getAdapterPosition());
            context.mStartForResult.launch(intent);

//        Intent intent = new Intent(context, AddressActivity.class);
//        context.startActivity(intent);
        }
    }
}
