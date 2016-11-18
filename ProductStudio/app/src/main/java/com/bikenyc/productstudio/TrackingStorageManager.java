package com.bikenyc.productstudio;

import android.hardware.SensorEvent;
import android.location.Location;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Diego on 11/15/2016.
 */

public class TrackingStorageManager {

    private ArrayList<float[]> mGyroscope;
    private ArrayList<float[]> mLinearAccelerometer;
    private ArrayList<double[]> mLocation;

    private Location mCurrentBestLocation;

    public TrackingStorageManager() {
        mGyroscope = new ArrayList<float[]>();
        mLinearAccelerometer = new ArrayList<float[]>();
        mLocation = new ArrayList<double[]>();
    }

    public void handleGyroscopeEvent(SensorEvent event) {
        float[] values = new float[4];
        values[0] = event.timestamp;
        values[1] = event.values[0];
        values[2] = event.values[1];
        values[3] = event.values[2];
        this.addValuesToGyroscope(values);
    }

    public void handleLinearAccelerometerEvent(SensorEvent event) {
        float[] values = new float[4];
        values[0] = event.timestamp;
        values[1] = event.values[0];
        values[2] = event.values[1];
        values[3] = event.values[2];
        this.addValuesToLinearAccelerometer(values);
    }

    public void handleLocationEvent(Location location) {
        //if (!this.isBetterLocation(location))
        //    return;

        //this.mCurrentBestLocation = location;
        double[] values = new double[4];
        values[0] = location.getElapsedRealtimeNanos();
        values[1] = location.getLatitude();
        values[2] = location.getLongitude();
        values[3] = location.getAltitude();
        this.addValuesToLocation(values);
    }

    private void addValuesToGyroscope(float[] values) {
        this.mGyroscope.add(values);
    }
    private void addValuesToLinearAccelerometer(float[] values) {this.mLinearAccelerometer.add(values);}
    private void addValuesToLocation(double[] values) { this.mLocation.add(values); }

    public String generateOutput() {
        StringBuilder output = new StringBuilder();
        output.append("[Gyroscope output(timestamp, x, y, z)]");
        for (float[] entry: mGyroscope) {
            output.append(Arrays.toString(entry));
            output.append('\n');
            System.out.println(Arrays.toString(entry));
        }

        output.append("[Linear Accelerometer output(timestamp, x, y, z)]");
        for (float[] entry: mLinearAccelerometer) {
            output.append(Arrays.toString(entry));
            output.append('\n');
            System.out.println(Arrays.toString(entry));
        }

        output.append("[Location output(timestamp, Lat, Lng, Alt)]");
        for (double[] entry: mLocation) {
            output.append(Arrays.toString(entry));
            output.append('\n');
            System.out.println(Arrays.toString(entry));
        }

        return output.toString();
    }

    private static final int TWO_MINUTES = 1000 * 60 * 2;

    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     */
    protected boolean isBetterLocation(Location location) {
        if (mCurrentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - mCurrentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - mCurrentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                mCurrentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
}
