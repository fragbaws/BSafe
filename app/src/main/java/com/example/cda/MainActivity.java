package com.example.cda;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

import static android.os.Environment.getExternalStorageDirectory;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final DecimalFormat df = new DecimalFormat("#.###");
    public static final double MS2KMH = 3.6;
    public static final int CLOSE_ZOOM = 15;
    public static final float NS2S = 1.0f / 1000000000.0f;
    public static final double RAD2D = 180.0 / Math.PI;
    public static final double GRAVITY_CONSTANT = 9.81;
    public static final int ACCIDENT_THRESHOLD = 1;
    public static final int LOW_SPEED_ACCIDENT_THRESHOLD = 3;
    public static final int G_FORCE_THRESHOLD = 4;
    public static final double STANDARD_DEVIATION_THRESHOLD = 2.06;
    public static final int SOUND_PRESSURE_LEVEL_THRESHOLD = 140;
    public static final int ROTATION_THRESHOLD = 45;
    public static final int VEHICLE_SPEED_THRESHOLD = 24;

    public static final int REQUEST_LOCATION_PERMISSIONS = 1;
    public static final int REQUEST_RECORD_AUDIO_PERMISSION = 2;
    public static final int REQUEST_WRITE_EXTERNAL_STORAGE = 3;
    public static final String[] PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private GoogleMap googleMap;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Marker marker;
    private Location oldLocation;
    private Location newLocation;
    private double speed, gforce, rotation,db;
    private boolean crash = false;

    private SensorManager manager;
    private float  timestamp_gyro;

    private ArrayList<Double> speedSSD;
    private double ssd = 0; //speed standard deviation
    private Handler handlerSSD;
    private Runnable threadSSD = new Runnable() {
        public void run()
        {
            if(speedSSD.size() == 1500){ // 30 seconds
                ssd = calcSSD(speedSSD);
                if(ssdWriter != null){
                    ssdWriter.writeNext(new String[]{String.valueOf(ssd), DateFormat.getDateTimeInstance().format(new Date())});
                }
                speedSSD.clear();
            }else {
                Log.v("SSD", "Current speed: " + speed);
                speedSSD.add(speed);
            }
            handlerSSD.postDelayed(this, 20); // set same as location request interval and other hardware sensors
        }
    };

    private MediaRecorder recorder;
    private String audioName;

    private Button btnActivate;
    private TextView rotationTxt, gTxt, speedTxt, dbTxt;
    private CSVWriter sensorWriter;
    private CSVWriter ssdWriter;

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            if (locationResult.getLocations().size() > 0) {
                oldLocation = newLocation;
                newLocation = locationResult.getLastLocation();

                if (marker != null) {
                    marker.remove();
                }
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(new LatLng(newLocation.getLatitude(), newLocation.getLongitude()));
                markerOptions.flat(true);
                float bearing;
                if (newLocation.hasBearing()) {
                    bearing = newLocation.getBearing();
                } else {
                    bearing = 0;
                }
                markerOptions.rotation(bearing);
                marker = googleMap.addMarker(markerOptions);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(newLocation.getLatitude(), newLocation.getLongitude()), CLOSE_ZOOM));

                if(oldLocation != null) {
                    //oldSpeed = speed;
                    Log.v("Location", "Time: " + oldLocation.getTime() + " " + newLocation.getTime());
                    Log.v("Location", "Distance: " + oldLocation.distanceTo(newLocation));
                    speed = calcSpeed(oldLocation, newLocation);
                }
                speedTxt.setText(String.format("%skm/h", df.format(((int) (speed * MS2KMH)))));
                Log.v("Location", "Current speed is " + ((int)speed*MS2KMH) +" km/h");
            }
        }
    };

    private SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
                double x = event.values[0];
                double y = event.values[1];
                double z = event.values[2];
                double max_g = Math.sqrt(x*x + y*y + z*z)/(GRAVITY_CONSTANT);
                gforce = max_g;
                gTxt.setText(df.format(max_g) +"g");
                Log.v("Accelerometer", "G-Force: " + max_g);
            }
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                if(timestamp_gyro!=0) {

                    float dT = (event.timestamp - timestamp_gyro) * NS2S;
                    float pitch = event.values[0];
                    float roll = event.values[1];
                    float yaw = event.values[2];
                    float omegaMag = (float) Math.sqrt(pitch*pitch + roll*roll);

                    // angular rotation in radians
                    float theta = omegaMag * dT;
                    float degrees = (float) (theta * RAD2D);
                    rotation = degrees;

                    rotationTxt.setText(df.format(degrees) + "°");
                    Log.v("Gyroscope", "Rotation: " + degrees);

                }
                timestamp_gyro = event.timestamp;
            }

            if(sensorWriter!= null){
                String triggeredEvent = "";
                if(((gforce/G_FORCE_THRESHOLD) + (db/SOUND_PRESSURE_LEVEL_THRESHOLD)) >= ACCIDENT_THRESHOLD && (speed>=VEHICLE_SPEED_THRESHOLD)){ // travelling and hit
                    crash= true;
                    triggeredEvent = "1";
                    Log.v("CDA", "Crash occured: " + "("+ newLocation.getLatitude() + "," +newLocation.getLongitude() +")" +
                            "\n Speed: " + speed + "km/h" +
                            "\n Rotation: " + rotation + "°" +
                            "\n dB: " + db + "dB");
                }else if(((gforce/G_FORCE_THRESHOLD) + (db/SOUND_PRESSURE_LEVEL_THRESHOLD)) >= ACCIDENT_THRESHOLD && (rotation>=ROTATION_THRESHOLD)) { // hit and vehicle overturned (rotation utilises pitch & roll)
                    crash = true;
                    triggeredEvent = "2";
                    Log.v("CDA", "Crash occured: " + "("+ newLocation.getLatitude() + "," +newLocation.getLongitude() +")" +
                            "\n Speed: " + speed + "km/h" +
                            "\n Rotation: " + rotation + "°" +
                            "\n dB: " + db + "dB");
                }else if((((gforce/G_FORCE_THRESHOLD) + (db/SOUND_PRESSURE_LEVEL_THRESHOLD) + (ssd/STANDARD_DEVIATION_THRESHOLD)) >= LOW_SPEED_ACCIDENT_THRESHOLD)){ // hit while slowly moving
                    crash = true;
                    triggeredEvent = "3";
                    Log.v("CDA", "Crash occured: " + "("+ newLocation.getLatitude() + "," +newLocation.getLongitude() +")" +
                            "\n Speed: " + speed + "km/h" +
                            "\n Rotation: " + rotation + "°" +
                            "\n dB: " + db + "dB");
                }else{
                    crash = false;
                }
                sensorWriter.writeNext(new String[]{String.valueOf(rotation), String.valueOf(gforce), String.valueOf(((int)speed*MS2KMH)), String.valueOf(db), String.valueOf(crash), triggeredEvent, DateFormat.getDateTimeInstance().format(new Date())});
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Log.v("Storage", "Writing permission already granted");
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, REQUEST_WRITE_EXTERNAL_STORAGE);
        }

        if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.RECORD_AUDIO)){
            Log.v("AUDIO", "Recording audio already permitted");
        }else{
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, REQUEST_RECORD_AUDIO_PERMISSION);
        }

        btnActivate = findViewById(R.id.activateBtn);
        rotationTxt = findViewById(R.id.rotationTxt);
        gTxt = findViewById(R.id.gTxt);
        speedTxt = findViewById(R.id.speedTxt);
        dbTxt = findViewById(R.id.dbTxt);
        audioName = getExternalCacheDir().getAbsolutePath() + "/audiorecordtest.3gp";

        speedSSD = new ArrayList<>();
        handlerSSD = new Handler();

        btnActivate.setOnClickListener(v -> {
            if(v.getId() == btnActivate.getId()){
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.cdaMap);
                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
                if (mapFragment != null) {
                    mapFragment.getMapAsync(this);
                }else{
                    Log.v("Location", "Could not load map fragment");
                }
                manager = (SensorManager) getSystemService(SENSOR_SERVICE);
                manager.registerListener(sensorListener, manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_GAME);
                manager.registerListener(sensorListener, manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME);
                startRecording();
                handlerSSD.postDelayed(threadSSD, 0);
            }
        });

    }

    private float calcSpeed(Location oldLocation, Location newLocation){
        float time = (newLocation.getTime() - oldLocation.getTime()) / 1000;
        float distance = newLocation.distanceTo(oldLocation);
        float speed = distance / time;
        if (speed < 1) {
            speed = 0;
        }
        return speed;
    }

    private double calcSSD(ArrayList<Double> speeds){
        double tmp = 0;
        for(Double curr: speeds){
            tmp+=curr;
        }
        double mean = tmp/speeds.size();
        double sum = 0;
        for(Double curr: speeds){
            sum += Math.pow((curr - mean), 2);
        }
        return Math.sqrt(sum/speeds.size());
    }

    private void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        recorder.setOutputFile(audioName);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.v("Microphone", "prepare() failed");
        }

        recorder.start();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                db = getDB();
                dbTxt.setText((int) db +"dB");

                handler.postDelayed(this, 20);
            }
        }, 20);
    }

    private double getDB(){
        if(recorder != null){
            float maxAmplitude = recorder.getMaxAmplitude();
            Log.v("Microphone","Amplitude: " + maxAmplitude);
            float db = (float) (20 * Math.log10(maxAmplitude));// / 32767.0f));
            Log.v("Microphone","Current DB: " + db);
            if(db < 0){
                return 0.0;
            }
            return db;
        }
        else{
            return 0.0;
        }
    }

    private void stopRecording() {
        if(recorder!=null) {
            recorder.stop();
            recorder.release();
            recorder = null;
        }
    }

    @Override
    public void onBackPressed(){
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            Log.i("MainActivity", "popping backstack");
            fm.popBackStack();
        } else {
            Log.i("MainActivity", "nothing on backstack, calling super");
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handlerSSD.removeCallbacks(threadSSD);
        speedSSD.clear();
        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
        if(manager != null) {
            manager.unregisterListener(sensorListener);
        }
        if(recorder!=null) {
            stopRecording();
        }
        try {
            if(sensorWriter != null) {
                sensorWriter.close();
            }
            if(ssdWriter != null){
                ssdWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        rotationTxt.setText("");
        speedTxt.setText("");
        gTxt.setText("");
        dbTxt.setText("");

        String baseDir = getExternalStorageDirectory().getAbsolutePath();
        String fileName = "crash_data.csv";
        String filePath = baseDir + File.separator + fileName;
        Log.v("Storage", "Writing to " + filePath);
        File f = new File(filePath);
        FileWriter mFileWriter;

        String ssdname = "ssd.csv";
        String ssdPath = baseDir + File.separator + ssdname;
        Log.v("Storage", "Writing to " + ssdPath);
        File fssd = new File(ssdPath);
        FileWriter ssdFileWriter;

        // File exist
        try {
            if(fssd.exists() && !fssd.isDirectory()){
                ssdFileWriter = new FileWriter(ssdPath, true);
                ssdWriter = new CSVWriter(ssdFileWriter);
            }else{
                ssdWriter = new CSVWriter(new FileWriter(ssdPath));
            }
            ssdWriter.writeNext(new String[]{"Standard Deviation", "Timestamp"});

            if (f.exists() && !f.isDirectory()) {
                mFileWriter = new FileWriter(filePath, true);
                sensorWriter = new CSVWriter(mFileWriter);
            } else {
                sensorWriter = new CSVWriter(new FileWriter(filePath));
            }
            sensorWriter.writeNext(new String[]{"Rotation", "G-Force", "Speed", "dB", "Crash", "Case", "Timestamp"});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handlerSSD.removeCallbacks(threadSSD);
        speedSSD.clear();
        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
        if(manager != null) {
            manager.unregisterListener(sensorListener);
        }
        if(recorder!=null) {
            stopRecording();
        }
        try {
            if(sensorWriter != null) {
                sensorWriter.close();
            }
            if(ssdWriter != null){
                ssdWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        requestLocation();
    }

    private void requestLocation() {
        locationRequest = new LocationRequest();
        Log.v("Location", "Requesting location");
        locationRequest.setFastestInterval(20);
        locationRequest.setSmallestDisplacement(5);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
            googleMap.setMyLocationEnabled(true);
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_LOCATION_PERMISSIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        Log.v("Location", "Permission granted");
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
                        googleMap.setMyLocationEnabled(true);
                    }
                } else {
                    Log.v("Location", "Permission denied");
                }
            }
            case REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v("Storage", "Permission Granted, you can use local drive .");
                } else {
                    Log.v("Storage", "Permission Denied, You cannot use local drive .");
                }
            }
            case REQUEST_RECORD_AUDIO_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v("AUDIO", "Recording audio permitted");
                } else {
                    Log.v("AUDIO", "Recording audio denied");
                }
            }
        }
    }
}
