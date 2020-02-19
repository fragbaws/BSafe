package com.example.cda.ui.home;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.cda.MainActivity;
import com.example.cda.R;
import com.example.cda.entry.User;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static android.content.Context.SENSOR_SERVICE;
import static android.os.Environment.getExternalStorageDirectory;

public class HomeFragment extends Fragment implements OnMapReadyCallback {
    
    private static final String TAG = "HOME";

    private static final double MS2KMH = 3.6;
    private static final int CLOSE_ZOOM = 13;
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

    private SupportMapFragment mapFragment;
    private GoogleMap googleMap;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Marker marker;
    private Location oldLocation;
    private Location newLocation;
    private int speed;
    private double gforce, rotation,db;
    private boolean crash = false;

    private SensorManager manager;
    private float  timestamp_gyro;

    private ArrayList<Integer> speedValues;
    private double speedDeviation = 0; //speed standard deviation
    private Handler collectSpeedDataThreadHandler;
    private Runnable collectSpeedDataThread = new Runnable() {
        public void run()
        {
            Log.v(TAG, "Current speed: " + speed);
            speedValues.add(speed);
            ssdWriter.writeNext(new String[]{"-", "-", String.valueOf((System.currentTimeMillis()/1000)), speed + "km/h"});
            collectSpeedDataThreadHandler.postDelayed(this, 1000); // set same as location request interval
        }
    };
    private Handler calculateSpeedDeviationThreadHandler;
    private Runnable calculateSpeedDeviationThread = new Runnable() {
        public void run()
        {
            speedDeviation = calculateSpeedDeviation(speedValues);
            if(ssdWriter != null){
                ssdWriter.writeNext(new String[]{String.valueOf(speedDeviation), DateFormat.getDateTimeInstance().format(new Date()), String.valueOf((System.currentTimeMillis()/1000))});
                speedValues.clear();
            }
            calculateSpeedDeviationThreadHandler.postDelayed(this, 15000); // calculate speed deviation every 15 seconds
        }
    };

    private MediaRecorder recorder;
    private String audioName;

    private Button monitorButton;
    private TextView speedTxt;
    private TextView statusTxt;
    private CSVWriter sensorWriter;
    private CSVWriter ssdWriter;

    private String phoneNo, message;

