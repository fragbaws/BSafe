package com.example.cda.ui.home;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static android.content.Context.SENSOR_SERVICE;
import static android.os.Environment.getExternalStorageDirectory;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private static final DecimalFormat df = new DecimalFormat("#.###");
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

    private static final int REQUEST_LOCATION_PERMISSIONS = 1;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 2;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 3;
    private static final int REQUEST_SEND_SMS = 4;
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.SEND_SMS
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
            if(speedSSD.size() == 3000){ // 30 seconds
                ssd = calcSSD(speedSSD);
                if(ssdWriter != null){
                    ssdWriter.writeNext(new String[]{String.valueOf(ssd), DateFormat.getDateTimeInstance().format(new Date())});
                }
                speedSSD.clear();
            }else {
                Log.d("SSD", "Current speed: " + speed);
                speedSSD.add(speed);
            }
            handlerSSD.postDelayed(this, 100); // set same as location request interval and other hardware sensors
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
                Log.v("Location", newLocation.toString());

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
                animateMarker(marker, newLocation);
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(newLocation.getLatitude(), newLocation.getLongitude()), CLOSE_ZOOM));

                if(oldLocation != null) {
                    //oldSpeed = speed;
                    Log.d("Location", "Time: " + oldLocation.getTime() + " " + newLocation.getTime());
                    Log.d("Location", "Distance: " + oldLocation.distanceTo(newLocation));
                    speed = calcSpeed(oldLocation, newLocation);

                }
                if(running){
                    if(((int)speed*MS2KMH) <= 24) {
                        speedTxt.setText("<24 km/h");
                    }else{
                        speedTxt.setText(String.format("%s km/h", df.format(((int) (speed * MS2KMH)))));
                    }
                }
                Log.d("Location", "Current speed is " + ((int)speed*MS2KMH) +" km/h");
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
                Log.d("Accelerometer", "G-Force: " + max_g);
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

                    Log.d("Gyroscope", "Rotation: " + degrees);

                }
                timestamp_gyro = event.timestamp;
            }
            if(sensorWriter!= null){
                String triggeredEvent = "";
                if(((gforce/G_FORCE_THRESHOLD) + (db/SOUND_PRESSURE_LEVEL_THRESHOLD)) >= ACCIDENT_THRESHOLD && (speed>=VEHICLE_SPEED_THRESHOLD)){ // travelling and hit
                    crash= true;
                    triggeredEvent = "1";
                    Log.d("CDA", "Crash occured: " + "("+ newLocation.getLatitude() + "," +newLocation.getLongitude() +")" +
                            "\n Speed: " + speed + "km/h" +
                            "\n Rotation: " + rotation + "°" +
                            "\n dB: " + db + "dB");
                }else if(((gforce/G_FORCE_THRESHOLD) + (db/SOUND_PRESSURE_LEVEL_THRESHOLD)) >= ACCIDENT_THRESHOLD && (rotation>=ROTATION_THRESHOLD)) { // hit and vehicle overturned (rotation utilises pitch & roll)
                    crash = true;
                    triggeredEvent = "2";
                    Log.d("CDA", "Crash occured: " + "("+ newLocation.getLatitude() + "," +newLocation.getLongitude() +")" +
                            "\n Speed: " + speed + "km/h" +
                            "\n Rotation: " + rotation + "°" +
                            "\n dB: " + db + "dB");
                }else if((((gforce/G_FORCE_THRESHOLD) + (db/SOUND_PRESSURE_LEVEL_THRESHOLD) + (ssd/STANDARD_DEVIATION_THRESHOLD)) >= LOW_SPEED_ACCIDENT_THRESHOLD)){ // hit while slowly moving
                    crash = true;
                    triggeredEvent = "3";
                    Log.d("CDA", "Crash occured: " + "("+ newLocation.getLatitude() + "," +newLocation.getLongitude() +")" +
                            "\n Speed: " + speed + "km/h" +
                            "\n Rotation: " + rotation + "°" +
                            "\n dB: " + db + "dB");
                }else{
                    crash = false;
                }
                sensorWriter.writeNext(new String[]{String.valueOf(rotation), String.valueOf(gforce), String.valueOf(((int)speed*MS2KMH)), String.valueOf(db), String.valueOf(crash), triggeredEvent, DateFormat.getDateTimeInstance().format(new Date())});
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

        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Log.d("Storage", "Writing permission already granted");
        } else {
            ActivityCompat.requestPermissions(getActivity(), PERMISSIONS, REQUEST_WRITE_EXTERNAL_STORAGE);
        }

        if(ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.RECORD_AUDIO)){
            Log.d("AUDIO", "Recording audio already permitted");
        }else{
            ActivityCompat.requestPermissions(getActivity(), PERMISSIONS, REQUEST_RECORD_AUDIO_PERMISSION);
        }

        audioName = getActivity().getExternalCacheDir().getAbsolutePath() + "/audiorecordtest.3gp";
        speedTxt = root.findViewById(R.id.speed_text);
        statusTxt = root.findViewById(R.id.status_text);
        monitorButton = root.findViewById(R.id.monitor_button);

        speedSSD = new ArrayList<>();
        handlerSSD = new Handler();
        monitorButton.setTag("ready");

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getActivity());
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.d("Location", "Could not load map fragment");
        }

        monitorButton.setOnClickListener(v -> {
            if(v.getId() == monitorButton.getId()){
                if(monitorButton.getTag().equals("ready")) {
                    running = true;
                    manager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
                    manager.registerListener(sensorListener, manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_GAME);
                    manager.registerListener(sensorListener, manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME);
                    startRecording();
                    handlerSSD.postDelayed(threadSSD, 0);
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
            Log.d("Microphone", "prepare() failed");
        }

        recorder.start();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                db = getDB();
                handler.postDelayed(this, 20);
            }
        }, 20);
    }

    private double getDB(){
        if(recorder != null){
            float maxAmplitude = recorder.getMaxAmplitude();
            Log.d("Microphone","Amplitude: " + maxAmplitude);
            float db = (float) (20 * Math.log10(maxAmplitude));// / 32767.0f));
            Log.d("Microphone","Current DB: " + db);
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
        speedTxt.setText("N/A");
        monitorButton.setText("Start Monitoring");
        monitorButton.setTag("ready");
        statusTxt.setText("Off");

        String baseDir = getExternalStorageDirectory().getAbsolutePath();
        String fileName = "crash_data.csv";
        String filePath = baseDir + File.separator + fileName;
        Log.d("Storage", "Writing to " + filePath);
        File f = new File(filePath);
        FileWriter mFileWriter;

        String ssdname = "ssd.csv";
        String ssdPath = baseDir + File.separator + ssdname;
        Log.d("Storage", "Writing to " + ssdPath);
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

    public void stop(){
        running = false;
        speedTxt.setText("N/A");
        monitorButton.setText("Start Monitoring");
        monitorButton.setTag("ready");
        statusTxt.setText("Off");
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

    public void SOS(){
        stop();
        Log.d("SOS", "Crash");
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
                            Log.d("SOS", "Sending message to emergency contact");
                            sendAlert();
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
        Log.i("SOS", "Building message for " + phoneNo);
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


        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.SEND_SMS)) {
            } else {
                ActivityCompat.requestPermissions(getActivity(), PERMISSIONS, REQUEST_SEND_SMS);
            }
        }else{
            Log.d("SOS", "Sending message to " + phoneNo);
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> parts = smsManager.divideMessage(message);

            smsManager.sendMultipartTextMessage(phoneNo, null, parts, null, null);
            Toast.makeText(getContext(), "Alert sent to emergency contact", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getActivity(), R.raw.style_json));
        requestLocation();
    }

    public void animateMarker(final Marker marker, final Location location) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final LatLng startLatLng = marker.getPosition();
        final double startRotation = marker.getRotation();
        final long duration = 500;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);

                double lng = t * location.getLongitude() + (1 - t)
                        * startLatLng.longitude;
                double lat = t * location.getLatitude() + (1 - t)
                        * startLatLng.latitude;

                float rotation = (float) (t * location.getBearing() + (1 - t)
                        * startRotation);

                marker.setPosition(new LatLng(lat, lng));
                marker.setRotation(rotation);

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                }
            }
        });
    }

    private void requestLocation() {
        locationRequest = new LocationRequest();
        Log.d("Location", "Requesting location");
        locationRequest.setFastestInterval(100);
        locationRequest.setSmallestDisplacement(5);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(false);
            googleMap.setIndoorEnabled(false);
            googleMap.setTrafficEnabled(true);
            googleMap.setBuildingsEnabled(false);
            monitorButton.setTag("stop");
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
                        Log.d("Location", "Permission granted");
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
                        googleMap.setMyLocationEnabled(true);
                    }
                } else {
                    Log.d("Location", "Permission denied");
                }
            }
            case REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Storage", "Permission Granted, you can use local drive.");
                } else {
                    Log.d("Storage", "Permission Denied, You cannot use local drive.");
                }
            }
            case REQUEST_RECORD_AUDIO_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("AUDIO", "Recording audio permitted");
                } else {
                    Log.d("AUDIO", "Recording audio denied");
                }
            }
            case REQUEST_SEND_SMS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("SMS", "Sending SMS permitted");
                    Log.d("SOS", "Sending message to " + phoneNo);
                    SmsManager smsManager = SmsManager.getDefault();
                    ArrayList<String> parts = smsManager.divideMessage(message);
                    smsManager.sendMultipartTextMessage(phoneNo, null, parts, null, null);
                    Toast.makeText(getContext(), "Alert sent to emergency contact.", Toast.LENGTH_LONG).show();
                } else {
                    Log.d("SMS", "Sending SMS permission denied.");
                }
            }
        }
    }

}