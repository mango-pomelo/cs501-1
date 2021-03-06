package com.example.accelerometer_measurements;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import java.text.DecimalFormat;
import android.content.Intent;
import android.media.MediaPlayer;

import android.widget.ListView;
import android.widget.ListAdapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import android.content.Context;
import androidx.annotation.RequiresApi;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;

public class MainActivity extends AppCompatActivity {

    private static int SIGNIFICANT_SHAKE = 1000;
    private static int easy_shake = 500;
    private static int medium_shake = 1000;
    private static int hard_shake = 1500;
    private CameraManager CamManager;
    private String CamID;
    private DecimalFormat df;
    MediaPlayer mp;

    private float lastX, lastY, lastZ;  //old coordinate positions from accelerometer, needed to calculate delta.
    private float acceleration;
    private float currentAcceleration;
    private float lastAcceleration;
    private int count;
    private String mode;

    Button btnStart;
    Button btnStop;
    TextView txtSteps;
    ListView lvWorkout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);
        txtSteps = (TextView) findViewById(R.id.txtSteps);

        lvWorkout = (ListView) findViewById(R.id.lvWorkout);
        final String[] modes = {"easy", "medium", "hard"};
        ArrayAdapter modeAdapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_list_item_1,
                modes);
        lvWorkout.setAdapter(modeAdapter);
        // ****** not sure put the string of selected mode into variable mode
        lvWorkout.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mode = String.valueOf(parent.getItemAtPosition(position));
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mode == "easy") {
                    easy(mode);
                } else if (mode == "medium") {
                    medium(mode);
                } else if (mode == "hard") {
                    hard(mode);
                }
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableAccelerometerListening();
                mp.stop();
            }
        });

        CamManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CamID = CamManager.getCameraIdList()[0];  //rear camera is at index 0
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        acceleration = 0.00f;                                         //Initializing Acceleration data.
        currentAcceleration = SensorManager.GRAVITY_EARTH;            //We live on Earth.
        lastAcceleration = SensorManager.GRAVITY_EARTH;               //Ctrl-Click to see where else we could use our phone.

    }
// ****** not sure how to repeatedly call sensor to increase the count
    private void easy(String mode) {
        SensorManager sensorManager =
                (SensorManager) this.getSystemService(
                        Context.SENSOR_SERVICE);
        count = 0;
        SIGNIFICANT_SHAKE = easy_shake;
        while (count<100) {
            sensorManager.registerListener(sensorEventListener,
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL);
            if (count==30) {
                mp = MediaPlayer.create(MainActivity.this, R.raw.superman);
                mp.start();
            }
        }
    }
    private void medium(String mode) {
        SensorManager sensorManager =
                (SensorManager) this.getSystemService(
                        Context.SENSOR_SERVICE);
        count = 0;
        SIGNIFICANT_SHAKE = medium_shake;
        while (count<100) {
            sensorManager.registerListener(sensorEventListener,
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL);
            if (count==50) {
                mp = MediaPlayer.create(MainActivity.this, R.raw.starwars);
                mp.start();
            }
        }
    }
    private void hard(String mode) {
        SensorManager sensorManager =
                (SensorManager) this.getSystemService(
                        Context.SENSOR_SERVICE);

        count = 0;
        SIGNIFICANT_SHAKE = hard_shake;
        while (count<100) {
            sensorManager.registerListener(sensorEventListener,
                    sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL);
            if (count>20) {
                LightOn();
                LightOff();
            }
            if (count==60) {
                mp = MediaPlayer.create(MainActivity.this, R.raw.rocky);
                mp.start();
            }
        }
    }

    private void disableAccelerometerListening() {

//Disabling Sensor Event Listener is two step process.
        //1. Retrieve SensorManager Reference from the activity.
        //2. call unregisterListener to stop listening for sensor events
        //THis will prevent interruptions of other Apps and save battery.

        // get the SensorManager
        SensorManager sensorManager =
                (SensorManager) this.getSystemService(
                        Context.SENSOR_SERVICE);

        // stop listening for accelerometer events
        sensorManager.unregisterListener(sensorEventListener,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
    }

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // get x, y, and z values for the SensorEvent
            //each time the event fires, we have access to three dimensions.
            //compares these values to previous values to determine how "fast"
            // the device was shaken.
            //Ref: http://developer.android.com/reference/android/hardware/SensorEvent.html

            float x = event.values[0];   //obtaining the latest sensor data.
            float y = event.values[1];   //sort of ugly, but this is how data is captured.
            float z = event.values[2];

            // save previous acceleration value
            lastAcceleration = currentAcceleration;

            // calculate the current acceleration
            currentAcceleration = x * x + y * y + z * z;   //This is a simplified calculation, to be real we would need time and a square root.

            // calculate the change in acceleration        //Also simplified, but good enough to determine random shaking.
            acceleration = currentAcceleration *  (currentAcceleration - lastAcceleration);

            // if the acceleration is above a certain threshold
            if (acceleration > SIGNIFICANT_SHAKE) {
                Log.e(TAG, "delta x = " + (x-lastX));
                Log.e(TAG, "delta y = " + (y-lastY));
                Log.e(TAG, "delta z = " + (z-lastZ));
                count++;
            }

            lastX = x;
            lastY = y;
            lastZ = z;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    public void LightOn() {
        try {
            CamManager.setTorchMode(CamID, true);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void LightOff()
    {
        try {
            CamManager.setTorchMode(CamID, false);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}