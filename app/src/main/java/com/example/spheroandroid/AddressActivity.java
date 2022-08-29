package com.example.spheroandroid;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

// Locates sphero by entering and saving the MAC address.
// Modified from https://github.com/r-cohen/macaddress-edittext
public class AddressActivity extends AppCompatActivity {

    private static final int MAC_ADDRESS_LENGTH = 17;

    private EditText editText_address;
    private TextView textView_addressStatus;

    private String previousMac = null;
    private int deviceIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address);

        // Get existing MAC address from intent and put it in the field
        editText_address = findViewById(R.id.editText_address);
        textView_addressStatus = findViewById(R.id.textView_addressStatus);
        String address = getIntent().getStringExtra(DeviceRecyclerAdapter.EXTRA_DEVICE_ADDRESS);
        deviceIndex = getIntent().getIntExtra(DeviceRecyclerAdapter.EXTRA_DEVICE_INDEX, 0);
        if(address.length() == MAC_ADDRESS_LENGTH) {
            editText_address.setText(address);
        }

        editText_address.addTextChangedListener(new TextWatcher() {

            String enteredMac, cleanMac, formattedMac;
            int selectionStart, lengthDiff;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                enteredMac = editText_address.getText().toString().toUpperCase();
                cleanMac = clearNonMacCharacters(enteredMac);
                formattedMac = formatMacAddress(cleanMac);
                selectionStart = editText_address.getSelectionStart();
                formattedMac = handleColonDeletion(enteredMac, formattedMac, selectionStart);
                lengthDiff = formattedMac.length() - enteredMac.length();
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (cleanMac.length() <= 12) {
                    previousMac = formattedMac;
                    editText_address.removeTextChangedListener(this);
                    editText_address.setText(formattedMac);
                    editText_address.addTextChangedListener(this);
                    editText_address.setSelection(selectionStart + lengthDiff);
                } else {
                    editText_address.removeTextChangedListener(this);
                    editText_address.setText(previousMac);
                    editText_address.addTextChangedListener(this);
                    editText_address.setSelection(previousMac.length());
                }
            }
        });
    }

    public void onClickButton_save(View view) {
        if(editText_address.getText().length() == MAC_ADDRESS_LENGTH) {
            Intent intent = new Intent();
            intent.putExtra(DeviceRecyclerAdapter.EXTRA_DEVICE_ADDRESS, editText_address.getText().toString());
            intent.putExtra(DeviceRecyclerAdapter.EXTRA_DEVICE_INDEX, deviceIndex);
            setResult(Activity.RESULT_OK, intent);
            finish();
        } else {
            textView_addressStatus.setText("Invalid format.");
        }
    }

    private String handleColonDeletion(String enteredMac, String formattedMac, int selectionStart) {
        if (previousMac != null && previousMac.length() > 1) {
            int previousColonCount = colonCount(previousMac);
            int currentColonCount = colonCount(enteredMac);

            if (currentColonCount < previousColonCount) {
                formattedMac = formattedMac.substring(0, selectionStart - 1) + formattedMac.substring(selectionStart);
                String cleanMac = clearNonMacCharacters(formattedMac);
                formattedMac = formatMacAddress(cleanMac);
            }
        }
        return formattedMac;
    }

    private static String formatMacAddress(String cleanMac) {
        int groupedCharacters = 0;
        StringBuilder formattedMac = new StringBuilder();
        for (int i = 0; i < cleanMac.length(); ++i) {
            formattedMac.append(cleanMac.charAt(i));
            groupedCharacters++;
            if (groupedCharacters == 2) {
                formattedMac.append(":");
                groupedCharacters = 0;
            }
        }
        if (cleanMac.length() == 12) {
            return formattedMac.substring(0, formattedMac.length() - 1);
        }
        return formattedMac.toString();
    }

    private static String clearNonMacCharacters(String mac) {
        return mac.replaceAll("[^A-Fa-f0-9]", "");
    }

    private static int colonCount(String formattedMac) {
        return formattedMac.replaceAll("[^:]", "").length();
    }

}