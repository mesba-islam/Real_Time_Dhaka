package com.example.cse_499;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.model.Image;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.Marker;
import com.jaredrummler.materialspinner.MaterialSpinner;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static com.example.cse_499.Utils.UPLOAD_IMAGE_RESPONSE;

public class MainActivity extends AppCompatActivity implements NetworkCallback.onImageResponseReceived,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private ImageView imageView;

    public static double latitude;
    public static double longitude;
    private int PROXIMITY_RADIUS = 3000;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    LocationRequest mLocationRequest;

    String category = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkLocationPermission();
        setContentView(R.layout.activity_main);
        findViewById(R.id.capture_image_button_home).setOnClickListener(v -> {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dailog_category);
            MaterialSpinner spinner = (MaterialSpinner) dialog.findViewById(R.id.spinner);
            spinner.setItems("Accident", "Crime", "Garbage");
            spinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {

                @Override
                public void onItemSelected(MaterialSpinner view, int position, long id, String item) {

                    category = item;
                }
            });


            dialog.findViewById(R.id.upload).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();

                    openCamera();

                }
            });


            dialog.show();

        });
        imageView = findViewById(R.id.image_view_uploaded_image);

        buildGoogleApiClient();


        findViewById(R.id.open_map).setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));

    }


    public void openCamera() {
        ImagePicker.cameraOnly().start(MainActivity.this);
    }

    @Override
    public void onReceived(int code, UploadFileResponse response) {
        if (code == UPLOAD_IMAGE_RESPONSE) {

            Retrofit retrofit = NetworkDataClient.getRetrofitClient(this);
            DataAPI uploadAPIs = retrofit.create(DataAPI.class);

            DataModel requestBody = new DataModel();
            if (response != null) {
                requestBody.imageLink = response.getFileDownloadUri();
            }
            requestBody.latitude = latitude;
            requestBody.longitude = longitude;
            requestBody.category = category;
            requestBody.time = System.currentTimeMillis();

            Call<DataModel> dataModelCall = uploadAPIs.saveData(requestBody);

            dataModelCall.enqueue(new Callback<DataModel>() {
                @Override
                public void onResponse(Call<DataModel> call, Response<DataModel> response) {

                    Toast.makeText(MainActivity.this, "Submitted", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(Call<DataModel> call, Throwable t) {
                    Toast.makeText(MainActivity.this, t.getMessage() + "", Toast.LENGTH_SHORT).show();

                }
            });

            category = "";
        }

    }

    @Override
    protected void onActivityResult(int requestCode, final int resultCode, Intent data) {
        if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {
            List<Image> images = ImagePicker.getImages(data);
            Image image = ImagePicker.getFirstImageOrNull(data);

            uploadToServer(image.getPath());

        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    private void uploadToServer(String filePath) {
        Retrofit retrofit = NetworkClient.getRetrofitClient(this);

        UploadApis uploadAPIs = retrofit.create(UploadApis.class);
        File file = new File(filePath);


        RequestBody fileReqBody = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", System.currentTimeMillis() + "_CSE499_" + file.getName(), fileReqBody);
        RequestBody description = RequestBody.create(MediaType.parse("text/plain"), "image-type");


        Call<UploadFileResponse> call = uploadAPIs.uploadImage(part, description);


        ProgressDialog progressDialog = new ProgressDialog(this);

        progressDialog.setTitle("Image uploading");
        progressDialog.setMessage("Please wait...");
        progressDialog.show();
        call.enqueue(new Callback<UploadFileResponse>() {
            @Override
            public void onResponse(Call<UploadFileResponse> call, Response<UploadFileResponse> response) {
                onsucupload(response, progressDialog);
                progressDialog.dismiss();

            }

            @Override
            public void onFailure(Call<UploadFileResponse> call, Throwable t) {
                onReceived(UPLOAD_IMAGE_RESPONSE, new UploadFileResponse("", "", "", 5));
                progressDialog.dismiss();
                // onerrorup(t,progressDialog);
            }
        });
    }

    private void onerrorup(Throwable t, ProgressDialog progressDialog) {
        Log.e("onFailure: ", t.getMessage());
        progressDialog.dismiss();
        Toast.makeText(MainActivity.this, "Upload Failed", Toast.LENGTH_SHORT).show();
    }

    private void onsucupload(Response<UploadFileResponse> response, ProgressDialog progressDialog) {
        UploadFileResponse body = response.body();
        progressDialog.dismiss();
        Toast.makeText(MainActivity.this, "Upload Successful", Toast.LENGTH_SHORT).show();
        onReceived(UPLOAD_IMAGE_RESPONSE, body);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
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
                    }

                } else {

                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
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
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

        latitude = location.getLatitude();
        longitude = location.getLongitude();
    }

}
