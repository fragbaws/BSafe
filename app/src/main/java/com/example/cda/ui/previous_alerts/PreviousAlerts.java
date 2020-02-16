package com.example.cda.ui.previous_alerts;

import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.cda.R;

public class PreviousAlerts extends Fragment {

    private PreviousAlertsViewModel mViewModel;

    public static PreviousAlerts newInstance() {
        return new PreviousAlerts();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_previous_alerts, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(PreviousAlertsViewModel.class);
        // TODO: Use the ViewModel
    }

}