    private boolean running = false;

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
        if (locationResult.getLocations().size() > 0) {
                oldLocation = newLocation;
                newLocation = locationResult.getLastLocation();
                Log.v(TAG, newLocation.toString());

                if (marker != null) {
                    marker.remove();
                }
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(new LatLng(newLocation.getLatitude(), newLocation.getLongitude()));
                markerOptions.flat(true);
                markerOptions.anchor(0.5f, 0.5f);
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.car_top_view));
                float bearing;
                if (newLocation.hasBearing()) {
                    bearing = newLocation.getBearing();
                } else {
                    bearing = 0;
                }
                markerOptions.rotation(bearing);
                marker = googleMap.addMarker(markerOptions);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(newLocation.getLatitude(), newLocation.getLongitude()), CLOSE_ZOOM));

                if(oldLocation != null) {
                    //oldSpeed = speed;
                    Log.v(TAG, "Time: " + oldLocation.getTime() + " " + newLocation.getTime());
                    Log.v(TAG, "Distance: " + oldLocation.distanceTo(newLocation));
                    speed = calcSpeed(oldLocation, newLocation);

                }
                if(running){
                    if(speed <= 24) {
                        speedTxt.setText("<24 km/h");
                    }else{
                        speedTxt.setText(String.format("%s km/h", speed));
                    }
                }
                Log.v(TAG, "Current speed is " + speed +" km/h");
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
                Log.v(TAG, "G-Force: " + max_g);
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

                    Log.v(TAG, "Rotation: " + degrees);

                }
                timestamp_gyro = event.timestamp;
            }
            if(sensorWriter!= null){
                String triggeredEvent = "";
                if(((gforce/G_FORCE_THRESHOLD) + (db/SOUND_PRESSURE_LEVEL_THRESHOLD)) >= ACCIDENT_THRESHOLD && (speed>=VEHICLE_SPEED_THRESHOLD)){ // travelling and hit
                    crash= true;
                    triggeredEvent = "1";
                    Log.v(TAG, "Crash occured: " + "("+ newLocation.getLatitude() + "," +newLocation.getLongitude() +")" +
                            "\n Speed: " + speed + "km/h" +
                            "\n Rotation: " + rotation + "°" +
                            "\n dB: " + db + "dB");
                }else if(((gforce/G_FORCE_THRESHOLD) + (db/SOUND_PRESSURE_LEVEL_THRESHOLD)) >= ACCIDENT_THRESHOLD && (rotation>=ROTATION_THRESHOLD)) { // hit and vehicle overturned (rotation utilises pitch & roll)
                    crash = true;
                    triggeredEvent = "2";
                    Log.v(TAG, "Crash occured: " + "("+ newLocation.getLatitude() + "," +newLocation.getLongitude() +")" +
                            "\n Speed: " + speed + "km/h" +
                            "\n Rotation: " + rotation + "°" +
                            "\n dB: " + db + "dB");
                }else if( (speed < VEHICLE_SPEED_THRESHOLD) && (((gforce/G_FORCE_THRESHOLD) + (db/SOUND_PRESSURE_LEVEL_THRESHOLD) + (speedDeviation/STANDARD_DEVIATION_THRESHOLD)) >= LOW_SPEED_ACCIDENT_THRESHOLD)){ // hit while slowly moving
                    crash = true;
                    triggeredEvent = "3";
                    Log.v(TAG, "Crash occured: " + "("+ newLocation.getLatitude() + "," +newLocation.getLongitude() +")" +
                            "\n Speed: " + speed + "km/h" +
                            "\n Rotation: " + rotation + "°" +
                            "\n dB: " + db + "dB");
                }else{
                    crash = false;
                }
                sensorWriter.writeNext(new String[]{String.valueOf(rotation), String.valueOf(gforce), String.valueOf(speed), String.valueOf(db), String.valueOf(crash), triggeredEvent, DateFormat.getDateTimeInstance().format(new Date()), String.valueOf(System.currentTimeMillis()/1000)});
                if(crash){
                    SOS();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private HomeViewModel homeViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);



        init(root);

        return root;
    }

    private void init(View root){
        speedTxt = root.findViewById(R.id.speed_text);
        statusTxt = root.findViewById(R.id.status_text);
        monitorButton = root.findViewById(R.id.monitor_button);
        monitorButton.setTag("ready");

        setupWriters();
        isLocationServiceEnabled();

        speedValues = new ArrayList<>();
        collectSpeedDataThreadHandler = new Handler();
        calculateSpeedDeviationThreadHandler = new Handler();

        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.v(TAG, "Could not load map fragment");
        }

        monitorButton.setOnClickListener(v -> {
            if(v.getId() == monitorButton.getId()){
                if(monitorButton.getTag().equals("ready")) {
                    Log.v(TAG, "Activating crash detection algorithm...");
                    running = true;

                    sensorWriter.writeNext(new String[]{"Rotation", "G-Force", "Speed", "dB", "Crash", "Case", "Timestamp", "Milliseconds"});
                    ssdWriter.writeNext(new String[]{"Standard Deviation", "Timestamp", "Milliseconds"});

                    manager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
                    manager.registerListener(sensorListener, manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_GAME);
                    manager.registerListener(sensorListener, manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME);
                    startRecording();

                    Log.v(TAG, "Activating threads for speed standard deviation calculations...");
                    collectSpeedDataThreadHandler.postDelayed(collectSpeedDataThread, 0);
                    calculateSpeedDeviationThreadHandler.postDelayed(calculateSpeedDeviationThread, 30000);

                    monitorButton.setText("Stop Monitoring");
                    statusTxt.setText("Normal");
                    speedTxt.setText("0 km/h");
                    monitorButton.setTag("running");
                }else{
                    stop();
                }
            }
        });
        speedTxt.setOnClickListener(v -> {
            if(newLocation!=null) {
                SOS();
            }
        });
    }

    private int calcSpeed(Location oldLocation, Location newLocation){
        float time = (newLocation.getTime() - oldLocation.getTime()) / 1000;
        float distance = newLocation.distanceTo(oldLocation);
        float speed = distance / time;
        if (speed < 1 || Double.isNaN(speed) || Double.isInfinite(speed)) {
            speed = 0;
        }
        return (int) (speed*MS2KMH);
    }

    private double calculateSpeedDeviation(ArrayList<Integer> speeds){
        double tmp = 0;
        for(Integer curr: speeds){
            tmp+=curr;
        }
        double mean = tmp/speeds.size();
        double sum = 0;
        for(Integer curr: speeds){
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
            Log.v(TAG, "Recording failed");
        }

        recorder.start();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                db = getDB();
                handler.postDelayed(this, 150); //20ms caused alot of -Infinity values (rubbish values)
            }
        }, 0);
    }

    private double getDB(){
        if(recorder != null){
            float maxAmplitude = recorder.getMaxAmplitude();
            float db = (float) (20 * Math.log10(maxAmplitude));// / 32767.0f));
            Log.v(TAG,"Current DB: " + db);
            if(db < 0 || Double.isInfinite(db) || Double.isNaN(db)){
                return 0.0;
            }else {
                return db;
            }
        }
        else{
            return 0.0;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(running){
            stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(running) {
            stop();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(running){
            stop();
        }
    }

    private void stop(){
        Log.v(TAG, "Deactivating crash detection algorithm...");
        running = false;
        speedTxt.setText("N/A");
        speedDeviation = 0;
        monitorButton.setText("Start Monitoring");
        monitorButton.setTag("ready");
        statusTxt.setText("Off");
        collectSpeedDataThreadHandler.removeCallbacks(collectSpeedDataThread);
        speedValues.clear();
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

    private void SOS(){
        stop();
        Log.v(TAG, "Crash detected");
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        MediaPlayer mp = MediaPlayer.create(getContext(), notification);
        mp.start();
        statusTxt.setText("Crash");

        AlertDialog dialog = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertStyle))
                .setTitle("Crash detected")
                .setMessage("The emergency services will be contacted")
                .setPositiveButton("SOS", (dialog1, which) -> sendAlert())
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            private static final int AUTO_DISMISS_MILLIS = 10000;
            @Override
            public void onShow(final DialogInterface dialog) {
                final Button defaultButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);
                final CharSequence negativeButtonText = defaultButton.getText();
                new CountDownTimer(AUTO_DISMISS_MILLIS, 100) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        defaultButton.setText(String.format(
                                Locale.getDefault(), "%s (%d)",
                                negativeButtonText,
                                TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) + 1 //add one so it never displays zero
                        ));
                    }
                    @Override
                    public void onFinish() {
                        if (((AlertDialog) dialog).isShowing()) {
                            Log.v(TAG, "User did not respond to alert, sending message to emergency contact.");
                            sendAlert();
                            mp.stop();
                            dialog.dismiss();
                        }
                    }
                }.start();
            }
        });
        dialog.show();
    }

    private void sendAlert(){
        User user = ((MainActivity)this.getActivity()).getUser();
        phoneNo = user.getEmergency();
        Log.v(TAG, "Building message for emergency contact: " + phoneNo);
        message = "This is an automated message from BSafe to alert that " + user.getFirstName() +" " + user.getSurname() +
                        " just experienced a vehicle crash. Please send an emergency response unit to (" + newLocation.getLatitude() +", " + newLocation.getLongitude()+") " +
                        "now and pass them these details about " + user.getFirstName() +": " + "\n" +
                        "Mobile number: " + user.getMobile() + "\n" +
                        "Date of birth: " + user.getDob() + "\n" +
                        "Height: " + user.getHeight() + " cm\n" +
                        "Weight: " + user.getWeight() + " kg\n" +
                        "Smoker: " + user.getSmoker() + "\n" +
                        "Bibulous: " + user.getBibulous() + "\n" +
                        "Medical condition: " + user.getMedicalCondition() + "\n" +
                        "Blood type: " + user.getBloodType() + "\n" +
                        "Last speed: " + speed + " km/h \n" +
                        "G-Force experienced: " + gforce + "\n\n" +
                        "You received this message because " + user.getFirstName() + " has listed you as an emergency contact in BSafe.";

            Log.v(TAG, "Sending message to " + phoneNo);
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message);

            smsManager.sendMultipartTextMessage(phoneNo, null, parts, null, null);
            Toast.makeText(getContext(), "Alert sent to emergency contact", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (googleMap != null) {
            this.googleMap = googleMap;
            this.googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getActivity(), R.raw.style_json));
            Log.v(TAG, "Initiating location request..");
            requestLocation();
        }
    }

    private void requestLocation() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        googleMap.setMyLocationEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.setIndoorEnabled(false);
        googleMap.setTrafficEnabled(true);
        googleMap.setBuildingsEnabled(false);
    }

    private void isLocationServiceEnabled(){
        Log.v(TAG, "Checking if location service is enabled...");
        LocationManager lm = (LocationManager)getContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ignored) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ignored) {}

        if(!gps_enabled && !network_enabled){
            Log.v(TAG, "Location services are disabled, opening location settings...");
            new AlertDialog.Builder(new ContextThemeWrapper(getContext(), R.style.AlertStyle))
                    .setMessage("The application will not work without location services.")
                    .setPositiveButton("Open Location Settings", (paramDialogInterface, paramInt) -> getContext().startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setCancelable(false)
                    .show();
        }
    }

    private void setupWriters(){
        audioName = getActivity().getExternalCacheDir().getAbsolutePath() + "/audiorecordtest.3gp";
        String baseDir = getExternalStorageDirectory().getAbsolutePath();
        String fileName = "crash_data.csv";
        String filePath = baseDir + File.separator + fileName;
        Log.v(TAG, "Writing to " + filePath);
        File f = new File(filePath);
        FileWriter mFileWriter;

        String ssdname = "ssd.csv";
        String ssdPath = baseDir + File.separator + ssdname;
        Log.v(TAG, "Writing to " + ssdPath);
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

            if (f.exists() && !f.isDirectory()) {
                mFileWriter = new FileWriter(filePath, true);
                sensorWriter = new CSVWriter(mFileWriter);
            } else {
                sensorWriter = new CSVWriter(new FileWriter(filePath));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}