package com.example.bsafe.ui.entry;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.bsafe.ui.MainActivity;
import com.example.bsafe.R;
import com.example.bsafe.db.DBHelper;
import com.example.bsafe.utils.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity{

    private static final int REQUEST_SIGNUP = 0;
    private static final int PERMISSION_REQUEST = 333;

    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.SEND_SMS
    };
    public static DBHelper sql;

    private EditText emailText;
    private EditText passwordText;
    private Button loginBtn;
    private TextView signUpText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_login);

        if(checkAndRequestPermissions()){ // allow user to continue only if the permissions have been accepted
            init();
        }

    }


    private void init(){
        sql = new DBHelper(this);
        this.emailText = findViewById(R.id.input_email);
        this.passwordText = findViewById(R.id.input_password);
        this.loginBtn = findViewById(R.id.login_button);
        this.signUpText = findViewById(R.id.beginSignUp);

        loginBtn.setOnClickListener(v -> login());
        signUpText.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
            startActivityForResult(intent, REQUEST_SIGNUP);
        });
    }

    /**
     * Method called once the user presses "Login" button
     * Used to validate the entered credentials with SQLite database
     * If credentials are valid, user is sent to main screen
     * If credentials are invalid, user has to try again or signup
     */
    private void login() {

        if (!validate()) {
            onLoginFailed();
            return;
        }

        loginBtn.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this, R.style.ProgressStyle);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();

        String email = emailText.getText().toString();
        String password = passwordText.getText().toString();

        new android.os.Handler().postDelayed(
                () -> {
                    if(sql.valid(email, password)){
                        onLoginSuccess();
                    }else{
                        onLoginFailed();
                    }

                    progressDialog.dismiss();
                }, TimeUnit.SECONDS.toMillis(3));
    }

    /**
     * Method used to validate the user's input to credential fields. It is to ensure they're not empty
     * @return whether the input is valid or not
     */
    private boolean validate() {
        boolean valid = true;

        String email = emailText.getText().toString();
        String password = passwordText.getText().toString();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(getApplicationContext(), "Enter a valid email address", Toast.LENGTH_LONG).show();
            valid = false;
        }

        if (password.isEmpty()){
            Toast.makeText(getApplicationContext(), "Invalid password", Toast.LENGTH_LONG).show();
            valid = false;
        }

        return valid;
    }

    /**
     * Method used to launch main screen if the login was successful
     */
    private void onLoginSuccess() {
        Bundle b = new Bundle();
        User user = sql.getUser(emailText.getText().toString());
        b.putSerializable("user", user);
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtras(b);
        startActivity(intent);
        finish();
    }

    /**
     * Method used to notify if their credentials were incorrect upon login
     */
    private void onLoginFailed(){
        Toast.makeText(getApplicationContext(), "Login failed", Toast.LENGTH_SHORT).show();
        loginBtn.setEnabled(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SIGNUP) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), "Registration complete, please login.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /** Method used to check and request all required permissions for using the app:
     * -> Write to external storage
     * -> Send SMS
     * -> Access to location
     * @return whether the permissions have been accepted or not
     */
    private boolean checkAndRequestPermissions() {
        ArrayList<String> permissionsRequired = new ArrayList<>();
        for (String permission : PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsRequired.add(permission);
            }
        }

        if (!permissionsRequired.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsRequired.toArray(new String[permissionsRequired.size()]), PERMISSION_REQUEST);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if(requestCode == PERMISSION_REQUEST){
            Map<String, Integer> permissionResults = new HashMap<>();
            int denied = 0;

            for(int i =0; i<grantResults.length;i++){
                if(grantResults[i] == PackageManager.PERMISSION_DENIED){
                    Log.v("PERMISSION", permissions[i]);
                    permissionResults.put(permissions[i], grantResults[i]);
                    denied++;
                }
            }

            if(denied == 0){
                init();
            }else{
                for(Map.Entry<String, Integer> entry: permissionResults.entrySet()){
                    String permission = entry.getKey();
                    //if permission has been denied first time and "never ask again" is not checked
                    if(ActivityCompat.shouldShowRequestPermissionRationale(this,permission)){
                        new AlertDialog.Builder(this)
                                .setTitle("")
                                .setCancelable(true)
                                .setMessage("This app requires all permissions to work properly")
                                .setPositiveButton("Grant Permissions",
                                        (dialog, which) -> {
                                            dialog.dismiss();
                                            checkAndRequestPermissions();
                                        })
                                .setNegativeButton("Exit",
                                        (dialog, which) -> {
                                            dialog.dismiss();
                                            finish();
                                        })
                                .show();
                    }else{ // if permission has been denied and "never ask again" was checked
                        new AlertDialog.Builder(this)
                                .setTitle("")
                                .setCancelable(true)
                                .setMessage("You previously denied permissions. Please allow all permissions.")
                                .setPositiveButton("Go to Settings",
                                        (dialog, which) -> {
                                            dialog.dismiss();
                                            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:com.example.cda")));
                                            finish();
                                        })
                                .setNegativeButton("Exit",
                                        (dialog, which) -> {
                                            dialog.dismiss();
                                            finish();
                                        })
                                .show();
                        break;
                    }
                }
            }
        }
    }

}
