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
    private final MutableLiveData<Integer> ledColor = new MutableLiveData<>();
    private final MutableLiveData<Boolean> awake = new MutableLiveData<>();
    private final MutableLiveData<Boolean> resettingHeading = new MutableLiveData<>();
    // X and Y values in range [-1, +1] sent by the control fragments.
    // Processed by SpheroMiniActivity to send a roll command to the sphero.
    private final MutableLiveData<Double> rollX = new MutableLiveData<>();
    private final MutableLiveData<Double> rollY = new MutableLiveData<>();


    public void setConnectionState(ConnectionState state) {
        connectionState.setValue(state);
    }
    public void setSpeed(int speed) {
        this.speed.setValue(speed);
    }
    public void setLedBrightness(int ledBrightness) {
        this.ledBrightness.setValue(ledBrightness);
    }
    public void setLedColor(int argb) {
        this.ledColor.setValue(argb);
    }
    public void setAwake(boolean awake) {
        this.awake.setValue(awake);
    }
    public void setResettingHeading(boolean resettingHeading) {
        this.resettingHeading.setValue(resettingHeading);
    }
    public void postRoll(double rollX, double rollY) {
        this.rollX.postValue(rollX);
        this.rollY.postValue(rollY);
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
    public LiveData<Integer> getLedColor() {
        return ledColor;
    }
    public LiveData<Boolean> getAwake() {
        return awake;
    }
    public LiveData<Boolean> getResettingHeading() {
        return resettingHeading;
    }
    public LiveData<Double> getRollX() {
        return rollX;
    }
    public LiveData<Double> getRollY() {
        return rollY;
    }
}
