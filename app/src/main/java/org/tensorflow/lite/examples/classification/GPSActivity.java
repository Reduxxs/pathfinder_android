package org.tensorflow.lite.examples.classification;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.view.Menu;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class GPSActivity extends FragmentActivity implements OnMapReadyCallback {

    TextView tvlat, tvlong, tvadd;
    Button getLoc, shareLoc;
    FusedLocationProviderClient fusedLocationProviderClient;

    private GoogleMap mMap;
    boolean locationPermission = false;
    Location myLocation = null;
    Location myUpdatedLocation = null;
    float Bearing = 0;
    boolean AnimationStatus = false;
    static Marker carMarker;
    Bitmap BitMapMarker;

    TextToSpeech tts;

    private final static int LOCATION_REQUEST_CODE = 23;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gpsactivity);

        tvlat = findViewById(R.id.lat);
        tvlong = findViewById(R.id.longi);
        tvadd = findViewById(R.id.address);
        getLoc = findViewById(R.id.getloc);
        shareLoc = findViewById(R.id.shareLoc);

        if (!isConnected(this)) {
            showCustomDialog();
        }

        @SuppressLint("UseCompatLoadingForDrawables")
        BitmapDrawable bitmapdraw = (BitmapDrawable) getResources().getDrawable(R.drawable.person_gps_icon);
        Bitmap b = bitmapdraw.getBitmap();
        BitMapMarker = Bitmap.createScaledBitmap(b, 110, 60, false);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        getLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*if (ActivityCompat.checkSelfPermission(GPSActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    showLocation();
                 */
                if (ActivityCompat.checkSelfPermission(GPSActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(GPSActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LOCATION_REQUEST_CODE);
                } else {
                    showLocation();
                    LocationstatusCheck();
                    locationPermission = true;
                    //init google map fragment to show map.
                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.map);
                    mapFragment.getMapAsync(GPSActivity.this);
                }
            }
        });

        shareLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                String Body = "See my real-time location on Maps: " + tvadd.getText().toString();
                intent.putExtra(Intent.EXTRA_TEXT, Body);
                startActivity(Intent.createChooser(intent, "Share using"));

            }
        });
    }

    private boolean isConnected(GPSActivity gpsActivity) {
        ConnectivityManager connectivityManager = (ConnectivityManager) gpsActivity.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifiConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileConn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if ((wifiConn != null && wifiConn.isConnected()) || (mobileConn != null && mobileConn.isConnected())) {
            return true;
        } else {
            return false;
        }
    }

    private void LocationstatusCheck() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }
    }

    private void buildAlertMessageNoGps() {
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final android.app.AlertDialog alert = builder.create();
        alert.show();
    }

    private void showCustomDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(GPSActivity.this);
        builder.setMessage("Please check internet connection to proceed!")
                .setCancelable(false)
                .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS));
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(GPSActivity.this, MenuActivity.class));
                        finish();
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }

    private void showLocation() {
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                Location location = task.getResult();
                if (location != null) {
                    try {
                        Geocoder geocoder = new Geocoder(GPSActivity.this, Locale.getDefault());

                        List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude()
                                , 1);

                        tvlat.setText(Html.fromHtml("<font color='#6200E'><b></b></font>" +
                                addresses.get(0).getLatitude()));

                        tvlong.setText(Html.fromHtml("<font color='#6200E'><b></b></font>" +
                                addresses.get(0).getLongitude()));

                        tvadd.setText(Html.fromHtml("<font color='#6200E'><b></b></font>" +
                                addresses.get(0).getAddressLine(0)));

                        //tts.speak("You are here at " + tvadd + addresses.get(0).getAddressLine(0), TextToSpeech.QUEUE_FLUSH, null);

                        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                            @Override
                            public void onInit(int status) {
                                if (status == TextToSpeech.SUCCESS) {
                                    int lang = tts.setLanguage(Locale.ENGLISH);

                                    String s = "You are here at" + tvadd.getText().toString();

                                    int speech = tts.speak(s, TextToSpeech.QUEUE_FLUSH, null);
                                }
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showLocation();
                    LocationstatusCheck();
                    //if permission granted.
                    locationPermission = true;
                    //init google map fragment to show map.
                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.map);
                    mapFragment.getMapAsync(this);
                    // getMyLocation();

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        getMyLocation();

    }

    private void getMyLocation() {
        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {

                if (AnimationStatus) {
                    myUpdatedLocation = location;
                } else {
                    myLocation = location;
                    myUpdatedLocation = location;
                    LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
                    carMarker = mMap.addMarker(new MarkerOptions().position(latlng).
                            flat(true).icon(BitmapDescriptorFactory.fromBitmap(BitMapMarker)));
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(
                            latlng, 17f);
                    mMap.animateCamera(cameraUpdate);
                }
                Bearing = location.getBearing();
                LatLng updatedLatLng = new LatLng(myUpdatedLocation.getLatitude(), myUpdatedLocation.getLongitude());
                changePositionSmoothly(carMarker, updatedLatLng, Bearing);

            }
        });
    }

    void changePositionSmoothly(Marker carMarker, LatLng updatedLatLng, float bearing) {
        final LatLng startPosition = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
        final LatLng finalPosition = updatedLatLng;
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final Interpolator interpolator = new AccelerateDecelerateInterpolator();
        final float durationInMs = 1000;
        final boolean hideMarker = false;

        handler.post(new Runnable() {
            long elapsed;
            float t;
            float v;

            @Override
            public void run() {
                carMarker.setRotation(bearing);
                // Calculate progress using interpolator
                elapsed = SystemClock.uptimeMillis() - start;
                t = elapsed / durationInMs;
                v = interpolator.getInterpolation(t);

                LatLng currentPosition = new LatLng(
                        startPosition.latitude * (1 - t) + finalPosition.latitude * t,
                        startPosition.longitude * (1 - t) + finalPosition.longitude * t);

                carMarker.setPosition(currentPosition);

                // Repeat till progress is complete.
                if (t < 1) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        carMarker.setVisible(false);
                    } else {
                        carMarker.setVisible(true);
                    }
                }
                myLocation.setLatitude(updatedLatLng.latitude);
                myLocation.setLongitude(updatedLatLng.longitude);
            }
        });
    }
}