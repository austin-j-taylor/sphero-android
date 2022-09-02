package com.example.spheroandroid;

import androidx.annotation.DisplayContext;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import static com.example.spheroandroid.SpheroController.ConnectionState;

import android.util.Log;

// View model used for storing data used by SpheroMiniActivity and the control fragments.
// This allows us to decouple the control fragments from the SpheroMiniActivity that communicates with the
// SpheroController.
public class SpheroMiniViewModel extends ViewModel {

    public final static int SPEED_DEFAULT = 100;

    private final MutableLiveData<ConnectionState> connectionState = new MutableLiveData<>(ConnectionState.DISCONNECTED);
    private final MutableLiveData<Integer> speed = new MutableLiveData<>(SPEED_DEFAULT); // 0-100%
    private final MutableLiveData<Integer> ledBrightness = new MutableLiveData<>(100); // 0-100%
    private final MutableLiveData<Integer> ledColor = new MutableLiveData<>(100); // 0-100% hue
    private final MutableLiveData<Boolean> awake = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> resettingHeading = new MutableLiveData<>(false);
    // X and Y values in range [-1, +1] sent by the control fragments.
    // Processed by SpheroMiniActivity to send a roll command to the sphero.
    private final MutableLiveData<Double> rollX = new MutableLiveData<>(0d);
    private final MutableLiveData<Double> rollY = new MutableLiveData<>(0d);

    public void setConnectionState(ConnectionState state) {
        connectionState.setValue(state);
    }
    public void setSpeed(int speed) {
        this.speed.setValue(speed);
    }
    public void setLedBrightness(int ledBrightness) {
        this.ledBrightness.setValue(ledBrightness);
    }
    public void setLedColor(int ledColor) {
        this.ledColor.setValue(ledColor);
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
    @NonNull
    public LiveData<ConnectionState> getConnectionState() {
        return connectionState;
    }
    @NonNull
    public LiveData<Integer> getSpeed() {
        return speed;
    }
    @NonNull
    public LiveData<Integer> getLedBrightness() {
        return ledBrightness;
    }
    @NonNull
    public LiveData<Integer> getLedColor() {
        return ledColor;
    }
    @NonNull
    public LiveData<Boolean> getAwake() {
        return awake;
    }
    @NonNull
    public LiveData<Boolean> getResettingHeading() {
        return resettingHeading;
    }
    @NonNull
    public LiveData<Double> getRollX() {
        return rollX;
    }
    @NonNull
    public LiveData<Double> getRollY() {
        return rollY;
    }
}
