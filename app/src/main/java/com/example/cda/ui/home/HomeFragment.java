package com.example.cda.ui.home;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.os.Build;
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
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.example.cda.MainActivity;
import com.example.cda.R;
import com.example.cda.data.PrimaryData;
import com.example.cda.data.SecondaryData;
import com.example.cda.entry.User;
import com.example.cda.utils.AccelerationTuple;
import com.example.cda.utils.Calculator;
import com.example.cda.utils.Constants;
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
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.TimeUnit;

import static android.content.Context.SENSOR_SERVICE;
import static android.os.Environment.getExternalStorageDirectory;
import static com.example.cda.utils.Constants.AUTO_DISMISS_MILLIS;
import static com.example.cda.utils.Constants.CLOSE_ZOOM;
import static com.example.cda.utils.Constants.COUNT_DOWN_INTERVAL_MILLIS;
import static com.example.cda.utils.Constants.G_FORCE_THRESHOLD;
import static com.example.cda.utils.Constants.INTERNAL_DATA_BUFFER_SIZE;
import static com.example.cda.utils.Constants.LOCATION_INTERVAL_SECS;
import static com.example.cda.utils.Constants.MICROPHONE_INTERVAL_SECS;
import static com.example.cda.utils.Constants.MS2KMH;
import static com.example.cda.utils.Constants.SECS2MS;
import static com.example.cda.utils.Constants.VEHICLE_EMERGENCY_DECELERATION_THRESHOLD;
import static com.example.cda.utils.Constants.VEHICLE_FIRST_GEAR_SPEED_THRESHOLD;

public class HomeFragment extends Fragment implements OnMapReadyCallback {
    
    private static final String HOME = "HOME";
    private static final String PRIMARY_DATA = "PRIMARY_DATA";
    private static final String SECONDARY_DATA = "SECONDARY_DATA";
    private static final String CDA = "CDA";
    private static final String SPEED = "SPEED";

    private static final Calculator calculator = Calculator.getInstance();
    private PrimaryData primaryData;
    private SecondaryData secondaryData;

    private ArrayList<Double> bufferOmega;
    private ArrayList<Double> bufferGForce;
    private ArrayList<Double> bufferDecibel;

    private SupportMapFragment mapFragment;
    private GoogleMap googleMap;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Marker marker;
    private Location oldLocation;
    private Location newLocation;

    private SensorManager manager;
    private float prevTimestamp;

    private MediaRecorder recorder;
    private String audioName;

    private Button monitorButton;
    private TextView speedTxt;
    private TextView statusTxt;
    private CSVWriter dataWriter;

    private String phoneNo, message;

    private boolean running = false;
    private boolean crash = false;

    private Handler calculateRunningAverageThreadHandler;
    private Runnable calculateRunningAverageThread = new Runnable() {
        @Override
        public void run() {

            double averageOmega, averageGForce, averageDecibel;
            if(!bufferOmega.isEmpty()){
                averageOmega = calculator.calculateAverage(bufferOmega);
                primaryData.getBufferOmega().add(averageOmega);
                bufferOmega.clear();
            }

            if(!bufferGForce.isEmpty()){
                averageGForce = calculator.calculateAverage(bufferGForce);
                primaryData.getBufferGForce().add(averageGForce);
                bufferGForce.clear();
            }

            if(!bufferDecibel.isEmpty()){
                averageDecibel = calculator.calculateAverage(bufferDecibel);
                primaryData.getBufferDecibels().add(averageDecibel);
                bufferDecibel.clear();
            }

            Log.v(PRIMARY_DATA, "Omega" + primaryData.getBufferOmega());
            Log.v(PRIMARY_DATA, "GForce" + primaryData.getBufferGForce());
            Log.v(PRIMARY_DATA, "Decibels" + primaryData.getBufferDecibels());
            Log.v(PRIMARY_DATA, "Speed" + primaryData.getBufferSpeed());


            calculateRunningAverageThreadHandler.postDelayed(this, (long) (LOCATION_INTERVAL_SECS*SECS2MS));
        }
    };

