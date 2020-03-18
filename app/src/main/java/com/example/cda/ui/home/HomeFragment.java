package com.example.cda.ui.home;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
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
import androidx.fragment.app.Fragment;

import com.example.cda.MainActivity;
import com.example.cda.R;
import com.example.cda.data.PrimaryData;
import com.example.cda.entry.User;
import com.example.cda.utils.Calculator;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static android.content.Context.SENSOR_SERVICE;
import static android.os.Environment.getExternalStorageDirectory;
import static com.example.cda.utils.Constants.AUTO_DISMISS_MILLIS;
import static com.example.cda.utils.Constants.BUFFER_SIZE;
import static com.example.cda.utils.Constants.CAR_HEIGHT;
import static com.example.cda.utils.Constants.CAR_WIDTH;
import static com.example.cda.utils.Constants.CLOSE_ZOOM;
import static com.example.cda.utils.Constants.COUNT_DOWN_INTERVAL_MILLIS;
import static com.example.cda.utils.Constants.LOCATION_INTERVAL_SECS;
import static com.example.cda.utils.Constants.MICROPHONE_INTERVAL_SECS;
import static com.example.cda.utils.Constants.MS2KMH;
import static com.example.cda.utils.Constants.SECS2MS;
import static com.example.cda.utils.Constants.VEHICLE_SPEED_THRESHOLD;

public class HomeFragment extends Fragment implements OnMapReadyCallback {
    
    private static final String TAG = "HOME";
    private static final String THREAD = "THREAD";

    private static final Calculator calculator = Calculator.getInstance();
    private PrimaryData primaryData;

    private ArrayList<Double> bufferRotation;
    private ArrayList<Double> bufferGForce;
    private ArrayList<Double> bufferDecibel;

