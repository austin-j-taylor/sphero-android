package com.example.spheroandroid;

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

public class GamepadControlFragment extends Fragment {

    public static final String TAG = "GamepadControl";

    private static final float OPACITY_USED = 1;
    private static final float OPACITY_UNUSED = 0.25f;
    // How far the sticks should appear to move when titled
    private final static int PIXELS_THUMBSTICK = 75;

    private boolean joystickDeadbanded;

    private SpheroMiniViewModel viewModel;
    private ImageView stick_L;
    private ImageView stickEmpty_L;
    private ImageView stick_R;
    private ImageView stickEmpty_R;
    private ImageView image_a, image_b, image_x, image_y, image_L1, image_R1, image_plus, image_minus;

    public GamepadControlFragment() {
        // Required empty public constructor
        joystickDeadbanded = false;
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


        view.setOnGenericMotionListener(new View.OnGenericMotionListener() {
            @Override
            public boolean onGenericMotion(View view, MotionEvent motionEvent) {
                return fragmentOnGenericMotionEvent(motionEvent);
            }
        });
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                return fragmentOnKeyEvent(keyEvent);
            }
        });
    }

    private void observeChangeConnectionState() {
        // TODO  Update the controls labels to reflect the connected/disconnect state
        // Hide most controls when disconnected
    }

    private void observeChangeSpeed() {
        // Do nothing
    }

    private void observeChangeLedBrightness() {
        // TODO  Update the controls labels to reflect the current brightness, or do nothing
    }

    private void observeChangeLedColor() {
        // TODO  Update the controls labels to reflect the current Color, or do nothing
    }

    private void observeChangeAwake() {
        // TODO  Update the controls labels to reflect the current awake status
        // Change Wake Up/Sleep text
    }

    private void changeGamepadMode() {
        // TODO  Update the gamepad mode - driving or changing LED settings
        // Change joystick/button opacity and labels
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


        return true;
    }

    public boolean fragmentOnKeyEvent(KeyEvent event) {
        boolean handled = true;


        if((event.getSource() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
            switch (event.getKeyCode()) {
                // A/B and X/Y are switched on the nintendo controllers
                case KeyEvent.KEYCODE_BUTTON_A: // actually B
                    setKeyImageResource(event, image_b, R.drawable.switch_b, R.drawable.switch_b_light);
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_B: // etc.
                    setKeyImageResource(event, image_a, R.drawable.switch_a, R.drawable.switch_a_light);
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_X:
                    setKeyImageResource(event, image_y, R.drawable.switch_y, R.drawable.switch_y_light);
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_Y:
                    setKeyImageResource(event, image_x, R.drawable.switch_x, R.drawable.switch_x_light);
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_L1:
                    setKeyImageResource(event, image_L1, R.drawable.switch_lb, R.drawable.switch_lb_light);
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_R1:
                    setKeyImageResource(event, image_R1, R.drawable.switch_rb, R.drawable.switch_rb_light);
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_SELECT:
                    setKeyImageResource(event, image_minus, R.drawable.switch_minus, R.drawable.switch_minus_light);
                    handled = true;
                    break;
                case KeyEvent.KEYCODE_BUTTON_START:
                    setKeyImageResource(event, image_plus, R.drawable.switch_plus, R.drawable.switch_plus_light);
                    handled = true;
                    break;
            }
        }

        return handled;
    }

    // Sets the ImageView for a particular key to its "pressed" or "unpressed" image resource.
    private void setKeyImageResource(KeyEvent event, ImageView imageView, int image_unpressed, int image_pressed) {
        if(event.getAction() == KeyEvent.ACTION_DOWN)
            imageView.setImageResource(image_pressed);
        else if(event.getAction() == KeyEvent.ACTION_UP)
            imageView.setImageResource(image_unpressed);
    }
}