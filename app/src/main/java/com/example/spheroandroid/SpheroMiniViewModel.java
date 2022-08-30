package com.example.spheroandroid;

import androidx.annotation.DisplayContext;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

// View model used for storing data used by SpheroMiniActivity and the control fragments.
public class SpheroMiniViewModel extends ViewModel {


    public enum ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING }

    private final MutableLiveData<ConnectionState> connectionState = new MutableLiveData<>();
    private final MutableLiveData<Integer> speed = new MutableLiveData<>();
    private final MutableLiveData<Integer> ledBrightness = new MutableLiveData<>();
    private final MutableLiveData<Integer> ledHue = new MutableLiveData<>();
    private final MutableLiveData<Boolean> awake = new MutableLiveData<>();
    private final MutableLiveData<Boolean> resettingHeading = new MutableLiveData<>();

    public void setConnectionState(ConnectionState state) {
        connectionState.setValue(state);
    }
    public void setSpeed(int speed) {
        this.speed.setValue(speed);
    }
    public void setLedBrightness(int ledBrightness) {
        this.ledBrightness.setValue(ledBrightness);
    }
    public void setLedHue(int ledHue) {
        this.ledHue.setValue(ledHue);
    }
    public void setAwake(boolean awake) {
        this.awake.setValue(awake);
    }
    public void setResettingHeading(boolean resettingHeading) {
        this.resettingHeading.setValue(resettingHeading);
    }

    public LiveData<ConnectionState> getConnectionState() {
        return connectionState;
    }
    public LiveData<Integer> getSpeed() {
        return speed;
    }
    public LiveData<Integer> getLedBrightness() {
        return ledBrightness;
    }
    public LiveData<Integer> getLedHue() {
        return ledHue;
    }
    public LiveData<Boolean> getAwake() {
        return awake;
    }
    public LiveData<Boolean> getResettingHeading() {
        return resettingHeading;
    }
}
