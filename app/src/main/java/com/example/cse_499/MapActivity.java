package com.example.cse_499;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;
import com.squareup.picasso.Picasso;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


public class MapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, GoogleMap.OnMarkerClickListener {
    private Polyline currentPolyline;

    private GoogleMap mMap;
    public static double latitude;
    public static double longitude;
    private int PROXIMITY_RADIUS = 3000;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    private NetworkClient service;
    private HeatmapTileProvider provider;
    private ArrayList<WeightedLatLng> data;
    private TileOverlay tileOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        String url = "https://maps.googleapis.com/maps/";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        if (!isGooglePlayServicesAvailable()) {
            Log.d("onCreate", "Google Play Services not available. Ending Test case.");
            finish();
        } else {
            Log.d("onCreate", "Google Play Services available. Continuing.");
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(100000);
        mLocationRequest.setFastestInterval(100000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {

        Log.d("onLocationChanged", "entered");

        mLastLocation = location;

        latitude = location.getLatitude();
        longitude = location.getLongitude();
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(8));

        Log.d("onLocationChanged", String.format("latitude:%.3f longitude:%.3f", latitude, longitude));

        Log.d("onLocationChanged", "Exit");
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);

            }
        } else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }
        getDataFromServer();


        int[] colors = {
                Color.rgb(102, 225, 0), // green
                Color.rgb(255, 0, 0)
        };
        float[] startPoints = {
                0.2f, .6f
        };
        Gradient gradient = new Gradient(colors, startPoints);
        mMap.setOnMarkerClickListener(this);


        data = new ArrayList<>();


        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                double intensity = (Math.random() * 100) % 30;


                Log.e("Intensity", intensity + "");
                data.add(new WeightedLatLng(latLng, intensity));

                if (tileOverlay != null) {
                    tileOverlay.clearTileCache();
                }


                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));

                if (provider == null)
                    provider = new HeatmapTileProvider.Builder()
                            .weightedData(data)
                            .radius(50)
                            .opacity(.7)
                            .build();
                else {
                    provider.setWeightedData(data);
                }

                tileOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
            }
        });

    }


    public void getDataFromServer() {

        Retrofit retrofit = NetworkDataClient.getRetrofitClient(this);
        DataAPI uploadAPIs = retrofit.create(DataAPI.class);


        Call<ArrayList<DataModel>> data = uploadAPIs.getData();
        data.enqueue(new Callback<ArrayList<DataModel>>() {
            @Override
            public void onResponse(Call<ArrayList<DataModel>> call, Response<ArrayList<DataModel>> response) {
                setDataIntoMap(response.body());
            }

            @Override
            public void onFailure(Call<ArrayList<DataModel>> call, Throwable t) {


                Log.e("", "onFailure:");
            }
        });


    }




    public void setDataIntoMap(ArrayList<DataModel> response) {

        if (response != null) {
            mMap.clear();
            for (DataModel item : response) {
                LatLng latLng = new LatLng(item.latitude, item.longitude);
                double intensity = 0.0;

                if (item.category.equalsIgnoreCase("Accident"))
                    intensity = (Math.random() * 100) % 30;
                else if (item.category.equalsIgnoreCase("Crime")) {
                    intensity = (Math.random() * 100) % 50;

                } else {
                    intensity = (Math.random() * 100) % 20;
                }


                Log.e("Intensity", intensity + "");
                data.add(new WeightedLatLng(latLng, intensity));

                if (tileOverlay != null) {
                    tileOverlay.clearTileCache();
                }


                Marker marker = mMap.addMarker(
                        new MarkerOptions()
                                .position(latLng)
                                .title(item.category)
                                .icon(BitmapDescriptorFactory
                                        .defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));


                marker.setTag(item);
                // mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));

                if (provider == null)
                    provider = new HeatmapTileProvider.Builder()
                            .weightedData(data)
                            .radius(50)
                            .opacity(.7)
                            .build();
                else {
                    provider.setWeightedData(data);
                }

            }


            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {

                    Dialog dialog = new Dialog(MapActivity.this);
                    dialog.setContentView(R.layout.dailog_image_preview);

                    ImageView image = dialog.findViewById(R.id.preview);
                    TextView title_preview = dialog.findViewById(R.id.title_preview);

                    DataModel dataModel = (DataModel) marker.getTag();

                    if (dataModel != null) {

                        if (!dataModel.imageLink.isEmpty()) {
                            Picasso.get().load(dataModel.imageLink).into(image);
                            title_preview.setText(dataModel.category);

                        }

                    }

                    dialog.show();

                    return false;
                }
            });
            tileOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));

        }

    }


    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result,
                        0).show();
            }
            return false;
        }
        return true;
    }


    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {


                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {


        Log.e("Faild Hoise", connectionResult.getErrorMessage());
    }

    private double distance(double lat1, double lon1, double lat2, double lon2, char unit) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        if (unit == 'K') {
            dist = dist * 1.609344;
        }
        return (dist);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {


        Object tag = marker.getTag();

        String title = marker.getTitle();

        Log.e("", "" + tag);
        return false;
    }
}
