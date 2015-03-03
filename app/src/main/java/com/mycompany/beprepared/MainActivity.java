 //@Author Matthew Bonilla
 //@email mbonill@unm.edu

package com.mycompany.beprepared;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;
import android.location.*;
import android.widget.EditText;

public class MainActivity extends ActionBarActivity {

    private final double meter = 0.000009029926;//about one meter in gps coordinate
    private boolean isTracking;
    private double northLat, eastLong, southLat, westLong, bestLong, bestLat;
    private float bestAcc, accuracy;
    private SharedPreferences data;
    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isTracking = false;
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        data = this.getSharedPreferences("data", Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = data.edit();
        final EditText curLat = (EditText) findViewById(R.id.currentLat);
        final EditText curLon = (EditText) findViewById(R.id.currentLong);
        final EditText track = (EditText) findViewById(R.id.tracking);
        final ProgressDialog progress = new ProgressDialog(this);
        fillValues();
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                double latitude = lastKnownLocation.getLatitude();
                double longitude = lastKnownLocation.getLongitude();
                float accuracy = lastKnownLocation.getAccuracy();
                curLat.setText("Latitude: " + Double.toString(latitude));
                curLon.setText("Longitude: " + Double.toString(longitude));
                track.setText("not tracking");
                if (isTracking) { // calculate and display if out of bounds
                    track.setText(" tracking within " + Float.toString(accuracy) + "m: in bounds");
                    if (latitude - (accuracy * meter) > northLat)
                        track.setText("out of bounds North");
                    else if (latitude + (accuracy * meter) < southLat)
                        track.setText("out of bounds South");
                    else if (longitude - (accuracy * meter) > eastLong)
                        track.setText("out of bounds East");
                    else if (longitude + (accuracy * meter) < westLong)
                        track.setText("out of bounds West");
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);


        //Use the current location and store the updated latitude line if a better accuracy is found
        Button northLocation = (Button) findViewById(R.id.northButton);
        northLocation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                calculateLocation();
                if (bestAcc < data.getFloat("northAcc", Float.MAX_VALUE)) {
                    editor.putString("northLat", Double.toString(bestLat + bestAcc * meter));
                    editor.putFloat("northAcc", bestAcc);
                    editor.commit();
                    fillValues();
                }
            }
        });


        Button eastLocation = (Button) findViewById(R.id.eastButton);
        //Use the current location and store the updated longitude line if a better accuracy is found
        eastLocation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                calculateLocation();
                if (bestAcc < data.getFloat("eastAcc", Float.MAX_VALUE)) {
                    editor.putString("eastLong", Double.toString(bestLong + bestAcc * meter));
                    editor.putFloat("eastAcc", bestAcc);
                    editor.commit();
                    fillValues();
                }
            }
        });


        Button southLocation = (Button) findViewById(R.id.southButton);
        //Use the current location and store the updated latitude line if a better accuracy is found
        southLocation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                calculateLocation();
                if (bestAcc < data.getFloat("southAcc", Float.MAX_VALUE)) {
                    editor.putString("southLat", Double.toString(bestLat - bestAcc * meter));
                    editor.putFloat("southAcc", bestAcc);
                    editor.commit();
                    fillValues();
                }
            }
        });


        Button westLocation = (Button) findViewById(R.id.westButton);
        //Use the current location and store the updated longitude line if a better accuracy is found
        westLocation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                calculateLocation();
                if (bestAcc < data.getFloat("westAcc", Float.MAX_VALUE)) {
                    editor.putString("westLong", Double.toString(bestLong - bestAcc * meter));
                    editor.putFloat("westAcc", bestAcc);
                    editor.commit();
                    fillValues();
                }
            }
        });


        Button tracking = (Button) findViewById(R.id.track);
        //reinitialize bounded values and turn on tracking
        tracking.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                northLat = Double.parseDouble(data.getString("northLat", "err"));
                eastLong = Double.parseDouble(data.getString("eastLong", "err"));
                southLat = Double.parseDouble(data.getString("southLat", "err"));
                westLong = Double.parseDouble(data.getString("westLong", "err"));
                isTracking = true;
            }
        });

        Button stop = (Button) findViewById(R.id.stop);
        //turn off tracking
        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                isTracking = false;
            }
        });
    }

    //fill in EditText views with appropriate values
    private void fillValues() {
        EditText text = (EditText) findViewById(R.id.maxNorth);
        EditText text2 = (EditText) findViewById(R.id.maxEast);
        EditText text3 = (EditText) findViewById(R.id.maxSouth);
        EditText text4 = (EditText) findViewById(R.id.maxWest);
        text.setText(data.getString("northLat", "") + " " + Float.toString(data.getFloat("northAcc", 0.0f)));
        text2.setText(data.getString("eastLong", "") + " " + Float.toString(data.getFloat("eastAcc", 0.0f)));
        text3.setText(data.getString("southLat", "") + " " + Float.toString(data.getFloat("southAcc", 0.0f)));
        text4.setText(data.getString("westLong", "") + " " + Float.toString(data.getFloat("westAcc", 0.0f)));
    }

    //use a three second window to try and calculate an accurate location to use for bounds
    private void calculateLocation() {
        bestLong = 0.0;
        bestLat = 0.0;
        bestAcc = Float.MAX_VALUE;
        accuracy = 0.0f;
        double latitude = 0.0;
        double longitude = 0.0;
        for (int i = 0; i < 3; i++) {
            try {
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                latitude = lastKnownLocation.getLatitude();
                longitude = lastKnownLocation.getLongitude();
                accuracy = lastKnownLocation.getAccuracy();
                if (accuracy < bestAcc) {
                    bestLat = latitude;
                    bestLong = longitude;
                    bestAcc = accuracy;
                }
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
