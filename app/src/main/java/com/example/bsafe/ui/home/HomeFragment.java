package com.example.bsafe.ui.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
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
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.example.bsafe.ui.MainActivity;
import com.example.bsafe.R;
import com.example.bsafe.data.PrimaryData;
import com.example.bsafe.data.SecondaryData;
import com.example.bsafe.ui.entry.LoginActivity;
import com.example.bsafe.utils.AccelerationTuple;
import com.example.bsafe.utils.Alert;
import com.example.bsafe.utils.Calculator;
import com.example.bsafe.utils.User;
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
import static com.example.bsafe.utils.Constants.CLOSE_ZOOM;
import static com.example.bsafe.utils.Constants.G_FORCE_THRESHOLD;
import static com.example.bsafe.utils.Constants.INTERNAL_DATA_BUFFER_SIZE;
import static com.example.bsafe.utils.Constants.MS2KMH;
import static com.example.bsafe.utils.Constants.ROTATION_THRESHOLD;
import static com.example.bsafe.utils.Constants.SECS2MS;
import static com.example.bsafe.utils.Constants.EMERGENCY_DECELERATION_THRESHOLD;
import static com.example.bsafe.utils.Constants.FIRST_GEAR_SPEED_THRESHOLD;
import static com.example.bsafe.utils.Constants.WIGGLE;

public class HomeFragment extends Fragment implements OnMapReadyCallback {
    
    private static final String HOME = "HOME";
    private static final String PRIMARY_DATA = "PRIMARY_DATA";
    private static final String SECONDARY_DATA = "SECONDARY_DATA";
    private static final String CDA = "CDA";
    private static final String SPEED = "SPEED";

    private static final Calculator calculator = Calculator.getInstance();
    private PrimaryData primaryData;
    private SecondaryData secondaryData;

    private double fixedOrientation;
    private ArrayList<Double> bufferOrientation; // holds sums of pitch and roll
    private ArrayList<Double> bufferGForce;

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Marker marker;
    private Location oldLocation;
    private Location newLocation;

    private SensorManager manager;

    private Button monitorButton;
    private TextView speedTxt;
    private TextView statusTxt;
    private CSVWriter dataWriter;

    private String phoneNo, message;

    private boolean running = false;
    private boolean crash = false;


    private BroadcastReceiver sentMessage;
    private BroadcastReceiver deliveredMessage;
    private Handler repeatSendMessageThreadHandler;
    private Runnable repeatSendMessageThread;

    private Handler calculateRunningAverageThreadHandler;
    private Runnable calculateRunningAverageThread;

    private Handler crashDetectionThreadHandler;
    private Runnable crashDetectionThread;

    private Handler deviceOrientationThreadHandler;
    private Runnable deviceOrientationThread;

    private String crashEvent = "N/A";

    private LocationCallback locationCallback;

    private SensorEventListener sensorListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        speedTxt = root.findViewById(R.id.speed_text);
        statusTxt = root.findViewById(R.id.status_text);
        monitorButton = root.findViewById(R.id.monitor_button);
        monitorButton.setTag("ready");

        primaryData = new PrimaryData();
        secondaryData = new SecondaryData();
        bufferGForce = new ArrayList<>();
        bufferOrientation = new ArrayList<>();

        calculateRunningAverageThreadHandler = new Handler();
        crashDetectionThreadHandler = new Handler();
        deviceOrientationThreadHandler = new Handler();
        repeatSendMessageThreadHandler = new Handler();

        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        calculateRunningAverageThread = new Runnable() {
            @Override
            public void run() {

                double averageOrientation, averageGForce;
                if (!bufferOrientation.isEmpty()) {
                    averageOrientation = calculator.calculateAverage(bufferOrientation);
                    primaryData.getBufferOrientation().add(averageOrientation);
                    bufferOrientation.clear();
                }

                if (!bufferGForce.isEmpty()) {
                    averageGForce = calculator.calculateAverage(bufferGForce);
                    primaryData.getBufferGForce().add(averageGForce);
                    bufferGForce.clear();
                }

                Log.v(PRIMARY_DATA, "Orientation" + primaryData.getBufferOrientation());
                Log.v(PRIMARY_DATA, "GForce" + primaryData.getBufferGForce());
                Log.v(PRIMARY_DATA, "Speed" + primaryData.getBufferSpeed());
                Log.v(SECONDARY_DATA, "Acceleration" + secondaryData.getBufferAcceleration());


                if(!crash) {
                    calculateRunningAverageThreadHandler.postDelayed(this, TimeUnit.SECONDS.toMillis(1));
                }
            }
        };

