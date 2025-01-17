package com.example.bsafe.ui.previous_alerts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.bsafe.R;
import com.example.bsafe.ui.entry.LoginActivity;
import com.example.bsafe.utils.Alert;

import java.util.ArrayList;

public class PreviousAlerts extends Fragment {

    private LinearLayout parentLayout;
    private View root;
    private LayoutInflater inflater;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        root = inflater.inflate(R.layout.fragment_previous_alerts, container, false);
        this.inflater = inflater;
        parentLayout =  (LinearLayout) root.findViewById(R.id.alerts_layout_parent);
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ArrayList<Alert> alerts = LoginActivity.sql.getPreviousAlerts();
        if(alerts != null) {

            // used to visualise all alerts that exist in the database
            for(Alert a: alerts) {
                View view = inflater.inflate(R.layout.previous_alerts_entry, parentLayout, false);

                LinearLayout dateLayout = (LinearLayout) view.findViewById(R.id.alerts_layout_date);
                LinearLayout infoLayout = (LinearLayout) view.findViewById(R.id.alerts_layout_info);

                dateLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(v.getId() == dateLayout.getId()){
                            if(infoLayout.getVisibility() != View.VISIBLE) {
                                infoLayout.setVisibility(View.VISIBLE);
                            }else{
                                infoLayout.setVisibility(View.GONE);
                            }
                        }
                    }
                });

                TextView dateTxt = (TextView) view.findViewById(R.id.alerts_date);
                TextView latitudeTxt = (TextView) view.findViewById(R.id.alerts_latitude);
                TextView longitudeTxt = (TextView) view.findViewById(R.id.alerts_longitude);
                TextView speedTxt = (TextView) view.findViewById(R.id.alerts_speed);
                TextView gforceTxt = (TextView) view.findViewById(R.id.alerts_gforce);

                dateTxt.setText(a.getTimestamp());
                latitudeTxt.setText(a.getLatitude());
                longitudeTxt.setText(a.getLongitude());
                speedTxt.setText(a.getSpeed() +" km/h");
                gforceTxt.setText(a.getGforce() +" G");
                parentLayout.addView(view);
            }
        }else{
            View view = inflater.inflate(R.layout.previous_alerts_entry, parentLayout, false);
            parentLayout.addView(view);
        }
    }

}
