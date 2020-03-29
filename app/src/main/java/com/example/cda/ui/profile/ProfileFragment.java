package com.example.cda.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.cda.ui.MainActivity;
import com.example.cda.R;
import com.example.cda.utils.User;

public class ProfileFragment extends Fragment {

    private TextView nameTxt, mobileNumberTxt, emailTxt, emergencyContactTxt, weightTxt, heightTxt,
                    dobTxt, bloodTypeTxt, smokerTxt, bibulousTxt, medicalConditionTxt;

    private TextView personalDetailsBtn, medicalProfileBtn;
    private LinearLayout personalDetails, medicalProfile;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_profile, container, false);
        User user = ((MainActivity)this.getActivity()).getUser();

        nameTxt = root.findViewById(R.id.profile_name);
        mobileNumberTxt = root.findViewById(R.id.profile_mobile_number);
        emailTxt = root.findViewById(R.id.profile_email);
        emergencyContactTxt = root.findViewById(R.id.profile_emergency_contact);
        weightTxt = root.findViewById(R.id.profile_weight);
        heightTxt = root.findViewById(R.id.profile_height);
        dobTxt = root.findViewById(R.id.profile_dob);
        bloodTypeTxt = root.findViewById(R.id.profile_blood_type);
        smokerTxt = root.findViewById(R.id.profile_smoker);
        bibulousTxt = root.findViewById(R.id.profile_bibulous);
        medicalConditionTxt = root.findViewById(R.id.profile_medical_condition);

        // Setup UI with details of signed-in user
        nameTxt.setText(user.getFirstName() + " " + user.getSurname());
        mobileNumberTxt.setText(user.getMobile());
        emailTxt.setText(user.getEmail());
        emergencyContactTxt.setText(user.getEmergency());
        weightTxt.setText(user.getWeight() + " kg");
        heightTxt.setText(user.getHeight() + " cm");
        dobTxt.setText(user.getDob());
        bloodTypeTxt.setText(user.getBloodType());
        smokerTxt.setText(user.getSmoker());
        bibulousTxt.setText(user.getBibulous());
        medicalConditionTxt.setText(user.getMedicalCondition());

        personalDetailsBtn = root.findViewById(R.id.profile_personal_details_button);
        medicalProfileBtn = root.findViewById(R.id.profile_medical_button);

        personalDetails = root.findViewById(R.id.profile_personal_details);
        medicalProfile = root.findViewById(R.id.profile_medical_profile);

        personalDetailsBtn.setOnClickListener(v -> { // switch to personal profile when clicked
                personalDetails.setVisibility(View.VISIBLE);
                medicalProfile.setVisibility(View.GONE);

                personalDetailsBtn.setTextColor(getResources().getColor(R.color.colorPrimary));
                medicalProfileBtn.setTextColor(getResources().getColor(R.color.silver));
        });

        medicalProfileBtn.setOnClickListener(v -> { // switch to medical profile when clicked
            personalDetails.setVisibility(View.GONE);
            medicalProfile.setVisibility(View.VISIBLE);

            personalDetailsBtn.setTextColor(getResources().getColor(R.color.silver));
            medicalProfileBtn.setTextColor(getResources().getColor(R.color.colorPrimary));
        });

        return root;
    }
}