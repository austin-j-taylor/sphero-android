package com.example.spheroandroid;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

// The list of Sphero devices that are presented on the main activity.
// Clicking on a sphero device opens the activity that controls it.
public class DeviceRecyclerAdapter extends RecyclerView.Adapter<DeviceRecyclerAdapter.DeviceViewHolder> {

    String[] deviceNames, deviceStatuses;
    Context context;

    public DeviceRecyclerAdapter(Context context, String deviceNames[], String deviceStatuses[]) {
        this.context = context;
        this.deviceNames = deviceNames; // shouldn't have to worry about shallow copy
        this.deviceStatuses = deviceStatuses;
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
        holder.itemView.findViewById(R.id.button_item).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(holder.text_deviceName.getText().equals("Sphero Mini")) {
                    Intent intent = new Intent(context, SpheroMiniActivity.class);
                    context.startActivity(intent);
                } else {
                    // not yet implemented
                }
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
        }
    }
}