        crashDetectionThread = new Runnable() { // CDA running every second
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void run() {
                Log.v(CDA, "Checking if crash occurred...");
                boolean speedEvent = false, gForceEvent = false, rotationEvent = false;

                // Speed Parameter
                AccelerationTuple criticalDeceleration = null;
                if (!primaryData.getBufferSpeed().isEmpty()) {
                    double currentSpeed = primaryData.getBufferSpeed().recent();
                    Log.v(CDA, "Analysing speed parameter");
                    if (currentSpeed >= 0 && currentSpeed <= FIRST_GEAR_SPEED_THRESHOLD) { // vehicle is idle / moving slowly
                        Log.v(CDA, "Vehicle is moving slowly @ " + currentSpeed + " km/h");
                        OptionalDouble runningAverageOptional = primaryData.getBufferSpeed()
                                .stream()
                                .mapToDouble(Double::doubleValue)
                                .average();

                        if (runningAverageOptional.isPresent()) {
                            double runningAverage = runningAverageOptional.getAsDouble();
                            if (runningAverage > FIRST_GEAR_SPEED_THRESHOLD) { // vehicle was previously moving
                                Log.v(CDA, "Vehicle was previously moving @ ~" + runningAverage + " km/h");

                                Optional<AccelerationTuple> decelerationOptional = secondaryData.getBufferAcceleration()
                                        .stream()
                                        .filter(x -> x.getValue() <= EMERGENCY_DECELERATION_THRESHOLD)
                                        .findFirst();
                                Log.v(CDA, "Speed: " + primaryData.getBufferSpeed());
                                Log.v(CDA, "Acceleration: " + secondaryData.getBufferAcceleration());

                                if (decelerationOptional.isPresent()) { // recent critical deceleration exists
                                    Log.v(CDA, "Vehicle experienced critical deceleration @ " + decelerationOptional.get().getValue() + " m/s^2");
                                    speedEvent = true;
                                    criticalDeceleration = secondaryData.getBufferAcceleration().retrieveObjectWithValue(decelerationOptional.get());
                                    Log.v(CDA, "Critical deceleration occurred " + criticalDeceleration.getTime() + " seconds ago");
                                } else {
                                    Log.v(CDA, "Vehicle did not experience critical deceleration");
                                }
                            } else {
                                Log.v(CDA, "Vehicle was still previously moving slowly @ ~" + runningAverage + " km/h");
                            }
                        } else {
                            Log.v(CDA, "Previous vehicle speeds not available.");
                        }
                    } else {
                        Log.v(CDA, "Vehicle still moving @ " + currentSpeed + " km/h");
                    }
                } else {
                    Log.v(CDA, "Vehicle has not moved yet.");
                }

                // confirm critical deceleration with g-force, if it exists
                if (speedEvent) {
                    Log.v(CDA, "Checking g-force during critical deceleration");
                    if (criticalDeceleration.getTime() > INTERNAL_DATA_BUFFER_SIZE) {
                        Log.v(CDA, "No information available about g-force " + criticalDeceleration.getTime() + " seconds ago");
                    } else {
                        Log.v(CDA, "Fetching g-force from " + criticalDeceleration.getTime() + " seconds ago");

                        double gForceDuringDeceleration = primaryData.getBufferGForce().closestMax(Math.round(criticalDeceleration.getTime()));
                        Log.v(CDA, "Vehicle experienced " + gForceDuringDeceleration + " G during critical deceleration ");
                        if (gForceDuringDeceleration >= G_FORCE_THRESHOLD) { // check if g-force was critical during critical deceleration
                            Log.v(CDA, "Deceleration confirmed by g-force");
                            gForceEvent = true;
                        } else {
                            Log.v(CDA, "Vehicle did not experience critical g-force during critical deceleration");
                        }
                    }
                }

                // Rotation Parameter
                if(!primaryData.getBufferOrientation().isEmpty()){
                    Log.v(CDA, "Analysing orientation parameter");
                    double currentOrientation = primaryData.getBufferOrientation().recent();
                    double rotation = (fixedOrientation - currentOrientation);
                    Log.v(CDA, "Vehicle's wheels are raised by " + rotation + " degrees");
                    if(rotation >= ROTATION_THRESHOLD || rotation <= -ROTATION_THRESHOLD){ // check if clockwise/anti-clockwise rotation was critical
                        Log.v(CDA, "Vehicle at risk overturning with wheels " + (rotation < 0 ? -rotation:rotation) + " deg. above ground");
                        rotationEvent = true;
                    }else{
                        Log.v(CDA, "Vehicle did not experience a critical rotation");
                    }
                }

                // confirm critical rotation with g-force, if it exists
                if(rotationEvent){
                    Log.v(CDA, "Checking g-force during critical rotation");
                    double gForceDuringRotation = primaryData.getBufferGForce().closestMax(0); // gforce && rotation buffers are synced
                    Log.v(CDA, "Vehicle experienced " + gForceDuringRotation + " G during critical rotation");
                    if(gForceDuringRotation >= G_FORCE_THRESHOLD){ // check if g-force was critical during critical rotation
                        Log.v(CDA, "Rotation confirmed by g-force");
                        gForceEvent = true;
                    } else{
                        Log.v(CDA, "Vehicle did not experience critical g-force during critical rotation");
                    }
                }

                if(rotationEvent && gForceEvent){
                    crash = true;
                    crashEvent = "Overturned";
                }

                if (speedEvent && gForceEvent) {
                    crash = true;
                    crashEvent = "Collision";
                }

                // write current data to csv file for debugging
                if (dataWriter != null) {
                    AccelerationTuple accelerationTuple = secondaryData.getBufferAcceleration().recent();
                    if (accelerationTuple == null) {
                        accelerationTuple = new AccelerationTuple(0, 0);
                    }
                    double currOrientation, speed, gforce;
                    if(primaryData.getBufferOrientation().isEmpty()){
                        currOrientation = 0;
                    }else{
                        currOrientation = primaryData.getBufferOrientation().recent();
                    }

                    if(primaryData.getBufferSpeed().isEmpty()){
                        speed = 0;
                    }else{
                        speed = primaryData.getBufferOrientation().recent();
                    }

                    if(primaryData.getBufferGForce().isEmpty()){
                        gforce = 0;
                    }else{
                        gforce = primaryData.getBufferGForce().recent();
                    }

                    dataWriter.writeNext(new String[]{
                            String.valueOf(fixedOrientation - currOrientation),
                            String.valueOf(gforce),
                            String.valueOf(speed),
                            String.valueOf(accelerationTuple.getValue()),
                            String.valueOf(accelerationTuple.getTime()),
                            crashEvent,
                            String.valueOf(crash),
                            DateFormat.getDateTimeInstance().format(new Date()),
                            String.valueOf(System.currentTimeMillis())
                    });
                }

                // if crash occurred, the runnable needs to be stopped
                if (crash) {
                    SOS();
                }else {
                    crashDetectionThreadHandler.postDelayed(this, (long) TimeUnit.SECONDS.toMillis(1));
                }
            }
        };

