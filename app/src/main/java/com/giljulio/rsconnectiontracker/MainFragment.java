package com.giljulio.rsconnectiontracker;

import android.app.ActivityManager;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Created by Gil on 03/11/14.
 */
public class MainFragment extends Fragment {

    @InjectView(R.id.submit) Button submit;
    @InjectView(R.id.username) EditText username;

    public MainFragment(){

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.inject(this, rootView);

        return rootView;
    }

    @OnClick(R.id.submit) void submit() {
        Intent intent = new Intent(getActivity(), TrackerService.class);
        if(!isMyServiceRunning(TrackerService.class)){
            intent.putExtra(TrackerService.KEY_USERNAME, username.getText().toString());
            getActivity().startService(intent);
        } else {
            getActivity().stopService(intent);
        }
        updateUI();
    }


    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI(){
        boolean running = isMyServiceRunning(TrackerService.class);
        username.setEnabled(!running);
        if(running){
            submit.setText("Stop Notifying Me");
        } else {
            submit.setText("Notify Me");
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


}
