package com.example.spheroandroid.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import com.example.spheroandroid.R;
import com.example.spheroandroid.SpheroMiniActivity;

// Dialogue box for when a scan is performed and the sphero is not found.
public class FailedScanDialogue extends DialogFragment {

    SpheroMiniActivity activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            // Be able to send events to the host
            activity = (SpheroMiniActivity) context;
        } catch (ClassCastException e) {
            // Wrong activity, throw exception
            throw new ClassCastException(activity.toString()
                    + " is not the correct activity.");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.scanFailed)
                .setPositiveButton(R.string.close, (dialog, id) -> { });
        // Create the AlertDialog object and return it
        return builder.create();
    }
    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        super.onDismiss(dialogInterface);
        activity.onScanDismissListener();
    }
}