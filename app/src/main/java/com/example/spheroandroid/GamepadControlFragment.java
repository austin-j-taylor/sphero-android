package com.example.spheroandroid;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link GamepadControlFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class GamepadControlFragment extends Fragment {

    private static final float OPACITY_USED = 1;
    private static final float OPACITY_UNUSED = 0.25f;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private SpheroMiniViewModel viewModel;

    public GamepadControlFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment GamepadControlFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static GamepadControlFragment newInstance(String param1, String param2) {
        GamepadControlFragment fragment = new GamepadControlFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        viewModel = new ViewModelProvider(requireActivity()).get(SpheroMiniViewModel.class);

        viewModel.getConnectionState().observe(getViewLifecycleOwner(), connectionState -> observeChangeConnectionState());
        viewModel.getSpeed().observe(getViewLifecycleOwner(), speed -> observeChangeSpeed());
        viewModel.getLedBrightness().observe(getViewLifecycleOwner(), ledBrightness -> observeChangeLedBrightness());
        viewModel.getLedHue().observe(getViewLifecycleOwner(), ledHue -> observeChangeLedHue());
        viewModel.getAwake().observe(getViewLifecycleOwner(), awake -> observeChangeAwake());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_gamepad_control, container, false);
    }


    private void observeChangeConnectionState() {
        // Update the controls labels to reflect the connected/disconnect state
        // Hide most controls when disconnected
    }
    private void observeChangeSpeed() {
        // Do nothing
    }
    private void observeChangeLedBrightness() {
        // Update the controls labels to reflect the current brightness, or do nothing
    }
    private void observeChangeLedHue() {
        // Update the controls labels to reflect the current hue, or do nothing
    }
    private void observeChangeAwake() {
        // Update the controls labels to reflect the current awake status
        // Change Wake Up/Sleep text
    }
    private void changeGamepadMode() {
        // Update the gamepad mode - driving or changing LED settings
        // Change joystick/button opacity and labels
    }
}