package com.bikenyc.productstudio;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.RelativeLayout;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class RecordActivity extends AppCompatActivity implements SensorEventListener {
    private boolean mRecording;

    private SensorManager mSensorManager;
    private Sensor mSensorLinearAccelerometer;
    private Sensor mSensorGyroscope;

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;

    private TrackingStorageManager mStorageManager;

    private Button mStartStopButton;
    private Button mTrackButton;

    private Chronometer mChronometer;

    private HandlerThread mSensorThread;
    private Handler mSensorHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        // Get instance of the Sensor Manager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Get instance of the Location Manager
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        mStorageManager = new TrackingStorageManager();

        // Request access to the Linear Accelerometer
        mSensorLinearAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (mSensorLinearAccelerometer == null)
            System.out.println("Linear Accelerometer sensor not found");

        // Request access to the Linear Gyroscope
        mSensorGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (mSensorGyroscope == null)
            System.out.println("Gyroscope sensor not found");


        this.mStartStopButton = (Button) findViewById(R.id.start_stop);
        //this.mTrackButton = (Button) findViewById(R.id.track_double_parking);

        this.mChronometer = (Chronometer) findViewById(R.id.chronometer1);
        //this.mChronometer.setFormat("H:MM:SS");
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
        mSensorThread = new HandlerThread("Sensor thread", Thread.MAX_PRIORITY);
        mSensorThread.start();
        mSensorHandler = new Handler(mSensorThread.getLooper()); //Blocks until looper is prepared, which is fairly quick

        this.mSensorManager.registerListener(this, mSensorGyroscope, SensorManager.SENSOR_DELAY_NORMAL, mSensorHandler);
        this.mSensorManager.registerListener(this, mSensorLinearAccelerometer, SensorManager.SENSOR_DELAY_NORMAL, mSensorHandler);

        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                mStorageManager.handleLocationEvent(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission_group.LOCATION},100);

        }

        // Register the listener with the Location Manager to receive location updates
        if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener, mSensorThread.getLooper());
        else
            System.out.println("No Network provider");

        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener, mSensorThread.getLooper());
        else
            System.out.println("No GPS provider");

        ((RelativeLayout) findViewById(R.id.activity_record)).setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.red, null));
        this.mChronometer.setBase(SystemClock.elapsedRealtime());
        this.mChronometer.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == 100) {
            System.out.println("Finally we got permission!");
        }
    }

    public void stopRecording() {
        mSensorManager.unregisterListener(this);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }mLocationManager.removeUpdates(this.mLocationListener);

        File file = this.createCSVFile();
        if (file.canRead()) {
            this.uploadFile(file);
        }
        ((RelativeLayout) findViewById(R.id.activity_record)).setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.green, null));
        this.mChronometer.stop();
    }

    private void showUploadMessage() {
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
        dlgAlert.setMessage("The data has been uploaded!");
        dlgAlert.setTitle("Upload");
        dlgAlert.setPositiveButton("OK", null);
        dlgAlert.setCancelable(false);
        dlgAlert.create().show();
    }

    /**
     * This method is inteded to store the data locally in case worse case scenario that the
     * user doesn't have an Internet connection when they stop and can't upload it to the servers.
     *
     * @return String filename of the recently created file
     */
    public File createCSVFile() {
        FileOutputStream outputStream;
        String filename = "output.csv";
        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(mStorageManager.generateOutput().getBytes());
            outputStream.close();
            return getFileStreamPath(filename);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * This method will attempt to upload the stored data into the cloud and return a flag
     * stating if the upload was successful.
     * @param file It's the file reference to be used to upload.
     * @return
     */
    public void uploadFile(final File file) {
        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:8945e6af-0fa5-453b-84f7-f4e221bcf815", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );

        AmazonS3 s3 = new AmazonS3Client(credentialsProvider);

        TransferUtility transferUtility = new TransferUtility(s3, getApplicationContext());

        TransferObserver observer = transferUtility.upload(
                "bikenyc",     /* The bucket to upload to */
                "output.csv",    /* The key for the uploaded object */
                file        /* The file where the data to upload exists */
        );


        class UploadListener implements TransferListener {

            private static final String TAG = "UploadListener";

            @Override
            public void onStateChanged(int id, TransferState state) {
                Log.d(TAG, "onStateChanged: " + id + ", " + state);

                if (state == TransferState.IN_PROGRESS) {
                    System.out.println("Upload initialized!");
                    showUploadMessage();
                }

                if (state == TransferState.COMPLETED) {
                    System.out.println("Upload Completed!");
                    getApplicationContext().deleteFile(file.getName());
                    mStorageManager.reset();
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                Log.d(TAG, String.format("onProgressChanged: %d, total: %d, current: %d",
                        id, bytesTotal, bytesCurrent));
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.e(TAG, "Error during upload: " + id, ex);
            }
        }

        observer.setTransferListener(new UploadListener());

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