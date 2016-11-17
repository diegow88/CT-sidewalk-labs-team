package com.bikenyc.productstudio;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class RecordActivity extends AppCompatActivity implements SensorEventListener {
    private boolean mRecording;

    private SensorManager mSensorManager;
    private Sensor mSensorLinearAccelerometer;
    private Sensor mSensorGyroscope;
    private LocationManager mLocationManager;

    private TrackingStorageManager mStorageManager;

    private Button mStartStopButton;
    private Button mTrackButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        // Get instance of the Sensor Manager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        mStorageManager = new TrackingStorageManager();

        // Request access to the Linear Accelerometer
        mSensorLinearAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (mSensorLinearAccelerometer == null)
            System.out.println("Linear Accelerometer sensor not found");

        // Request access to the Linear Gyroscope
        mSensorGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (mSensorGyroscope == null)
            System.out.println("Gyroscope sensor not found");

        // Request access to location manager

        this.mStartStopButton = (Button) findViewById(R.id.start_stop);
        this.mTrackButton = (Button) findViewById(R.id.track_double_parking);

        this.mRecording = false;
    }

    @Override
    protected void onResume() {
        super.onResume();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        mSensorManager.unregisterListener(this);
    }

    public void handleStartStopButton(View view) {
        if (this.mRecording) {
            this.mStartStopButton.setText(R.string.start);
            this.stopRecording();
        } else {
            this.mStartStopButton.setText(R.string.stop);
            this.startRecording();
        }
        this.mRecording = !this.mRecording;

    }

    public void startRecording() {
        this.mSensorManager.registerListener(this, mSensorGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        //this.mSensorManager.registerListener(this, mSensorLinearAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stopRecording() {
        String filename = this.createCSVFile();
        if (this.uploadFile()) {
            this.getApplicationContext().deleteFile(filename);
        }
    }

    /**
     * This method is inteded to store the data locally in case worse case scenario that the
     * user doesn't have an Internet connection when they stop and can't upload it to the servers.
     *
     * @return String filename of the recently created file
     */
    public String createCSVFile() {
        FileOutputStream outputStream;
        String filename = "output.csv";
        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(mStorageManager.generateOutput().getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return filename;
    }

    /**
     * This method will attempt to upload the stored data into the cloud and return a flag
     * stating if the upload was successful.
     *
     * @return boolean Whether if it was a succesful upload or not
     */
    public boolean uploadFile() {
        return false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_LINEAR_ACCELERATION:
                this.mStorageManager.handleLinearAccelerometerEvent(event);
                break;

            case Sensor.TYPE_GYROSCOPE:
                this.mStorageManager.handleGyroscopeEvent(event);
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}