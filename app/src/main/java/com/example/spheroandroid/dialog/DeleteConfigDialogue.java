package com.example.spheroandroid.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import com.example.spheroandroid.R;
import com.example.spheroandroid.MainActivity;

// Dialog box for confirming to delete device configuration.
public class DeleteConfigDialogue extends DialogFragment {

    MainActivity activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            // Be able to send events to the host
            activity = (MainActivity) context;
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
        builder.setMessage(R.string.deleteConfig)
                .setTitle(R.string.deleteConfigTitle)
                .setPositiveButton(R.string.yes, (dialog, id) -> activity.onConfigDeleteListener())
                .setNegativeButton(R.string.no, (dialog, id) -> {});
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