    private Handler calculateRateOfChangeThreadHandler;
    private Runnable calculateRateOfChangeThread = new Runnable() {
        @Override
        public void run() {
            double[] gForcePair = null, decibelPair = null, omegaPair = null;

            if(primaryData.getBufferGForce().size() >= 2) {
                gForcePair = primaryData.getBufferGForce().getRecentPair();
            }
            if(primaryData.getBufferDecibels().size() >= 2) {
                decibelPair = primaryData.getBufferDecibels().getRecentPair();
            }
            if(primaryData.getBufferOmega().size() >=2){
                omegaPair = primaryData.getBufferOmega().getRecentPair();
            }


            double jerk, decibelROC, angularAcceleration;
            if(omegaPair != null){
                angularAcceleration = calculator.calculateRateOfChange(omegaPair[1], omegaPair[0], 1);
                secondaryData.getBufferAngularAcceleration().add(angularAcceleration);
            }
            
            if(gForcePair != null){
                jerk = calculator.calculateRateOfChange(gForcePair[1], gForcePair[0], 1);
                secondaryData.getBufferGForceJerk().add(jerk);
            }

            if(decibelPair != null){
                decibelROC = calculator.calculateRateOfChange(decibelPair[1], decibelPair[0], 1);
                secondaryData.getBufferDecibelROC().add(decibelROC);
            }


            Log.v(SECONDARY_DATA, "Angular Acceleration" + secondaryData.getBufferAngularAcceleration());
            Log.v(SECONDARY_DATA, "Jerk (GForce)" + secondaryData.getBufferGForceJerk());
            Log.v(SECONDARY_DATA, "Decibels ROC" + secondaryData.getBufferDecibelROC());
            Log.v(SECONDARY_DATA, "Acceleration" + secondaryData.getBufferAcceleration());

            if(dataWriter!=null){
                AccelerationTuple accelerationTuple = secondaryData.getBufferAcceleration().recent();
                if(accelerationTuple == null){
                    accelerationTuple = new AccelerationTuple(0,0);
                }
                dataWriter.writeNext(new String[]{
                        String.valueOf(primaryData.getBufferOmega().recent()),
                        String.valueOf(secondaryData.getBufferAngularAcceleration().recent()),
                        String.valueOf(primaryData.getBufferGForce().recent()),
                        String.valueOf(secondaryData.getBufferGForceJerk().recent()),
                        String.valueOf(primaryData.getBufferSpeed().recent()),
                        String.valueOf(accelerationTuple.getValue()),
                        String.valueOf(accelerationTuple.getdT()),
                        String.valueOf(primaryData.getBufferDecibels().recent()),
                        String.valueOf(secondaryData.getBufferDecibelROC().recent()),
                        "N/A",
                        String.valueOf(crash),
                        DateFormat.getDateTimeInstance().format(new Date()),
                        String.valueOf(System.currentTimeMillis())
                });
            }

            calculateRateOfChangeThreadHandler.postDelayed(this, (long) (LOCATION_INTERVAL_SECS*SECS2MS));
        }
    };

    private Handler crashDetectionThreadHandler;
    private Runnable crashDetectionThread = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            Log.v(CDA, "Checking if crash occurred...");
            boolean speedEvent = false, gForceEvent = false;