        deviceOrientationThread = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void run() {
                if(!primaryData.getBufferOrientation().isEmpty()){
                    if(fixedOrientation == 0){
                        fixedOrientation = primaryData.getBufferOrientation().recent();
                        Log.v("ORIENTATION", "Initialised to: " + fixedOrientation);

                    }else{ 
                        // checking if the fixed position of the device has changed
                        ArrayList<Double> orientations = new ArrayList<>(primaryData.getBufferOrientation());
                        int sameElements = 0;
                       for(int i=0; i<primaryData.getBufferOrientation().size()-1;i++){
                           double curr = orientations.get(i);
                           double next = orientations.get(i+1);
                           if((curr-WIGGLE <= next && curr+WIGGLE >=next) || curr==next){
                               Log.v("ORIENTATION", "Comparing: " + curr + " " + next);
                               sameElements++;
                           }else{
                               Log.v("ORIENTATION", "Not same: " + curr + " " + next);

                               break;
                           }
                       }
                       if(sameElements==primaryData.getBufferOrientation().size()-1){
                           fixedOrientation = primaryData.getBufferOrientation().recent();
                           Log.v("ORIENTATION", "Fixed position changed to " + fixedOrientation);
                       }
                    }
                }

                // if crash occurred, the runnable needs to be stopped
                if(!crash) {
                    deviceOrientationThreadHandler.postDelayed(this, TimeUnit.SECONDS.toMillis(5));
                }
            }
        };

        repeatSendMessageThread = new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(), "Trying to send SMS again", Toast.LENGTH_LONG).show();
                sendSMS();
                repeatSendMessageThreadHandler.postDelayed(this, TimeUnit.SECONDS.toMillis(30));
            }
        };

        sensorListener = new SensorEventListener() {
            float[] gravityValues;
            float[] magneticValues;
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                    double gforce = calculator.calculateGForce(event.values[0], event.values[1], event.values[2]);
                    bufferGForce.add(gforce);
                }

                if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                    gravityValues = event.values;
                }
                if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
                    magneticValues = event.values;
                }

                if(gravityValues != null && magneticValues != null){
                   double orientation = calculator.calculateOrientation(gravityValues, magneticValues);
                   if(orientation != Double.NEGATIVE_INFINITY) {
                       bufferOrientation.add(orientation);
                   }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        locationCallback = new LocationCallback() {
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
                    markerOptions.rotation(bearing + 180);
                    marker = googleMap.addMarker(markerOptions);
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(newLocation.getLatitude(), newLocation.getLongitude()), CLOSE_ZOOM));

                    if (oldLocation != null) { // More than one location is available

                        float delayBetweenLocations = (float) ((newLocation.getTime() - oldLocation.getTime()) / SECS2MS);

                        // Small fix to a glitch where the time between each location contains a decimal would result in an incorrect acceleration
                        // Could not find reason for this glitch
                        if (delayBetweenLocations < 0) { // if negative
                            delayBetweenLocations = -delayBetweenLocations;
                        }
                        if (delayBetweenLocations < 1) { // if a decimal below 1
                            delayBetweenLocations = 1;
                        }
                        if (delayBetweenLocations != Math.round(delayBetweenLocations)) { // if not a whole number
                            delayBetweenLocations = Math.round(delayBetweenLocations);
                        }

                        double currentSpeed = calculator.calculateCurrentSpeed(oldLocation, newLocation);
                        Log.v(SPEED, "Calculating speed between locations");
                        Log.v(SPEED, "Current speed: " + currentSpeed + " km/h");
                        Log.v(SPEED, "Delay between locations: " + delayBetweenLocations + "s");

                        if (currentSpeed <= FIRST_GEAR_SPEED_THRESHOLD) {
                            speedTxt.setText(R.string.LessThanTwentyFourKmh);
                        } else {
                            speedTxt.setText(String.format("%s km/h", Math.floor(currentSpeed)));
                        }

                        if (running) {
                            primaryData.getBufferSpeed().add(currentSpeed);
                            if (primaryData.getBufferSpeed().size() >= 2) { // calculate acceleration between locations
                                Log.v(SPEED, "Calculating acceleration between locations");
                                double[] speedPair = primaryData.getBufferSpeed().getRecentPair();
                                double acceleration = calculator.calculateRateOfChange(speedPair[1] / MS2KMH, speedPair[0] / MS2KMH, delayBetweenLocations);
                                AccelerationTuple accTuple = new AccelerationTuple(acceleration, delayBetweenLocations);
                                secondaryData.getBufferAcceleration().add(accTuple);
                                Log.v(SPEED, "Acceleration between locations: " + acceleration + " m/s^2");
                            }
                        }


                    }
                }
            }
        };

        sentMessage = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(getContext(), "Alert sent to emergency contact", Toast.LENGTH_LONG).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        // if could not send SMS, initiate runnable that will send it again in 30 seconds
                        if(!repeatSendMessageThreadHandler.hasCallbacks(repeatSendMessageThread)){
                            repeatSendMessageThreadHandler.postDelayed(repeatSendMessageThread, 0);
                        }
                        break;
                }
            }
        };

        deliveredMessage = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(getContext(), "Alert delivered to emergency contact", Toast.LENGTH_LONG).show();

                        // SMS was delivered, remove all corresponding receivers and runnables
                        crash = false;
                        getActivity().unregisterReceiver(sentMessage);
                        getActivity().unregisterReceiver(deliveredMessage);
                        if(repeatSendMessageThreadHandler.hasCallbacks(repeatSendMessageThread)){
                            repeatSendMessageThreadHandler.removeCallbacks(repeatSendMessageThread);
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        // if could not deliver SMS, initiate runnable that will send it again in 30 seconds
                        if(!repeatSendMessageThreadHandler.hasCallbacks(repeatSendMessageThread)){
                            repeatSendMessageThreadHandler.postDelayed(repeatSendMessageThread, 0);
                        }
                        break;
                }
            }
        };

        monitorButton.setOnClickListener(v -> {
            if (v.getId() == monitorButton.getId()) {
                if (monitorButton.getTag().equals("ready")) {
                    Log.v(HOME, "Activating crash detection algorithm...");
                    running = true;
                    setupWriters(); // csv file for debugging

                    //Setup sensors
                    manager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
                    manager.registerListener(sensorListener, manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_GAME);
                    manager.registerListener(sensorListener, manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
                    manager.registerListener(sensorListener, manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);

                    //Setup runnables
                    calculateRunningAverageThreadHandler.postDelayed(calculateRunningAverageThread, 0);
                    deviceOrientationThreadHandler.postDelayed(deviceOrientationThread, 0);
                    crashDetectionThreadHandler.postDelayed(crashDetectionThread, TimeUnit.SECONDS.toMillis(1)); // does not have access to the most up to date ROC values therefore delay is added

                    //Alter UI
                    monitorButton.setText(R.string.StopMonitoring);
                    statusTxt.setText(R.string.Normal);
                    monitorButton.setTag("running");
                } else {
                    stop();
                    if (primaryData != null) {
                        primaryData.clearBuffers();
                    }
                    if (secondaryData != null) {
                        secondaryData.clearBuffers();
                    }
                }
            }
        });

    }

    /**
     * Method used for checking if the device's location services are enabled
     * If disabled, the user will be prompted to open location settings and turn on location services
     */
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
        if(running) {
            stop();
            if(primaryData != null){
                primaryData.clearBuffers();
            }
            if(secondaryData != null){
                secondaryData.clearBuffers();
            }
        }
    }

    /**
     * Ensures location services are enabled on launch of the fragment
     * Once location services are enabled, request current location of the device
     */
    @Override
    public void onResume() {
        super.onResume();
        isLocationServiceEnabled();
        if(fusedLocationProviderClient != null){
            requestLocation();
        }else {
            SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
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
        if(running) {
            stop();
            if(primaryData != null){
                primaryData.clearBuffers();
            }
            if(secondaryData != null){
                secondaryData.clearBuffers();
            }
        }
    }

    /**
     * Method used to return the fragment to its original state
     * The runnables and sensors are stopped
     */
    private void stop(){
        running = false;
        monitorButton.setText(R.string.StartMonitoring);
        monitorButton.setTag("ready");
        statusTxt.setText(R.string.Off);
        if(calculateRunningAverageThreadHandler.hasCallbacks(calculateRunningAverageThread)){
            calculateRunningAverageThreadHandler.removeCallbacks(calculateRunningAverageThread);
        }
        if(crashDetectionThreadHandler.hasCallbacks(crashDetectionThread)){
            crashDetectionThreadHandler.removeCallbacks(crashDetectionThread);
        }
        if(deviceOrientationThreadHandler.hasCallbacks(deviceOrientationThread)){
            deviceOrientationThreadHandler.removeCallbacks(deviceOrientationThread);
        }
        if(manager != null) {
            manager.unregisterListener(sensorListener);
        }
        try {
            if(dataWriter != null) {
                dataWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to put the fragment into the state for alerting emergency contact
     * Launches an AlertDialogue that lets the user know a crash has been detected
     */
    private void SOS(){
        stop();
        Log.v(HOME, "Crash detected");
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        MediaPlayer mp = MediaPlayer.create(getContext(), notification);
        mp.start();
        statusTxt.setText(R.string.Crash);

        AlertDialog alert = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertStyle))
                .setTitle("Crash detected")
                .setMessage("Your emergency contact will be alerted")
                .setPositiveButton("SOS", (dialog1, which) -> {
                    buildMSD();
                    sendSMS();
                })
                .setNegativeButton("Cancel", null)
                .create();
        alert.setOnShowListener(dialog -> {
            final Button defaultButton = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);
            final CharSequence negativeButtonText = defaultButton.getText();
            // Initiate a timer for the alert in the case that the alert is a false positive and can be cancelled
            new CountDownTimer(TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(1)) {
                @Override
                public void onTick(long millisUntilFinished) {
                    defaultButton.setText(String.format(
                            Locale.getDefault(), "%s (%d)",
                            negativeButtonText,
                            TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) + 1 //add one so it never displays zero
                    ));
                }
                // Once the timer counts to zero, alert the emergency contact
                @Override
                public void onFinish() {
                    if (((AlertDialog) dialog).isShowing()) {
                        Log.v(HOME, "User did not respond to alert, sending message to emergency contact.");
                        buildMSD();
                        sendSMS();
                        mp.stop();
                        dialog.dismiss();
                        if(primaryData != null){
                            primaryData.clearBuffers();
                        }
                        if(secondaryData != null){
                            secondaryData.clearBuffers();
                        }
                    }
                }
            }.start();
        });
        alert.show();
    }

    /**
     * Method used to build MSD consisting of ->
     * Geo coordinates, timestamp, speed, g-force, personal profile, medical profile and type of crash
     */
    private void buildMSD() {
        User user = ((MainActivity) this.getActivity()).getUser();
        phoneNo = user.getEmergency();
        String latitude = String.valueOf(newLocation.getLatitude());
        String longitude = String.valueOf(newLocation.getLongitude());
        String timestamp = String.valueOf(new Date(newLocation.getTime()));
        String speed = String.valueOf(Math.floor(primaryData.getBufferSpeed().isEmpty() ? 0 : primaryData.getBufferSpeed().recent()));
        String gforce = String.valueOf(primaryData.getBufferGForce().recent());

        Log.v(HOME, "Building message for emergency contact: " + phoneNo);
        message = "This is an automated message from BSafe to alert that " + user.getFirstName() + " " + user.getSurname() +
                " just experienced a vehicle crash. Please send an emergency response unit to (" + latitude + ", " + longitude + ") " +
                "now and pass them these details about " + user.getFirstName() + ": " + "\n" +
                "Mobile number: " + user.getMobile() + "\n" +
                "Date of birth: " + user.getDob() + "\n" +
                "Height: " + user.getHeight() + " cm\n" +
                "Weight: " + user.getWeight() + " kg\n" +
                "Smoker: " + user.getSmoker() + "\n" +
                "Bibulous: " + user.getBibulous() + "\n" +
                "Medical condition: " + user.getMedicalCondition() + "\n" +
                "Blood type: " + user.getBloodType() + "\n" +
                "Last speed: " + speed + " km/h \n" +
                "G-Force experienced: " + gforce + "\n" +
                "Time of crash: " + timestamp + "\n" +
                "Type of crash: " + crashEvent + "\n" +
                "You received this message because " + user.getFirstName() + " has listed you as an emergency contact in BSafe.";

        Log.v(HOME, "Sending message to " + phoneNo);
        LoginActivity.sql.insertAlert(new Alert(latitude, longitude, timestamp, speed, gforce)); // sending alert to database
    }

    /**
     * Method used to register Broadcast Receivers and send SMS with MSD to emergency contact
     */
    private void sendSMS(){
        getActivity().registerReceiver(sentMessage, new IntentFilter("SMS_SENT"));
        getActivity().registerReceiver(deliveredMessage, new IntentFilter("SMS_DELIVERED"));
        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> parts = smsManager.divideMessage(message);

        ArrayList<PendingIntent> sentPendingIntents = new ArrayList<>();
        ArrayList<PendingIntent> deliveredPendingIntents = new ArrayList<>();
        PendingIntent sentPI = PendingIntent.getBroadcast(getActivity(), 0, new Intent("SMS_SENT"), 0);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(getActivity(), 0, new Intent("SMS_DELIVERED"), 0);

        for(int i =0;i<parts.size();i++){
            sentPendingIntents.add(sentPI);
            deliveredPendingIntents.add(deliveredPI);
        }

        smsManager.sendMultipartTextMessage(phoneNo, null, parts, sentPendingIntents, deliveredPendingIntents); // sending alert to emergency contact
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (googleMap != null) {
            this.googleMap = googleMap;
            this.googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getActivity(), R.raw.style_json));
            this.googleMap.getUiSettings().setMyLocationButtonEnabled(false);
            this.googleMap.setIndoorEnabled(false);
            this.googleMap.setTrafficEnabled(true);
            this.googleMap.setBuildingsEnabled(false);
            Log.v(HOME, "Initiating location request..");
            requestLocation();
        }
    }

    /**
     * Method used to request user's location using Google's FusedLocationProviderClient
     */
    private void requestLocation() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(TimeUnit.SECONDS.toMillis(1));
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    /**
     * Method used to set up a CSVWriter for debugging purposes
     * Can be accessed in internal storage under the file name "crash_data.csv"
     */
    private void setupWriters(){
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
                    "Rotation",
                    "G-Force",
                    "Speed",
                    "Acceleration",
                    "dT",
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