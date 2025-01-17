package com.example.bsafe.ui.entry;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.example.bsafe.R;
import com.example.bsafe.utils.User;

public class SignupActivity extends AppCompatActivity {


    private EditText firstNameText;
    private EditText surnameText;
    private EditText birthText;
    private EditText emergencyText;
    private EditText mobileText;
    private EditText emailText;
    private EditText passwordText;

    private Button continueBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_signup);

        firstNameText = findViewById(R.id.input_firstName);
        surnameText = findViewById(R.id.input_surname);
        birthText = findViewById(R.id.input_dob);
        emergencyText = findViewById(R.id.input_emergency_contact);
        mobileText = findViewById(R.id.input_mobile_number);
        emailText = findViewById(R.id.input_email);
        passwordText = findViewById(R.id.input_password);
        continueBtn = findViewById(R.id.continue_button);

        // Continue button is used to proceed the user to continue registering their medical profile
        continueBtn.setOnClickListener(v -> {
            if(!validate()){
               Toast.makeText(getApplicationContext(), "Check your details are filled correctly.", Toast.LENGTH_LONG).show();
               return;
           }

            User newUser = new User();
            newUser.setFirstName(firstNameText.getText().toString());
            newUser.setSurname(surnameText.getText().toString());
            newUser.setDob(birthText.getText().toString());
            newUser.setMobile(mobileText.getText().toString());
            newUser.setEmergency(emergencyText.getText().toString());
            newUser.setEmail(emailText.getText().toString());
            newUser.setPassword(passwordText.getText().toString());

            SignUpMedicalFragment fragment = new SignUpMedicalFragment();
            fragment.setNewUser(newUser);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.signup, new SignUpMedicalFragment());
            transaction.addToBackStack(null);
            transaction.commit(); // launch medical profile registration
        });
    }

    /**
     * Method used to ensure none of the fields are empty
     * @return whether the fields are valid or not
     */
    private boolean validate() {
        boolean valid = true;

        String firstName = firstNameText.getText().toString();
        String surname = surnameText.getText().toString();
        String dob = birthText.getText().toString();
        String mobile = mobileText.getText().toString();
        String emergency = emergencyText.getText().toString();
        String email = emailText.getText().toString();
        String password = passwordText.getText().toString();

        if(firstName.isEmpty() || surname.isEmpty() || dob.isEmpty() || password.isEmpty() || emergency.isEmpty() || mobile.isEmpty()){
            valid = false;
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            valid = false;
        }

        return valid;
    }

}
