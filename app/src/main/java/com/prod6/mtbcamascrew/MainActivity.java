package com.prod6.mtbcamascrew;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LocationListener {
    private MapView             mMapView       = null;
    private MapController       mMapController = null;

    private ArrayList<Point>    points         = null;
    public boolean permissionChecked = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            Dexter.withActivity(this)
                    .withPermissions(
                            Manifest.permission.INTERNET,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    .withListener(new MultiplePermissionsListener()
                    {
                        @Override
                        public void onPermissionsChecked(MultiplePermissionsReport report)
                        {
                            if (report.areAllPermissionsGranted())
                            {
                                permissionChecked = true;
                            } else
                            {
                                permissionChecked = false;
                            }
                        }


                        @Override
                        public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token)
                        {
                            token.continuePermissionRequest();
                        }


                    }).onSameThread().withErrorListener(new PermissionRequestErrorListener()
            {

                @Override
                public void onError(DexterError error)
                {
                    Log.e("XXXXX", error.toString());
                }
            }).check();
        } else
        {
            permissionChecked = true;
        }



        // ATTIVAZIONE GPS - INIZIO //
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, new Integer(60 * 1000), new Double( 0.00).floatValue(), this);
        } else {
            Toast.makeText(this, "Nessun accesso alla localizzazione", Toast.LENGTH_LONG).show();
        }
        // ATTIVAZIONE GPS - INIZIO //



        // TASTO AGGIORNA - INIZIO //
        ((android.widget.Button) this.findViewById(R.id.bRefresh)).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MainActivity.this.reStartService();
                        MainActivity.this.aggiornaPrimaPosizioniEPoiMappa();
                    }
                }
        );
        // TASTO AGGIORNA - FINE   //



        // SERVICE - INIZIO //
        this.reStartService();
        // SERVICE - FINE   //



        // MAPPA - INIZIO //
        this.mMapView = (MapView) findViewById(R.id.mapview);
        this.mMapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        this.mMapView.setBuiltInZoomControls(true);
        this.mMapView.setTileSource(TileSourceFactory.CYCLEMAP);
        this.mMapController = (MapController) this.mMapView.getController();
        this.mMapController.setZoom(13);
        GeoPoint gPt = new GeoPoint(51500000, -150000);
        this.mMapController.setCenter(gPt);
        // MAPPA - FINE   //
    }


    @Override
    protected void onResume()
    {
        super.onResume();
        if (!permissionChecked)
            return;


    }

    private void reStartService() {
        Intent i = new Intent(this.getApplicationContext(), PositionService.class);
        i.putExtra("message", ((TextView) findViewById(R.id.ptMessage)).getText().toString());
        this.getApplicationContext().startService(i);
    }

    private void aggiornaSoloMappa() {
        GeoPoint gPt = null;
        Marker   m   = null;

        this.mMapView.getOverlays().clear();

        for (Point point : this.points) {
            gPt = new GeoPoint(point.getLat(), point.getLon());
            m   = new Marker(this.mMapView);

            m.setTitle(point.getMessage());
            m.setIcon(this.getResources().getDrawable(R.drawable.marker));
            m.setPosition(gPt);
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

            this.mMapView.getOverlays().add(m);
        }

        Location location = this.getLocation();
        if (location != null) {
            gPt = new GeoPoint(location.getLatitude(), location.getLongitude());
            this.mMapController.setCenter(gPt);

            m = new Marker(this.mMapView);
            m.setTitle("hello world");
            m.setIcon(this.getResources().getDrawable(R.drawable.marker));
            m.setPosition(gPt);
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

            // this.mMapView.getOverlays().add(m);
        }
    }

    public void aggiornaPrimaPosizioniEPoiMappa() {
        // Toast.makeText(this, "DEBUG: refreshWayPointsFromServiceToDB", Toast.LENGTH_LONG).show();

        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://mtbcamascrew.prod6.com/service/positions.php";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            // Toast.makeText(MainActivity.this, "Risposta servizio json: " + response, Toast.LENGTH_LONG).show();

                            MainActivity.this.points    = new ArrayList<Point>();
                            Point      point            = null;
                            JSONObject jsReponse        = new JSONObject(response);
                            JSONArray  jsWaypoints      = jsReponse.getJSONArray("waypoints");

                            for (int i = 0; i < jsWaypoints.length(); i++) {
                                JSONObject jsWaypoint = (JSONObject) jsWaypoints.get(i);
                                point                   = new Point();

                                point.setId(                        jsWaypoint.getString("id"));
                                point.setLat(    Double.parseDouble(jsWaypoint.getString("lat")));
                                point.setLon(    Double.parseDouble(jsWaypoint.getString("lon")));
                                point.setMessage(                   jsWaypoint.getString("message"));

                                MainActivity.this.points.add(point);
                            }
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "201901191257: " + e.toString(), Toast.LENGTH_LONG).show();
                        }

                        MainActivity.this.aggiornaSoloMappa();;
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Errore nella connessione al sito", Toast.LENGTH_LONG).show();
            }
        });

        queue.add(stringRequest);
    }

    public Location getLocation() {
        Location location = null;

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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

    // METODI LOCATION LISTENER - INIZIO //
    @Override
    public void onLocationChanged(Location location) {
        try {
            this.aggiornaPrimaPosizioniEPoiMappa();
            // this.aggiornaTesti();
        } catch (Exception ex) {
            // Toast.makeText(this, "DEBUG: 527", Toast.LENGTH_LONG).show();
            Toast.makeText(this, ex.toString(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
    // METODI LOCATION LISTENER - FINE   //
}
