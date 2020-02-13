package com.example.cda;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cda.DB.DBHelper;
import com.github.nikartm.button.FitButton;

public class SignupActivity extends AppCompatActivity {

    public static DBHelper sql;

    private EditText firstNameText;
    private EditText surnameText;
    private EditText birthText;
    private EditText emailText;
    private EditText passwordText;
    private FitButton continueBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_signup);

        firstNameText = findViewById(R.id.input_firstName);
        surnameText = findViewById(R.id.input_surname);
        birthText = findViewById(R.id.input_dob);
        emailText = findViewById(R.id.input_email);
        passwordText = findViewById(R.id.input_password);
        continueBtn = findViewById(R.id.continueBtn);

        sql = new DBHelper(this);
        continueBtn.setOnClickListener(v -> {

            if(!validate()){
                Toast.makeText(getApplicationContext(), "Check your details are filled correctly.", Toast.LENGTH_LONG).show();
                return;
            }

            User newUser = new User();
            newUser.setFirstName(firstNameText.getText().toString());
            newUser.setSurname(surnameText.getText().toString());
            newUser.setDob(birthText.getText().toString());
            newUser.setEmail(emailText.getText().toString());
            newUser.setPassword(passwordText.getText().toString());

            SignUpMedicalFragment fragment = new SignUpMedicalFragment();
            fragment.setNewUser(newUser);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.framelayout, new SignUpMedicalFragment());
            transaction.addToBackStack(null);
            transaction.commit();
        });
    }

    private boolean validate() {
        boolean valid = true;

        String firstName = firstNameText.getText().toString();
        String surname = surnameText.getText().toString();
        String dob = birthText.getText().toString();
        String email = emailText.getText().toString();
        String password = passwordText.getText().toString();

        if(firstName.isEmpty() || surname.isEmpty() || dob.isEmpty() || password.isEmpty()){
            valid = false;
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            valid = false;
        }

        return valid;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_OK);
        finish();
    }
}
