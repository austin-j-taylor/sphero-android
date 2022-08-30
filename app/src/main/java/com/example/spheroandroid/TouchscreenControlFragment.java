package com.example.spheroandroid;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.ToggleButton;


public class TouchscreenControlFragment extends Fragment {

    public static final String TAG = "TouchscreenControl";

    private SpheroMiniViewModel viewModel;

    private ImageView touchJoystick, touchJoystickCore, touchJoystickCoreEmpty;
    private ToggleButton button_awake;
    private Button button_heading;
    private SeekBar seekBar_speed, seekBar_brightness, seekBar_color;

    public TouchscreenControlFragment() {
        // Required empty public constructor
    }

    // Factory method to create an instance of this fragment
    public static TouchscreenControlFragment newInstance() {
        TouchscreenControlFragment fragment = new TouchscreenControlFragment();
//        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
//        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (getArguments() != null) {
//            mParam1 = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
//        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_touchscreen_control, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(SpheroMiniViewModel.class);

        viewModel.getConnectionState().observe(getViewLifecycleOwner(), connectionState -> observeChangeConnectionState());
        viewModel.getSpeed().observe(getViewLifecycleOwner(), speed -> observeChangeSpeed());
        viewModel.getLedBrightness().observe(getViewLifecycleOwner(), ledBrightness -> observeChangeLedBrightness());
        viewModel.getLedColor().observe(getViewLifecycleOwner(), ledHue -> observeChangeLedColor());
        viewModel.getAwake().observe(getViewLifecycleOwner(), awake -> observeChangeAwake());

        button_awake = getView().findViewById(R.id.button_awake);
        button_heading = getView().findViewById(R.id.button_heading);
        seekBar_speed = getView().findViewById(R.id.seekBar_speed);
        seekBar_brightness = getView().findViewById(R.id.seekBar_brightness);
        seekBar_color = getView().findViewById(R.id.seekBar_color);
        touchJoystick = getView().findViewById(R.id.image_touchJoystick);
        touchJoystickCore = getView().findViewById(R.id.image_touchJoystickCore);
        touchJoystickCoreEmpty = getView().findViewById(R.id.image_touchJoystickCoreEmpty);

        // UI callbacks
        seekBar_speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                viewModel.setSpeed(i);
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        seekBar_brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                viewModel.setLedBrightness(i);
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        seekBar_color.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(i == 100) {
                    viewModel.setLedColor(Color.WHITE);
                } else {
                    // Convert slider to RGB color
                    float[] hsv = {(i * 360) / 100, 1, 1};
                    int argb = Color.HSVToColor(hsv);
                    viewModel.setLedColor(argb);
                }
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });


        // Touch listener for touch joystick
        touchJoystick.setOnTouchListener((touchView, event) -> {
            switch(event.getActionMasked()) {
                case MotionEvent.ACTION_MOVE:
                    float view_x = event.getX() - (touchView.getWidth())/2;
                    float view_y = event.getY() - (touchView.getHeight())/2;
                    // Clamp joystick core to stay within the circle
                    double magnitude = Math.sqrt(view_x * view_x + view_y * view_y);
                    double maxMagnitude = (touchView.getWidth() - touchJoystickCore.getWidth())/2;
                    if(magnitude > maxMagnitude) {
                        view_x = (float)(view_x / magnitude * maxMagnitude);
                        view_y = (float)(view_y / magnitude * maxMagnitude);
                    }
                    touchJoystickCore.animate()
                            // .x() and .y() take parent-constraint-space inputs
                            // parent-constraint-space Joystick position + (Offsets to reach center of joystick) + local-origin-centered touch position
                            .x(touchView.getX() + ((touchView.getWidth() - touchJoystickCore.getWidth()) / 2) + view_x)
                            .y(touchView.getY() + ((touchView.getHeight() - touchJoystickCore.getHeight()) / 2) + view_y)
                            .setDuration(0).start();

                    // range: [-1, +1] and a radius of 1
                    double roll_x = view_x / maxMagnitude;
                    double roll_y = view_y / maxMagnitude;

                    viewModel.postRoll(roll_x, roll_y);
                    break;
                case MotionEvent.ACTION_UP:
//                    touchJoystick.setImageResource(R.drawable.switch_stick_empty);
                    touchJoystickCore.animate().x(touchJoystickCoreEmpty.getX()).y(touchJoystickCoreEmpty.getY()).setDuration(0).start();

                    // Stop setting heading, or stop rolling
                    if(Boolean.TRUE.equals(viewModel.getResettingHeading().getValue())) // null check
                        viewModel.setResettingHeading(false);
                    else
                        viewModel.postRoll(0, 0);

                    return true;
            }

            return true;
        });
    }

    private void observeChangeConnectionState() {
        // TODO Reset awake button state when disconnecting
    }
    private void observeChangeSpeed() {
        // TODO  Set slider position
    }
    private void observeChangeLedBrightness() {
        // TODO  Set slider position
    }
    private void observeChangeLedColor() {
        // TODO  Set slider position
    }
    private void observeChangeAwake() {
        // TODO  Set awake button state
    }
}