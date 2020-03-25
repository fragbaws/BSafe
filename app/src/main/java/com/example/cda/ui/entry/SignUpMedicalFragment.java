package com.example.cda.ui.entry;

import androidx.lifecycle.ViewModelProviders;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.cda.R;
import com.example.cda.utils.User;
import com.jaredrummler.materialspinner.MaterialSpinner;

public class SignUpMedicalFragment extends Fragment {

    private SignUpMedicalViewModel mViewModel;

    private static User newUser;
    private EditText weightText;
    private EditText heightText;
    private MaterialSpinner bloodType;
    private MaterialSpinner smoker;
    private MaterialSpinner bibulous;
    private MaterialSpinner condition;
    private Button signupBtn;

    public static SignUpMedicalFragment newInstance() {
        return new SignUpMedicalFragment();
    }

    static void setNewUser(User u){
        newUser = u;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.sign_up_medical_fragment, container, false);

        weightText = root.findViewById(R.id.input_weight);
        heightText = root.findViewById(R.id.input_height);
        bloodType = root.findViewById(R.id.input_bloodtype);
        bloodType.setItems("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-");
        smoker = root.findViewById(R.id.input_smoker);
        smoker.setItems("Yes", "No");
        bibulous = root.findViewById(R.id.input_bibulous);
        bibulous.setItems("Yes", "No");
        condition = root.findViewById(R.id.input_condition);
        condition.setItems("Arthritis", "Hypertension", "Asthma", "Blindness", "Cancer", "Coronary Heart Disease", "Dementia", "Diabetes", "Epilepsy", "Multiple Sclerosis", "Osteoporosis", "None");

        signupBtn = root.findViewById(R.id.signup_button);
        signupBtn.setOnClickListener(v -> {
            if(!validate()){
                Toast.makeText(getContext(),"Check your details before finishing registration", Toast.LENGTH_LONG).show();
                return;
            }

            newUser.setWeight(weightText.getText().toString());
            newUser.setHeight(heightText.getText().toString());
            newUser.setBloodType((String) bloodType.getItems().get(bloodType.getSelectedIndex()));
            newUser.setSmoker((String) smoker.getItems().get(smoker.getSelectedIndex()));
            newUser.setBibulous((String) bibulous.getItems().get(bibulous.getSelectedIndex()));
            newUser.setMedicalCondition((String) condition.getItems().get(condition.getSelectedIndex()));

            signupBtn.setEnabled(false);

            final ProgressDialog progressDialog = new ProgressDialog(getContext(), R.style.ProgressStyle);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Just a moment...");
            progressDialog.show();

            new android.os.Handler().postDelayed(
                    () -> {
                        LoginActivity.sql.insertUser(newUser);
                        Toast.makeText(getContext(), "Registration complete, please login.", Toast.LENGTH_LONG).show();
                        getActivity().setResult(Activity.RESULT_OK);
                        getActivity().finish();
                        progressDialog.dismiss();
                    }, 1000);
        });

        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(SignUpMedicalViewModel.class);
        // TODO: Use the ViewModel
    }

    private boolean validate() {
        boolean valid = true;

        String weight = weightText.getText().toString();
        String height = heightText.getText().toString();
        String blood = (String) bloodType.getItems().get(bloodType.getSelectedIndex());
        String smoke = (String) smoker.getItems().get(smoker.getSelectedIndex());
        String drinker = (String) bibulous.getItems().get(bibulous.getSelectedIndex());
        String medical = (String) condition.getItems().get(condition.getSelectedIndex());

        if(weight.isEmpty() || height.isEmpty() || blood.isEmpty() || smoke.isEmpty() || drinker.isEmpty() || medical.isEmpty()){
            valid = false;
        }

        return valid;
    }

}
