package com.example.bsafe.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.bsafe.R;
import com.example.bsafe.ui.entry.LoginActivity;
import com.example.bsafe.utils.User;
import com.google.android.material.navigation.NavigationView;

import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    BiometricManager biometricManager;
    boolean biometricAvailable = false;
    boolean biometricSucceeded = false;
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    private MenuItem selectedItem;
    private AppBarConfiguration mAppBarConfiguration;

    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Setting up navigation drawer
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_profile, R.id.nav_previous_alerts)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        user = (User) getIntent().getExtras().getSerializable("user");

        // Setting up biometric authentication for accessing user's profile
        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                biometricSucceeded = false;
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                biometricSucceeded = true;
                NavigationUI.onNavDestinationSelected(selectedItem, navController);
                drawer.closeDrawer(GravityCompat.START);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                biometricSucceeded = false;
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle("Access your profile using biometric credentials")
                .setNegativeButtonText("Cancel")
                .build();

        navigationView.setNavigationItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();

            if(R.id.nav_profile == id && !navigationView.getMenu().findItem(R.id.nav_profile).isChecked()) {
                selectedItem = menuItem;
                if (biometricAvailable) {
                    biometricPrompt.authenticate(promptInfo); // prompting user with biometric authentication when profile is being accessed
                    return biometricSucceeded;
                }
            }

            if(id == R.id.nav_home && !navigationView.getMenu().findItem(R.id.nav_home).isChecked()){ // ensuring home screen isn't fully reloaded when pressed again
                NavigationUI.onNavDestinationSelected(menuItem, navController);
                drawer.closeDrawer(GravityCompat.START);
                return true;
            }


            if(R.id.nav_sign_out == id){ // Sending user back to Login screen when sign out is pressed
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
                return false;
            }

            NavigationUI.onNavDestinationSelected(menuItem, navController);
            drawer.closeDrawer(GravityCompat.START);
            return true;

        });

        biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate()) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                Log.d("Biometric", "App can authenticate using biometrics.");
                biometricAvailable = true;
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Log.e("Biometric", "No biometric features available on this device.");
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Log.e("Biometric", "Biometric features are currently unavailable.");
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Log.e("Biometric", "The user hasn't associated any biometric credentials with their account.");
                break;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public User getUser(){
        return this.user;
    }

}