    private Handler calculateRunningAverageThreadHandler;
    private Runnable calculateRunningAverageThread = new Runnable() {
        @Override
        public void run() {

            double averageRotation, averageGForce, averageDecibel;
            if(!bufferRotation.isEmpty()){
                averageRotation = calculator.calculateAverage(bufferRotation);
                primaryData.getBufferRotation().add(averageRotation);
                bufferRotation.clear();
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

            Log.v(THREAD, "Rotation" + primaryData.getBufferRotation());
            Log.v(THREAD, "GForce" + primaryData.getBufferGForce());
            Log.v(THREAD, "Decibels" + primaryData.getBufferDecibels());
            Log.v(THREAD, "Speed" + primaryData.getBufferSpeed());

            calculateRunningAverageThreadHandler.postDelayed(this, (long) (LOCATION_INTERVAL_SECS*SECS2MS));
        }
    };

    private Handler calculateRateOfChangeThreadHandler;
    private Runnable calculateRateOfChangeThread = new Runnable() {
        @Override
        public void run() {
            //double[] rotationPair = primaryData.getBufferRotation().getRecentPair(); Exists where resulting rotation is calculate (omega rad/s)
            double[] gForcePair = null, decibelPair = null, speedPair = null;
            if(primaryData.getBufferSpeed().size() >= 2) {
                speedPair = primaryData.getBufferSpeed().getRecentPair();
            }
            if(primaryData.getBufferGForce().size() >= 2) {
                gForcePair = primaryData.getBufferGForce().getRecentPair();
            }
            if(primaryData.getBufferDecibels().size() >= 2) {
                decibelPair = primaryData.getBufferDecibels().getRecentPair();
            }

            //double rotationROC, 
            double jerk, decibelROC, speedROC;
            /*if(rotationPair != null){
                rotationROC = calculator.calculateRateOfChange(rotationPair[1], rotationPair[0], )
            }*/
            
            if(gForcePair != null){
                jerk = calculator.calculateRateOfChange(gForcePair[1], gForcePair[0]);
                primaryData.getBufferGForceROC().add(jerk);
            }

            if(decibelPair != null){
                decibelROC = calculator.calculateRateOfChange(decibelPair[1], decibelPair[0]);
                primaryData.getBufferDecibelROC().add(decibelROC);
            }

            if(speedPair != null){
                speedROC = calculator.calculateRateOfChange(speedPair[1]/MS2KMH, speedPair[0]/MS2KMH); // converting to m/s as speed rate of change = acceleration
                primaryData.getBufferSpeedROC().add(speedROC);
            }


            //Log.v(THREAD, "Rotation ROC" + Arrays.toString(primaryData.getBufferRotation().getRecentPair()));
            Log.v(THREAD, "GForce ROC" + primaryData.getBufferGForceROC());
            Log.v(THREAD, "Decibels ROC" + primaryData.getBufferDecibelROC());
            Log.v(THREAD, "Speed ROC" + primaryData.getBufferSpeedROC());

            calculateRateOfChangeThreadHandler.postDelayed(this, (long) (LOCATION_INTERVAL_SECS*SECS2MS));
        }
    };

    private SupportMapFragment mapFragment;
    private GoogleMap googleMap;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Marker marker;
    private Location oldLocation;
    private Location newLocation;

    private SensorManager manager;
    private float prevTimestamp;

    /*private ArrayList<Integer> speedValues;
    private double speedDeviation = 0; //speed standard deviation
    private Handler collectSpeedDataThreadHandler;
    private Runnable collectSpeedDataThread = new Runnable() {
        public void run()
        {
            speedValues.add(primaryData.getCurrentSpeed());
            ssdWriter.writeNext(new String[]{"-", "-", String.valueOf((System.currentTimeMillis()/SECS2MS)), primaryData.getCurrentSpeed() + "km/h"});
            collectSpeedDataThreadHandler.postDelayed(this, (long) (LOCATION_INTERVAL_SECS*SECS2MS)); // set same as location request interval
        }
    };
    private Handler calculateSpeedDeviationThreadHandler;
    private Runnable calculateSpeedDeviationThread = new Runnable() {
        public void run()
        {
            speedDeviation = calculator.calculateSpeedDeviation(speedValues);
            if(ssdWriter != null){
                ssdWriter.writeNext(new String[]{String.valueOf(speedDeviation), DateFormat.getDateTimeInstance().format(new Date()), String.valueOf((System.currentTimeMillis()/SECS2MS))});
                speedValues.clear();
            }

            calculateSpeedDeviationThreadHandler.postDelayed(this, (long) (LOCATION_INTERVAL_SECS*SECS2MS*15)); // calculate speed deviation every 15 seconds
        }
    };*/

    private MediaRecorder recorder;
    private String audioName;

    private Button monitorButton;
    private TextView speedTxt;
    private TextView statusTxt;
    private CSVWriter sensorWriter;
    private CSVWriter ssdWriter;

    private String phoneNo, message;

    private boolean running = false;
    private boolean crash = false;


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

            if(oldLocation != null) {
                //oldSpeed = speed;
                primaryData.setCurrentSpeed(calculator.calculateCurrentSpeed(oldLocation, newLocation));
            }
            if(running){
                if(primaryData.getCurrentSpeed() <= VEHICLE_SPEED_THRESHOLD) {
                    speedTxt.setText("<24 km/h");
                }else{
                    speedTxt.setText(String.format("%s km/h", (int) primaryData.getCurrentSpeed()));
                }
            }
        }
        }
    };

    private SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
                primaryData.setCurrentGForce(calculator.calculateGForce(event.values[0], event.values[1], event.values[2]));
                bufferGForce.add(primaryData.getCurrentGForce());
            }
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                if(prevTimestamp!=0) {
                    primaryData.setCurrentRotation(calculator.calculateResultingRotation(event.values[0],
                            event.values[1], event.values[2], prevTimestamp, event.timestamp));
                    bufferRotation.add(primaryData.getCurrentRotation());

                }
                prevTimestamp = event.timestamp;
            }
            if(sensorWriter!= null){
               /* String triggeredEvent = "";
                if(((primaryData.getCurrentGForce()/G_FORCE_THRESHOLD) + (primaryData.getCurrentDecibels()/SOUND_PRESSURE_LEVEL_THRESHOLD)) >= ACCIDENT_THRESHOLD && (primaryData.getCurrentSpeed()>=VEHICLE_SPEED_THRESHOLD)){ // travelling and hit
                    crash= true;
                    triggeredEvent = "1";
                    Log.v(TAG, "Crash occured: " + "("+ newLocation.getLatitude() + "," +newLocation.getLongitude() +")" +
                            "\n Speed: " + primaryData.getCurrentSpeed() + "km/h" +
                            "\n Rotation: " + primaryData.getCurrentRotation() + "°" +
                            "\n dB: " + primaryData.getCurrentDecibels() + "dB");
                }else if(((primaryData.getCurrentGForce()/G_FORCE_THRESHOLD) + (primaryData.getCurrentDecibels()/SOUND_PRESSURE_LEVEL_THRESHOLD)) >= ACCIDENT_THRESHOLD && (primaryData.getCurrentRotation()>=ROTATION_THRESHOLD)) { // hit and vehicle overturned (rotation utilises pitch & roll)
                    crash = true;
                    triggeredEvent = "2";
                    Log.v(TAG, "Crash occured: " + "("+ newLocation.getLatitude() + "," +newLocation.getLongitude() +")" +
                            "\n Speed: " + primaryData.getCurrentSpeed() + "km/h" +
                            "\n Rotation: " + primaryData.getCurrentRotation() + "°" +
                            "\n dB: " + primaryData.getCurrentDecibels() + "dB");
                }else if( (primaryData.getCurrentSpeed() < VEHICLE_SPEED_THRESHOLD) && (((primaryData.getCurrentGForce()/G_FORCE_THRESHOLD) + (primaryData.getCurrentDecibels()/SOUND_PRESSURE_LEVEL_THRESHOLD) + (speedDeviation/STANDARD_DEVIATION_THRESHOLD)) >= LOW_SPEED_ACCIDENT_THRESHOLD)){ // hit while slowly moving
                    crash = true;
                    triggeredEvent = "3";
                    Log.v(TAG, "Crash occured: " + "("+ newLocation.getLatitude() + "," +newLocation.getLongitude() +")" +
                            "\n Speed: " + primaryData.getCurrentSpeed() + "km/h" +
                            "\n Rotation: " + primaryData.getCurrentRotation() + "°" +
                            "\n dB: " + primaryData.getCurrentDecibels() + "dB");
                }else{
                    crash = false;
                }
                sensorWriter.writeNext(new String[]{String.valueOf(primaryData.getCurrentRotation()), String.valueOf(primaryData.getCurrentGForce()), String.valueOf(primaryData.getCurrentSpeed()), String.valueOf(primaryData.getCurrentDecibels()), String.valueOf(crash), triggeredEvent, DateFormat.getDateTimeInstance().format(new Date()), String.valueOf(System.currentTimeMillis()/SECS2MS)});
                */if(crash){
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
        speedTxt = root.findViewById(R.id.speed_text);
        statusTxt = root.findViewById(R.id.status_text);
        monitorButton = root.findViewById(R.id.monitor_button);
        monitorButton.setTag("ready");

        primaryData = new PrimaryData(BUFFER_SIZE);
        bufferDecibel = new ArrayList<>();
        bufferGForce = new ArrayList<>();
        bufferRotation = new ArrayList<>();
        calculateRunningAverageThreadHandler = new Handler();
        calculateRateOfChangeThreadHandler = new Handler();


  /*      speedValues = new ArrayList<>();
        collectSpeedDataThreadHandler = new Handler();
        calculateSpeedDeviationThreadHandler = new Handler();*/


        monitorButton.setOnClickListener(v -> {
            if(v.getId() == monitorButton.getId()){
                if(monitorButton.getTag().equals("ready")) {
                    Log.v(TAG, "Activating crash detection algorithm...");
                    running = true;
                    setupWriters();

                    sensorWriter.writeNext(new String[]{"Rotation", "G-Force", "Speed", "dB", "Crash", "Case", "Timestamp", "Milliseconds"});
                    ssdWriter.writeNext(new String[]{"Standard Deviation", "Timestamp", "Milliseconds"});

                    manager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
                    manager.registerListener(sensorListener, manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_GAME);
                    manager.registerListener(sensorListener, manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME);
                    startRecording();
                    calculateRateOfChangeThreadHandler.postDelayed(calculateRateOfChangeThread, (long) (LOCATION_INTERVAL_SECS*SECS2MS*2));
                    calculateRunningAverageThreadHandler.postDelayed(calculateRunningAverageThread, 0);

/*                    Log.v(TAG, "Activating threads for speed standard deviation calculations...");
                    collectSpeedDataThreadHandler.postDelayed(collectSpeedDataThread, 0);
                    calculateSpeedDeviationThreadHandler.postDelayed(calculateSpeedDeviationThread, 30000);*/
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
            Log.v(TAG, "Recording failed");
        }

        recorder.start();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                primaryData.setCurrentDecibels(calculator.calculateDecibels(recorder));
                bufferDecibel.add(primaryData.getCurrentDecibels());
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
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.v(TAG, "Could not load map fragment");
        }
        if(ssdWriter == null || sensorWriter == null) {
            setupWriters();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stop();
    }

    private void stop(){
        running = false;
        speedTxt.setText("N/A");
        //speedDeviation = 0;
        monitorButton.setText("Start Monitoring");
        monitorButton.setTag("ready");
        statusTxt.setText("Off");
        if(calculateRunningAverageThreadHandler.hasCallbacks(calculateRunningAverageThread)){
            calculateRunningAverageThreadHandler.removeCallbacks(calculateRunningAverageThread);
        }
        if(calculateRateOfChangeThreadHandler.hasCallbacks(calculateRateOfChangeThread)){
            calculateRateOfChangeThreadHandler.removeCallbacks(calculateRateOfChangeThread);
        }
        if(primaryData != null){
            primaryData.clearBuffers();
        }
        /*if(speedValues != null) {
            speedValues.clear();
        }
        if(collectSpeedDataThreadHandler.hasCallbacks(collectSpeedDataThread)){
            collectSpeedDataThreadHandler.removeCallbacks(collectSpeedDataThread);
        }
        if(calculateSpeedDeviationThreadHandler.hasCallbacks(calculateSpeedDeviationThread)){
            calculateSpeedDeviationThreadHandler.removeCallbacks(collectSpeedDataThread);
        }*/
        /*if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }*/
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
                        "Last speed: " + (int) primaryData.getCurrentSpeed() + " km/h \n" +
                        "G-Force experienced: " + primaryData.getCurrentGForce() + "\n\n" +
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
            Log.v(TAG,e.getMessage());
        }
    }

}