            AccelerationTuple criticalDeceleration = null;
            double currentSpeed = primaryData.getBufferSpeed().recent();
            Log.v(CDA, "Analysing speed parameter");
            if(currentSpeed >= 0 && currentSpeed <= VEHICLE_FIRST_GEAR_SPEED_THRESHOLD){ // vehicle is idle / moving slowly
                Log.v(CDA, "Vehicle is moving slowly @ " + currentSpeed + " km/h");
                OptionalDouble runningAverageOptional = primaryData.getBufferSpeed()
                                                    .stream()
                                                    .mapToDouble(Double::doubleValue)
                                                    .average();
                /*double runningAverage = primaryData.getBufferSpeed().previous();
                if (runningAverage > VEHICLE_FIRST_GEAR_SPEED_THRESHOLD) { // vehicle was previously moving
                    Log.v(CDA, "Vehicle was previously moving @ ~" + runningAverage + " km/h");

                    double deceleration = (currentSpeed/MS2KMH - runningAverage/MS2KMH);
                    Log.v(CDA, "Speed: " + primaryData.getBufferSpeed());
                    Log.v(CDA, "Acceleration: " + primaryData.getBufferAcceleration());
                    Log.v(CDA, "Deceleration: " + deceleration);
                    if (deceleration <= VEHICLE_EMERGENCY_DECELERATION_THRESHOLD) { // recent critical deceleration exists
                        Log.v(CDA, "Vehicle experienced critical deceleration @ " + deceleration + " m/s^2");
                        speedEvent = true;
                        criticalDecelerationIndex = primaryData.getBufferAcceleration().indexOf(deceleration);
                        Log.v(CDA, "Critical Deceleration Index: " + criticalDecelerationIndex);

                    } else {
                        Log.v(CDA, "Vehicle did not experience critical deceleration");
                    }
                } else {
                    Log.v(CDA, "Vehicle was still previously moving slowly @ ~" + runningAverage + " km/h");
                }*/
                if(runningAverageOptional.isPresent()) {
                    double runningAverage = runningAverageOptional.getAsDouble();
                    if (runningAverage > VEHICLE_FIRST_GEAR_SPEED_THRESHOLD) { // vehicle was previously moving
                        Log.v(CDA, "Vehicle was previously moving @ ~" + runningAverage + " km/h");

                        Optional<AccelerationTuple> decelerationOptional = secondaryData.getBufferAcceleration()
                                .stream()
                                .filter(x -> x.getValue() <= VEHICLE_EMERGENCY_DECELERATION_THRESHOLD)
                                .findFirst();
                        Log.v(CDA, "Speed: " + primaryData.getBufferSpeed());
                        Log.v(CDA, "Acceleration: " + secondaryData.getBufferAcceleration());

                        if (decelerationOptional.isPresent()) { // recent critical deceleration exists
                            Log.v(CDA, "Vehicle experienced critical deceleration @ " + decelerationOptional.get().getValue() + " m/s^2");
                            speedEvent = true;
                            criticalDeceleration = secondaryData.getBufferAcceleration().indexOf(decelerationOptional.get());
                            Log.v(CDA, "Critical deceleration occurred " + criticalDeceleration.getdT() +" seconds ago");
                        } else {
                            Log.v(CDA, "Vehicle did not experience critical deceleration");
                        }
                    } else {
                        Log.v(CDA, "Vehicle was still previously moving slowly @ ~" + runningAverage + " km/h");
                    }
                }else{
                    Log.v(CDA, "Previous vehicle speeds not available.");
                }
            }else{
                Log.v(CDA, "Vehicle still moving @ " + currentSpeed + " km/h");
            }

            Log.v(CDA, "Analysing g-force parameter"); // used to confirm the critical deceleration
            if(criticalDeceleration != null){
                Log.v(CDA, "Checking g-force during critical deceleration");
                if(criticalDeceleration.getdT() > INTERNAL_DATA_BUFFER_SIZE){
                    Log.v(CDA, "No information available about g-force " + criticalDeceleration.getdT() + " seconds ago");
                }else {
                    Log.v(CDA, "Fetching g-force from " + criticalDeceleration.getdT() + " seconds ago");

                    double gForceDuringDeceleration = primaryData.getBufferGForce().closestMax(Math.round(criticalDeceleration.getdT()));
                    Log.v(CDA, "Vehicle experienced " + gForceDuringDeceleration + " G during critical deceleration ");
                    if (gForceDuringDeceleration >= G_FORCE_THRESHOLD) {
                        Log.v(CDA, "Deceleration confirmed by g-force");
                        gForceEvent = true;
                    } else {
                        Log.v(CDA, "Vehicle did not experience critical g-force");
                    }
                }
            }else{
                Log.v(CDA, "Critical deceleration did not occur");
            }

            if(speedEvent && gForceEvent){
                crash = true;
            }


            if(crash){
                SOS();
            }

