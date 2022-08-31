package com.example.spheroandroid;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class GamepadControlFragment extends Fragment {

    public static final String TAG = "GamepadControl";

    // Decides what controls to display, indicating the available actions.
    private enum GamepadMode { DISCONNECTED, ASLEEP, DRIVE, LED_CONFIGURATION }

    private static final int OPACITY_USED = 255;
    private static final int OPACITY_UNUSED = 63;
    // How far the sticks should appear to move when titled
    private final static int PIXELS_THUMBSTICK = 75;
    // Speed factor applied when for holding down certain buttons
    private final static int SPEED_SLOW = 30;
    private final static int SPEED_NORMAL = 60;
    private final static int SPEED_FAST = 100;

    private GamepadMode mode;
    private boolean joystickDeadbanded;
    private boolean buttonPressedY, buttonPressedB;

    private SpheroMiniViewModel viewModel;
    private ImageView stick_L;
    private ImageView stickEmpty_L;
    private ImageView stick_R;
    private ImageView stickEmpty_R;
    private ImageView image_a, image_b, image_x, image_y, image_L1, image_R1, image_plus, image_minus;
    private TextView text_gamepadB, text_gamepadX, text_gamepadY, text_gamepadStickLeft, text_gamepadStickRight,
            text_gamepadConnect, text_gamepadModeSelected0, text_gamepadModeSelected1,
            text_gamepadMode0, text_gamepadMode1, text_gamepadR1;

    public GamepadControlFragment() {
        // Required empty public constructor
    }

    public static GamepadControlFragment newInstance() {
        GamepadControlFragment fragment = new GamepadControlFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_gamepad_control, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(SpheroMiniViewModel.class);

        viewModel.getConnectionState().observe(getViewLifecycleOwner(), connectionState -> observeChangeConnectionState());
        viewModel.getSpeed().observe(getViewLifecycleOwner(), speed -> observeChangeSpeed());
        viewModel.getLedBrightness().observe(getViewLifecycleOwner(), ledBrightness -> observeChangeLedBrightness());
        viewModel.getLedColor().observe(getViewLifecycleOwner(), ledColor -> observeChangeLedColor());
        viewModel.getAwake().observe(getViewLifecycleOwner(), awake -> observeChangeAwake());

        stick_L = view.findViewById(R.id.image_stick_L);
        stickEmpty_L = view.findViewById(R.id.image_stickEmpty_L);
        stick_R = view.findViewById(R.id.image_stick_R);
        stickEmpty_R = view.findViewById(R.id.image_stickEmpty_R);
        image_a = view.findViewById(R.id.image_a);
        image_b = view.findViewById(R.id.image_b);
        image_x = view.findViewById(R.id.image_x);
        image_y = view.findViewById(R.id.image_y);
        image_L1 = view.findViewById(R.id.image_L1);
        image_R1 = view.findViewById(R.id.image_R1);
        image_plus = view.findViewById(R.id.image_plus);
        image_minus = view.findViewById(R.id.image_minus);

        text_gamepadB = view.findViewById(R.id.text_gamepadB);
        text_gamepadX = view.findViewById(R.id.text_gamepadX);
        text_gamepadY = view.findViewById(R.id.text_gamepadY);
        text_gamepadStickLeft = view.findViewById(R.id.text_gamepadStickLeft);
        text_gamepadStickRight = view.findViewById(R.id.text_gamepadStickRight);
        text_gamepadConnect = view.findViewById(R.id.text_gamepadConnect);
        text_gamepadModeSelected0 = view.findViewById(R.id.text_gamepadModeSelected0);
        text_gamepadModeSelected1 = view.findViewById(R.id.text_gamepadModeSelected1);
        text_gamepadMode0 = view.findViewById(R.id.text_gamepadMode0);
        text_gamepadMode1 = view.findViewById(R.id.text_gamepadMode1);
        text_gamepadR1 = view.findViewById(R.id.text_gamepadR1);

        joystickDeadbanded = true;
        buttonPressedY = false;
        buttonPressedB = false;
        observeChangeConnectionState();

        view.setOnGenericMotionListener((view1, motionEvent) -> fragmentOnGenericMotionEvent(motionEvent));
        view.setOnKeyListener((view12, i, keyEvent) -> fragmentOnKeyEvent(keyEvent));
    }

    private void observeChangeConnectionState() {
        // Hide most controls when disconnected
        switch(viewModel.getConnectionState().getValue()) {
            case DISCONNECTED:
                text_gamepadConnect.setText(R.string.connect);
                image_plus.setImageAlpha(OPACITY_USED);
                setGamepadMode(GamepadMode.DISCONNECTED);
                break;
            case CONNECTING:
                text_gamepadConnect.setText(R.string.connecting);
                image_plus.setImageAlpha(OPACITY_UNUSED);
                break;
            case CONNECTED:
                text_gamepadConnect.setText(R.string.disconnect);
                image_plus.setImageAlpha(OPACITY_USED);
                setGamepadMode(GamepadMode.ASLEEP);
                break;
            case DISCONNECTING:
                text_gamepadConnect.setText(R.string.disconnecting);
                image_plus.setImageAlpha(OPACITY_UNUSED);
                setGamepadMode(GamepadMode.DISCONNECTED);
                break;
        }
    }

    private void observeChangeSpeed() {
        // Do nothing
    }

    private void observeChangeLedBrightness() {
    }

    private void observeChangeLedColor() {
    }

    private void observeChangeAwake() {
        // Change Wake Up/Sleep text
        if(viewModel.getAwake().getValue()) {
            setGamepadMode(GamepadMode.DRIVE);
        } else {
            setGamepadMode(GamepadMode.ASLEEP);
        }
    }

    private void setGamepadMode(GamepadMode newMode) {
        // Change joystick/button opacity and labels
        mode = newMode;
        switch(mode) {
            case DISCONNECTED:
                // Disconnected. Hide everything except the connect button.
                text_gamepadR1.setVisibility(View.INVISIBLE);
                image_R1.setImageAlpha(OPACITY_UNUSED);

                text_gamepadModeSelected0.setVisibility(View.INVISIBLE);
                text_gamepadModeSelected1.setVisibility(View.INVISIBLE);
                text_gamepadMode0.setTypeface(text_gamepadMode0.getTypeface(), Typeface.NORMAL);
                text_gamepadMode1.setTypeface(text_gamepadMode1.getTypeface(), Typeface.NORMAL);

                text_gamepadStickLeft.setText("");
                text_gamepadStickRight.setText("");
                stick_L.setImageAlpha(OPACITY_UNUSED);
                stick_R.setImageAlpha(OPACITY_UNUSED);

                text_gamepadB.setVisibility(View.INVISIBLE);
                text_gamepadB.setText("");
                text_gamepadX.setVisibility(View.INVISIBLE);
                text_gamepadX.setText("");
                text_gamepadY.setVisibility(View.INVISIBLE);
                text_gamepadY.setText("");
                image_b.setImageAlpha(OPACITY_UNUSED);
                image_x.setImageAlpha(OPACITY_UNUSED);
                image_y.setImageAlpha(OPACITY_UNUSED);
                break;
            case ASLEEP:
                // Asleep. Hide everything except the wake up and disconnect button.
                text_gamepadR1.setVisibility(View.INVISIBLE);
                image_R1.setImageAlpha(OPACITY_UNUSED);

                text_gamepadModeSelected0.setVisibility(View.INVISIBLE);
                text_gamepadModeSelected1.setVisibility(View.INVISIBLE);
                text_gamepadMode0.setTypeface(text_gamepadMode0.getTypeface(), Typeface.NORMAL);
                text_gamepadMode1.setTypeface(text_gamepadMode1.getTypeface(), Typeface.NORMAL);

                text_gamepadStickLeft.setText("");
                text_gamepadStickRight.setText("");
                stick_L.setImageAlpha(OPACITY_UNUSED);
                stick_R.setImageAlpha(OPACITY_UNUSED);

                text_gamepadB.setVisibility(View.INVISIBLE);
                text_gamepadB.setText("");
                text_gamepadX.setVisibility(View.VISIBLE);
                text_gamepadX.setText(R.string.gamepad_wake);
                text_gamepadY.setVisibility(View.INVISIBLE);
                text_gamepadY.setText("");
                image_b.setImageAlpha(OPACITY_UNUSED);
                image_x.setImageAlpha(OPACITY_USED);
                image_y.setImageAlpha(OPACITY_UNUSED);
                break;
            case DRIVE:
                // Now in drive mode
                text_gamepadR1.setVisibility(View.VISIBLE);
                image_R1.setImageAlpha(OPACITY_USED);

                text_gamepadModeSelected0.setVisibility(View.VISIBLE);
                text_gamepadModeSelected1.setVisibility(View.INVISIBLE);
                text_gamepadMode0.setTypeface(text_gamepadMode0.getTypeface(), Typeface.BOLD);
                text_gamepadMode1.setTypeface(text_gamepadMode1.getTypeface(), Typeface.NORMAL);

                text_gamepadStickLeft.setText(R.string.gamepad_joystick_move);
                text_gamepadStickRight.setText("");
                stick_L.setImageAlpha(OPACITY_USED);
                stick_R.setImageAlpha(OPACITY_UNUSED);

                text_gamepadB.setVisibility(View.VISIBLE);
                text_gamepadB.setText(R.string.gamepad_moveFast);
                text_gamepadX.setVisibility(View.VISIBLE);
                text_gamepadX.setText(R.string.gamepad_wake);
                text_gamepadY.setVisibility(View.VISIBLE);
                text_gamepadY.setText(R.string.gamepad_moveSlow);
                image_b.setImageAlpha(OPACITY_USED);
                image_x.setImageAlpha(OPACITY_USED);
                image_y.setImageAlpha(OPACITY_USED);
                break;
            case LED_CONFIGURATION:
                // Now in LED config mode
                text_gamepadR1.setVisibility(View.INVISIBLE);
                image_R1.setImageAlpha(OPACITY_UNUSED);

                text_gamepadModeSelected0.setVisibility(View.INVISIBLE);
                text_gamepadModeSelected1.setVisibility(View.VISIBLE);
                text_gamepadMode0.setTypeface(text_gamepadMode0.getTypeface(), Typeface.NORMAL);
                text_gamepadMode1.setTypeface(text_gamepadMode1.getTypeface(), Typeface.BOLD);

                text_gamepadStickLeft.setText(R.string.gamepad_joystick_hue);
                text_gamepadStickRight.setText(R.string.gamepad_joystick_brightness);
                stick_L.setImageAlpha(OPACITY_USED);
                stick_R.setImageAlpha(OPACITY_USED);

                text_gamepadB.setVisibility(View.INVISIBLE);
                text_gamepadB.setText("");
                text_gamepadX.setVisibility(View.INVISIBLE);
                text_gamepadX.setText("");
                text_gamepadY.setVisibility(View.INVISIBLE);
                text_gamepadY.setText("");
                image_b.setImageAlpha(OPACITY_UNUSED);
                image_x.setImageAlpha(OPACITY_UNUSED);
                image_y.setImageAlpha(OPACITY_UNUSED);
                break;
        }
    }

    public boolean fragmentOnGenericMotionEvent(MotionEvent event) {
        float xaxis_L = event.getAxisValue(MotionEvent.AXIS_X);
        float yaxis_L = event.getAxisValue(MotionEvent.AXIS_Y);
        float xaxis_R = event.getAxisValue(MotionEvent.AXIS_Z);
        float yaxis_R = event.getAxisValue(MotionEvent.AXIS_RZ);

        // Display input in UI
        ConstraintLayout.LayoutParams params_L = (ConstraintLayout.LayoutParams) stick_L.getLayoutParams();
        ConstraintLayout.LayoutParams params_R = (ConstraintLayout.LayoutParams) stick_R.getLayoutParams();
        ConstraintLayout.LayoutParams paramsEmpty_L = (ConstraintLayout.LayoutParams) stickEmpty_L.getLayoutParams();
        ConstraintLayout.LayoutParams paramsEmpty_R = (ConstraintLayout.LayoutParams) stickEmpty_R.getLayoutParams();

        params_L.rightMargin = (int)(paramsEmpty_L.rightMargin - PIXELS_THUMBSTICK * xaxis_L);
        params_L.leftMargin = (int)(paramsEmpty_L.leftMargin + PIXELS_THUMBSTICK * xaxis_L);
        params_L.bottomMargin = (int)(paramsEmpty_L.bottomMargin - PIXELS_THUMBSTICK * yaxis_L);
        params_L.topMargin = (int)(paramsEmpty_L.topMargin + PIXELS_THUMBSTICK * yaxis_L);
        params_R.rightMargin = (int)(paramsEmpty_R.rightMargin - PIXELS_THUMBSTICK * xaxis_R);
        params_R.leftMargin = (int)(paramsEmpty_R.leftMargin + PIXELS_THUMBSTICK * xaxis_R);
        params_R.bottomMargin = (int)(paramsEmpty_R.bottomMargin - PIXELS_THUMBSTICK * yaxis_R);
        params_R.topMargin = (int)(paramsEmpty_R.topMargin + PIXELS_THUMBSTICK * yaxis_R);
        stick_L.setLayoutParams(params_L);
        stick_R.setLayoutParams(params_R);

        switch(mode) {
            case DRIVE:
                // Send stop command if joystick is near the center
                if(xaxis_L > -0.1 && xaxis_L < 0.1 && yaxis_L > -0.1 && yaxis_L < 0.1) {
                    if(!joystickDeadbanded) {
                        viewModel.postRoll(0, 0);
                        joystickDeadbanded = true;
                    }
                } else {
                    // Move normally
                    joystickDeadbanded = false;
                    // Bind the input within the range
                    // TODO: determine if this matters

                    viewModel.postRoll(xaxis_L, yaxis_L);
                }
                break;
            case LED_CONFIGURATION:
                // Change color when joystick is outside center
                if(xaxis_L < -0.2 || xaxis_L > 0.2 || yaxis_L < -0.2 || yaxis_L > 0.2) {
                    // Convert axes to 360 degree angle to hue
                    double radian = Math.atan2(yaxis_L, xaxis_L);
                    int hue = (int)(radian / (Math.PI * 2) * 100);
//                    float[] hsv = {hue, 1, 1};
//                    int argb = Color.HSVToColor(hsv);
                    viewModel.setLedColor(hue);
                }
                if(xaxis_R < -0.2 || xaxis_R > 0.2 || yaxis_R < -0.2 || yaxis_R > 0.2) {
                    // Convert axes to 360 degree angle to brightness level
                    double radian = Math.atan2(yaxis_L, xaxis_L);
                    int brightness = (int)(radian / (Math.PI * 2) * 100);
                    viewModel.setLedBrightness(brightness);
                }
                break;
        }


        return true;
    }

    public boolean fragmentOnKeyEvent(KeyEvent event) {
        boolean handled = true;

        if((event.getSource() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
            boolean pressed = event.getAction() == KeyEvent.ACTION_DOWN;
            int keyCode = event.getKeyCode();
            // A/B and X/Y are switched on the nintendo controllers. Do this flip here so that
            // everything else matches with what visually appears on the controller.
            switch(keyCode) {
                case KeyEvent.KEYCODE_BUTTON_A:
                    keyCode = KeyEvent.KEYCODE_BUTTON_B;
                    break;
                case KeyEvent.KEYCODE_BUTTON_B:
                    keyCode = KeyEvent.KEYCODE_BUTTON_A;
                    break;
                case KeyEvent.KEYCODE_BUTTON_X:
                    keyCode = KeyEvent.KEYCODE_BUTTON_Y;
                    break;
                case KeyEvent.KEYCODE_BUTTON_Y:
                    keyCode = KeyEvent.KEYCODE_BUTTON_X;
                    break;
            }
            // Now, actually process the button press:
            switch (keyCode) {
                case KeyEvent.KEYCODE_BUTTON_A:
                    setKeyImageResource(pressed, image_a, R.drawable.switch_a, R.drawable.switch_a_light);
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_B:
                    setKeyImageResource(pressed, image_b, R.drawable.switch_b, R.drawable.switch_b_light);
                    buttonPressedB = pressed;
                    if(mode != GamepadMode.DRIVE) {
                        handled = true;
                        break;
                    }
                    // Set ball speed
                    if(pressed) {
                        viewModel.setSpeed(SPEED_FAST);
                    } else {
                        if(buttonPressedY) {
                            viewModel.setSpeed(SPEED_SLOW);
                        } else {
                            viewModel.setSpeed(SPEED_NORMAL);
                        }
                    }
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_X:
                    setKeyImageResource(pressed, image_x, R.drawable.switch_x, R.drawable.switch_x_light);
                    if(mode != GamepadMode.DRIVE) {
                        handled = true;
                        break;
                    }
                    viewModel.setAwake(!viewModel.getAwake().getValue());
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_Y:
                    setKeyImageResource(pressed, image_y, R.drawable.switch_y, R.drawable.switch_y_light);
                    buttonPressedY = pressed;
                    if(mode != GamepadMode.DRIVE) {
                        handled = true;
                        break;
                    }
                    // Set ball speed
                    if(pressed) {
                        viewModel.setSpeed(SPEED_SLOW);
                    } else {
                        if(buttonPressedB) {
                            viewModel.setSpeed(SPEED_FAST);
                        } else {
                            viewModel.setSpeed(SPEED_NORMAL);
                        }
                    }
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_L1:
                    setKeyImageResource(pressed, image_L1, R.drawable.switch_lb, R.drawable.switch_lb_light);
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_R1:
                    setKeyImageResource(pressed, image_R1, R.drawable.switch_rb, R.drawable.switch_rb_light);
                    if(mode != GamepadMode.DRIVE) {
                        handled = true;
                        break;
                    }
                    viewModel.setResettingHeading(pressed);
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_SELECT:
                    setKeyImageResource(pressed, image_minus, R.drawable.switch_minus, R.drawable.switch_minus_light);
                    if(pressed) {
                        // Check old mode, change it, and process the new mode
                        switch(mode) {
                            case DRIVE:
                                setGamepadMode(GamepadMode.LED_CONFIGURATION);
                                break;
                            case LED_CONFIGURATION:
                                setGamepadMode(GamepadMode.DRIVE);
                                break;
                        }
                    }
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_START:
                    setKeyImageResource(pressed, image_plus, R.drawable.switch_plus, R.drawable.switch_plus_light);

                    switch(viewModel.getConnectionState().getValue()) {
                        case DISCONNECTED:
                            viewModel.setConnectionState(SpheroController.ConnectionState.CONNECTING);
                            break;
                        case CONNECTING:
                            // Do nothing
                            break;
                        case CONNECTED:
                            viewModel.setConnectionState(SpheroController.ConnectionState.DISCONNECTING);
                            break;
                        case DISCONNECTING:
                            // Do nothing
                            break;
                    }

                    handled = true;
                    break;
            }
        }

        return handled;
    }

    // Sets the ImageView for a particular key to its "pressed" or "unpressed" image resource.
    private void setKeyImageResource(boolean pressed, ImageView imageView, int image_unpressed, int image_pressed) {
        if(pressed)
            imageView.setImageResource(image_pressed);
        else
            imageView.setImageResource(image_unpressed);
    }
}