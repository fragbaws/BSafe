package com.example.cda.entry;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cda.MainActivity;
import com.example.cda.R;
import com.github.nikartm.button.FitButton;

public class LoginActivity extends AppCompatActivity{

    private static final int REQUEST_SIGNUP = 0;

    private EditText emailText;
    private EditText passwordText;
    private FitButton loginBtn;
    private TextView signUpText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_login);

        this.emailText = findViewById(R.id.input_email);
        this.passwordText = findViewById(R.id.input_password);
        this.loginBtn = findViewById(R.id.login);
        this.signUpText = findViewById(R.id.beginSignUp);

        loginBtn.setOnClickListener(v -> login());
        signUpText.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
            startActivityForResult(intent, REQUEST_SIGNUP);
        });
    }

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
                   /* if(SignupActivity.sql.valid(email, password)){
                        onLoginSuccess();
                    }else{
                        onLoginFailed();
                    }*/
                    if(email.equals("root@root.com") && password.equals("123")) {
                        onLoginSuccess();
                    }else {
                        onLoginFailed();
                    }
                    progressDialog.dismiss();
                }, 3000);
    }

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

    private void onLoginSuccess() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void onLoginFailed(){
        Toast.makeText(getApplicationContext(), "Login failed", Toast.LENGTH_SHORT).show();
        loginBtn.setEnabled(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SIGNUP) {
            if (resultCode == RESULT_OK) {
                //Toast.makeText(getApplicationContext(), "Registration complete, please login.", Toast.LENGTH_LONG).show();
            }
        }
    }

}