            crashDetectionThreadHandler.postDelayed(this, (long) (LOCATION_INTERVAL_SECS*SECS2MS));

        }
    };

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
        if (locationResult.getLocations().size() > 0) {
            oldLocation = newLocation;
            newLocation = locationResult.getLastLocation();
            Log.v(HOME, newLocation.toString());

            if (marker != null) {
                marker.remove();
            }

            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(new LatLng(newLocation.getLatitude(), newLocation.getLongitude()));
            markerOptions.flat(true);
            markerOptions.anchor(0.5f, 0.5f);
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.car_color));
            float bearing;
            if (newLocation.hasBearing()) {
                bearing = newLocation.getBearing();
            } else {
                bearing = 0;
            }
            markerOptions.rotation(bearing);
            marker = googleMap.addMarker(markerOptions);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(newLocation.getLatitude(), newLocation.getLongitude()), CLOSE_ZOOM));

            if(oldLocation != null) { // More than one location is available

                if(running) {
                    float delayBetweenLocations = (float) ((newLocation.getTime() - oldLocation.getTime()) / Constants.SECS2MS);

                    /** Temporary solution to acceleration being calculated incorrectly **/
                    if(delayBetweenLocations < 0){ // if negative
                        delayBetweenLocations = -delayBetweenLocations;
                    }
                    if(delayBetweenLocations < 1){ // if a decimal below 1
                        delayBetweenLocations = 1;
                    }
                    if(delayBetweenLocations != Math.round(delayBetweenLocations)){ // if not a whole number
                        delayBetweenLocations = Math.round(delayBetweenLocations);
                    }

                    double currentSpeed = calculator.calculateCurrentSpeed(oldLocation, newLocation);
                    Log.v(SPEED, "Calculating speed between locations");
                    primaryData.getBufferSpeed().add(currentSpeed); // calculate speed between locations
                    Log.v(SPEED, "Current speed: " + currentSpeed + " km/h");
                    Log.v(SPEED, "Delay between locations: " + delayBetweenLocations + "s");

                    if (primaryData.getBufferSpeed().size() >= 2) { // calculate acceleration between locations
                        Log.v(SPEED, "Calculating acceleration between locations");
                        double[] speedPair = primaryData.getBufferSpeed().getRecentPair();
                        double acceleration = calculator.calculateRateOfChange(speedPair[1]/MS2KMH, speedPair[0]/MS2KMH, delayBetweenLocations);
                        AccelerationTuple accTuple = new AccelerationTuple(acceleration, delayBetweenLocations);
                        secondaryData.getBufferAcceleration().add(accTuple);
                        Log.v(SPEED, "Acceleration between locations: " + acceleration + " m/s^2");
                    }

                    if(primaryData.getBufferSpeed().recent() <= VEHICLE_FIRST_GEAR_SPEED_THRESHOLD) {
                        speedTxt.setText("<24 km/h");
                    } else{
                        speedTxt.setText(String.format("%s km/h", Math.floor(primaryData.getBufferSpeed().recent())));
                    }
                }
            }
        }
        }
    };

    private SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
                double gforce = calculator.calculateGForce(event.values[0], event.values[1], event.values[2]);
                bufferGForce.add(gforce);
            }
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                if(prevTimestamp!=0) {
                    double omega = calculator.calculateOmega(event.values[0],
                            event.values[1], event.values[2], prevTimestamp, event.timestamp);
                    bufferOmega.add(omega);
                }
                prevTimestamp = event.timestamp;
            }
            /*if(dataWriter!= null){
               /* String triggeredEvent = "";
                if(((primaryData.getCurrentGForce()/G_FORCE_THRESHOLD) + (primaryData.getCurrentDecibels()/SOUND_PRESSURE_LEVEL_THRESHOLD)) >= ACCIDENT_THRESHOLD && (primaryData.getCurrentSpeed()>=VEHICLE_SPEED_THRESHOLD)){ // travelling and hit
                    crash= true;
                    triggeredEvent = "1";
                    Log.v(HOME, "Crash occured: " + "("+ newLocation.getLatitude() + "," +newLocation.getLongitude() +")" +
                            "\n Speed: " + primaryData.getCurrentSpeed() + "km/h" +
                            "\n Omega: " + primaryData.getCurrentOmega() + "°" +
                            "\n dB: " + primaryData.getCurrentDecibels() + "dB");
                }else if(((primaryData.getCurrentGForce()/G_FORCE_THRESHOLD) + (primaryData.getCurrentDecibels()/SOUND_PRESSURE_LEVEL_THRESHOLD)) >= ACCIDENT_THRESHOLD && (primaryData.getCurrentOmega()>=Omega_THRESHOLD)) { // hit and vehicle overturned (Omega utilises pitch & roll)
                    crash = true;
                    triggeredEvent = "2";
                    Log.v(HOME, "Crash occured: " + "("+ newLocation.getLatitude() + "," +newLocation.getLongitude() +")" +
                            "\n Speed: " + primaryData.getCurrentSpeed() + "km/h" +
                            "\n Omega: " + primaryData.getCurrentOmega() + "°" +
                            "\n dB: " + primaryData.getCurrentDecibels() + "dB");
                }else if( (primaryData.getCurrentSpeed() < VEHICLE_SPEED_THRESHOLD) && (((primaryData.getCurrentGForce()/G_FORCE_THRESHOLD) + (primaryData.getCurrentDecibels()/SOUND_PRESSURE_LEVEL_THRESHOLD) + (speedDeviation/STANDARD_DEVIATION_THRESHOLD)) >= LOW_SPEED_ACCIDENT_THRESHOLD)){ // hit while slowly moving
                    crash = true;
                    triggeredEvent = "3";
                    Log.v(HOME, "Crash occured: " + "("+ newLocation.getLatitude() + "," +newLocation.getLongitude() +")" +
                            "\n Speed: " + primaryData.getCurrentSpeed() + "km/h" +
                            "\n Omega: " + primaryData.getCurrentOmega() + "°" +
                            "\n dB: " + primaryData.getCurrentDecibels() + "dB");
                }else{
                    crash = false;
                }
                dataWriter.writeNext(new String[]{String.valueOf(primaryData.getCurrentOmega()), String.valueOf(primaryData.getCurrentGForce()), String.valueOf(primaryData.getCurrentSpeed()), String.valueOf(primaryData.getCurrentDecibels()), String.valueOf(crash), triggeredEvent, DateFormat.getDateTimeInstance().format(new Date()), String.valueOf(System.currentTimeMillis()/SECS2MS)});
                if(crash){
                    SOS();
                }
            }*/
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private HomeViewModel homeViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        speedTxt = root.findViewById(R.id.speed_text);
        statusTxt = root.findViewById(R.id.status_text);
        monitorButton = root.findViewById(R.id.monitor_button);
        monitorButton.setTag("ready");

        primaryData = new PrimaryData();
        secondaryData = new SecondaryData();
        bufferDecibel = new ArrayList<>();
        bufferGForce = new ArrayList<>();
        bufferOmega = new ArrayList<>();
        calculateRunningAverageThreadHandler = new Handler();
        calculateRateOfChangeThreadHandler = new Handler();
        crashDetectionThreadHandler = new Handler();

        monitorButton.setOnClickListener(v -> {
            if(v.getId() == monitorButton.getId()){
                if(monitorButton.getTag().equals("ready")) {
                    Log.v(HOME, "Activating crash detection algorithm...");
                    running = true;
                    setupWriters();
                    manager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
                    manager.registerListener(sensorListener, manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_GAME);
                    manager.registerListener(sensorListener, manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME);
                    startRecording();
                    calculateRunningAverageThreadHandler.postDelayed(calculateRunningAverageThread, 0);
                    crashDetectionThreadHandler.postDelayed(crashDetectionThread, (long) (LOCATION_INTERVAL_SECS*SECS2MS)); // does not have access to the most up to date ROC values therefore delay is added
                    calculateRateOfChangeThreadHandler.postDelayed(calculateRateOfChangeThread, (long) (LOCATION_INTERVAL_SECS*SECS2MS*2)); //TODO TimeUnit.SECONDS.toMilis()

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

        return root;
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
            Log.v(HOME, "Recording failed");
        }

        recorder.start();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                double dB = calculator.calculateDecibels(recorder);
                bufferDecibel.add(dB);
                handler.postDelayed(this, (long) (MICROPHONE_INTERVAL_SECS*SECS2MS)); //20ms caused alot of -Infinity values (rubbish values)
            }
        }, 0);
    }

    private void isLocationServiceEnabled(){
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
            new AlertDialog.Builder(new ContextThemeWrapper(getContext(), R.style.AlertStyle))
                    .setMessage("The application will not work without location services.")
                    .setPositiveButton("Open Location Settings", (paramDialogInterface, paramInt) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setCancelable(false)
                    .show();
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
        isLocationServiceEnabled();
        if(fusedLocationProviderClient != null){
            requestLocation();
        }else {
            mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());
            if (mapFragment != null) {
                mapFragment.getMapAsync(this);
            } else {
                Log.v(HOME, "Could not load map fragment");
            }
        }
        if(dataWriter == null) {
            setupWriters();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
        stop();
    }

    private void stop(){
        running = false;
        speedTxt.setText("N/A");
        monitorButton.setText("Start Monitoring");
        monitorButton.setTag("ready");
        statusTxt.setText("Off");
        if(calculateRunningAverageThreadHandler.hasCallbacks(calculateRunningAverageThread)){
            calculateRunningAverageThreadHandler.removeCallbacks(calculateRunningAverageThread);
        }
        if(calculateRateOfChangeThreadHandler.hasCallbacks(calculateRateOfChangeThread)){
            calculateRateOfChangeThreadHandler.removeCallbacks(calculateRateOfChangeThread);
        }
        if(crashDetectionThreadHandler.hasCallbacks(crashDetectionThread)){
            crashDetectionThreadHandler.removeCallbacks(crashDetectionThread);
        }
        if(primaryData != null){
            primaryData.clearBuffers();
        }
        if(secondaryData != null){
            secondaryData.clearBuffers();
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
            if(dataWriter != null) {
                dataWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void SOS(){
        stop();
        Log.v(HOME, "Crash detected");
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
            @Override
            public void onShow(final DialogInterface dialog) {
                final Button defaultButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);
                final CharSequence negativeButtonText = defaultButton.getText();
                new CountDownTimer(AUTO_DISMISS_MILLIS, COUNT_DOWN_INTERVAL_MILLIS) {
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
                            Log.v(HOME, "User did not respond to alert, sending message to emergency contact.");
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
        Log.v(HOME, "Building message for emergency contact: " + phoneNo);
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
                        "Last speed: " + Math.floor(primaryData.getBufferSpeed().recent()) + " km/h \n" +
                        "G-Force experienced: " + primaryData.getBufferGForce().recent() + "\n\n" +
                        "You received this message because " + user.getFirstName() + " has listed you as an emergency contact in BSafe.";

            Log.v(HOME, "Sending message to " + phoneNo);
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
            Log.v(HOME, "Initiating location request..");
            requestLocation();
        }
    }

    private void requestLocation() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval((long) (LOCATION_INTERVAL_SECS*SECS2MS));
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        //googleMap.setMyLocationEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.setIndoorEnabled(false);
        googleMap.setTrafficEnabled(true);
        googleMap.setBuildingsEnabled(false);
    }

    private void setupWriters(){
        audioName = getActivity().getExternalCacheDir().getAbsolutePath() + "/audiorecordtest.3gp";
        String baseDir = getExternalStorageDirectory().getAbsolutePath();
        String fileName = "crash_data.csv";
        String filePath = baseDir + File.separator + fileName;
        Log.v(HOME, "Writing to " + filePath);
        File f = new File(filePath);
        FileWriter mFileWriter;

        // File exist
        try {
            if (f.exists() && !f.isDirectory()) {
                mFileWriter = new FileWriter(filePath, false);
                dataWriter = new CSVWriter(mFileWriter);
            } else {
                dataWriter = new CSVWriter(new FileWriter(filePath));
            }
            dataWriter.writeNext(new String[]{
                    "Omega",
                    "Angular Acceleration",
                    "G-Force",
                    "Jerk",
                    "Speed",
                    "Acceleration",
                    "dT",
                    "dB",
                    "dB Delta",
                    "Case",
                    "Crash",
                    "Timestamp",
                    "Milliseconds"
            });
        } catch (IOException e) {
            Log.v(HOME,e.getMessage());
        }
    }
}