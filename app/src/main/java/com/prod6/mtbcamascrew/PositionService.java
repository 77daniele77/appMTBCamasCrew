package com.prod6.mtbcamascrew;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

public class PositionService extends Service {
    private Double              sessionID      = Math.random();
    private Handler             mHandler       = null;
    private Runnable            mHandlerTask   = null;
    private Intent              intent         = null;

    private void doTimer() {
        Toast.makeText(this, "Invio posizione", Toast.LENGTH_LONG).show();

        RequestQueue queue       = Volley.newRequestQueue(this);
        String        url         = "http://mtbcamascrew.prod6.com/service/myPosition.php";
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // response
                        // Toast.makeText(MainActivity.this, "Risposta servizio: " + response, Toast.LENGTH_LONG).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        // Log.d("Error.Response", response);
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                Location location = PositionService.this.getLocation();

                if (location != null) {
                    params.put("id",      String.valueOf(PositionService.this.sessionID));
                    params.put("lat",     String.valueOf(location.getLatitude()));
                    params.put("lon",     String.valueOf(location.getLongitude()));
                    // params.put("message", ((TextView) findViewById(R.id.ptMessage)).getText().toString());
                    params.put("message", PositionService.this.intent.getExtras().getString("message"));
                }

                return params;
            }
        };

        queue.add(postRequest);
    }

    public Location getLocation() {
        Location location = null;

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            if (locationManager != null) {
                if (locationManager.isProviderEnabled("gps")) {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                } else {
                    Toast.makeText(this, "GPS spento", Toast.LENGTH_LONG).show();
                }
            }
        }

        return location;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.intent = intent;

        // TIMER - INIZIO //
        this.mHandler = new Handler();

        this.mHandlerTask = new Runnable() {
            @Override
            public void run() {
                PositionService.this.doTimer();
                PositionService.this.mHandler.postDelayed(PositionService.this.mHandlerTask, 60 * 1000);
            }
        };

        this.mHandlerTask.run();
        // TIMER - FINE   //

        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        //TODO for communication return IBinder implementation
        return null;
    }
}
