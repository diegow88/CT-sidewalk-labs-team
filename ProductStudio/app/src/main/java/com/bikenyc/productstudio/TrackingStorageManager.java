package com.bikenyc.productstudio;

import android.hardware.SensorEvent;

import java.util.ArrayList;

/**
 * Created by Diego on 11/15/2016.
 */

public class TrackingStorageManager {

    private ArrayList<float[]> mGyroscope;
    private ArrayList<float[]> mLinearAccelerometer;
    private float[] mLocation;

    public TrackingStorageManager() {
        mGyroscope = new ArrayList<float[]>();
    }

    public void handleGyroscopeEvent(SensorEvent event) {
        this.mGyroscope.add(event.values);
    }

    public void handleLinearAccelerometerEvent(SensorEvent event) {

    }

    public void handleLocationEvent(SensorEvent event) {

    }

    public String generateOutput() {
        StringBuilder output = new StringBuilder();
        for (float[] entry: mGyroscope) {
            output.append(entry);
        }

        return output.toString();
    }

}
