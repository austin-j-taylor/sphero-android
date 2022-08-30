package com.example.spheroandroid;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class GamepadControlFragment extends Fragment {

    public static final String TAG = "GamepadControl";

    private static final float OPACITY_USED = 1;
    private static final float OPACITY_UNUSED = 0.25f;
    // How far the sticks should appear to move when titled
    private final static int PIXELS_THUMBSTICK = 75;

    private SpheroMiniViewModel viewModel;
    private ImageView stick_L;
    private ImageView stickEmpty_L;
    private ImageView stick_R;
    private ImageView stickEmpty_R;

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

        stick_L = getView().findViewById(R.id.image_stick_L);
        stickEmpty_L = getView().findViewById(R.id.image_stickEmpty_L);
        stick_R = getView().findViewById(R.id.image_stick_R);
        stickEmpty_R = getView().findViewById(R.id.image_stickEmpty_R);
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
}