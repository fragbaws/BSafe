package com.example.cda.ui.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.cda.R;
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
import com.google.android.gms.maps.model.MapStyleOptions;
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

import static android.content.Context.SENSOR_SERVICE;
import static android.os.Environment.getExternalStorageDirectory;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private static final DecimalFormat df = new DecimalFormat("#.###");
    private static final double MS2KMH = 3.6;
    private static final int CLOSE_ZOOM = 15;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private static final double RAD2D = 180.0 / Math.PI;
    private static final double GRAVITY_CONSTANT = 9.81;
    private static final int ACCIDENT_THRESHOLD = 1;
    private static final int LOW_SPEED_ACCIDENT_THRESHOLD = 3;
    private static final int G_FORCE_THRESHOLD = 4;
    private static final double STANDARD_DEVIATION_THRESHOLD = 2.06;
    private static final int SOUND_PRESSURE_LEVEL_THRESHOLD = 140;
    private static final int ROTATION_THRESHOLD = 45;
    private static final int VEHICLE_SPEED_THRESHOLD = 24;

    private static final int REQUEST_LOCATION_PERMISSIONS = 1;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 2;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 3;
    private static final String[] PERMISSIONS = new String[]{
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

    private ImageButton power_button;
    private TextView speed_text;
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
                speed_text.setText(String.format("%skm/h", df.format(((int) (speed * MS2KMH)))));
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
//                gTxt.setText(df.format(max_g) +"g");
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

                    //rotationTxt.setText(df.format(degrees) + "°");
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

    private HomeViewModel homeViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Log.v("Storage", "Writing permission already granted");
        } else {
            ActivityCompat.requestPermissions(getActivity(), PERMISSIONS, REQUEST_WRITE_EXTERNAL_STORAGE);
        }

        if(ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.RECORD_AUDIO)){
            Log.v("AUDIO", "Recording audio already permitted");
        }else{
            ActivityCompat.requestPermissions(getActivity(), PERMISSIONS, REQUEST_RECORD_AUDIO_PERMISSION);
        }

        audioName = getActivity().getExternalCacheDir().getAbsolutePath() + "/audiorecordtest.3gp";
        speed_text = root.findViewById(R.id.speed_text);
        power_button = root.findViewById(R.id.power_button);

        speedSSD = new ArrayList<>();
        handlerSSD = new Handler();
        power_button.setTag("ready");

        power_button.setOnClickListener(v -> {
            if(v.getId() == power_button.getId()){
                if(power_button.getTag().equals("ready")) {
                    SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
                    fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());
                    if (mapFragment != null) {
                        mapFragment.getMapAsync(this);
                    } else {
                        Log.v("Location", "Could not load map fragment");
                    }
                    manager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
                    manager.registerListener(sensorListener, manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_GAME);
                    manager.registerListener(sensorListener, manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME);
                    startRecording();
                    handlerSSD.postDelayed(threadSSD, 0);
                    power_button.setColorFilter(getResources().getColor(R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);
                    power_button.setTag("running");
                }else{
                    stop();
                    power_button.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
                }
            }
        });

        return root;
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
                //dbTxt.setText((int) db +"dB");

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
    }

    @Override
    public void onResume() {
        super.onResume();

        //rotationTxt.setText("");
        speed_text.setText("");
        //gTxt.setText("");
        //dbTxt.setText("");

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
    public void onPause() {
        super.onPause();
        stop();
    }

    private void stop(){
        speed_text.setText("");
        power_button.setTag("ready");
        handlerSSD.removeCallbacks(threadSSD);
        speedSSD.clear();
        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
        if(manager != null) {
            manager.unregisterListener(sensorListener);
        }
        if(recorder!=null) {
            recorder.stop();
            recorder.release();
            recorder = null;
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
        this.googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getActivity(), R.raw.style_json));
        requestLocation();
    }

    private void requestLocation() {
        locationRequest = new LocationRequest();
        Log.v("Location", "Requesting location");
        locationRequest.setFastestInterval(20);
        locationRequest.setSmallestDisplacement(5);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
            googleMap.setMyLocationEnabled(true);
            power_button.setTag("stop");
        } else {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions(getActivity(), PERMISSIONS, REQUEST_LOCATION_PERMISSIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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
                    Log.v("Storage", "Permission Granted, you can use local drive.");
                } else {
                    Log.v("Storage", "Permission Denied, You cannot use local drive.");
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