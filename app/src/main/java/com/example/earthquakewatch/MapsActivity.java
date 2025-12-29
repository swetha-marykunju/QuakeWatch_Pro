package com.example.earthquakewatch;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.earthquakewatch.databinding.ActivityMapsBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private static final String TAG = "QUAKE_DEBUG";
    private EarthquakeResponse.Feature selectedQuake;

    private int currentMapMode = 0;
    private int filterMode = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        PeriodicWorkRequest alertRequest =
                new PeriodicWorkRequest.Builder(QuakeWorker.class, 15, TimeUnit.MINUTES)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "QuakeAlerts",
                ExistingPeriodicWorkPolicy.KEEP,
                alertRequest
        );
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        applyMapStyle();

        requestNotificationPermission();

        applyMapStyle();

        try {
            View bottomSheetContainer = findViewById(R.id.includedBottomSheet);
            if (bottomSheetContainer != null) {
                bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetContainer);
                bottomSheetBehavior.setHideable(true);
                bottomSheetBehavior.setPeekHeight(0);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

                bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View bottomSheet, int newState) {}

                    @Override
                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                        float moveUpBy = bottomSheet.getHeight() * slideOffset;
                        binding.fabContainer.setTranslationY(-moveUpBy);
                        binding.fabContainer.setAlpha(1.0f - (slideOffset * 0.3f));
                    }
                });
            }

            binding.searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    searchLocation(query);
                    return false;
                }
                @Override public boolean onQueryTextChange(String newText) { return false; }
            });

            binding.fabFilter.setOnClickListener(v -> {
                filterMode = (filterMode + 1) % 3;
                switch (filterMode) {
                    case 0:
                        binding.fabFilter.setSupportBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
                        Toast.makeText(this, "Filter: Showing All", Toast.LENGTH_SHORT).show();
                        break;
                    case 1:
                        binding.fabFilter.setSupportBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.YELLOW));
                        Toast.makeText(this, "Filter: Mag 5.0+ Only", Toast.LENGTH_SHORT).show();
                        break;
                    case 2:
                        binding.fabFilter.setSupportBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.CYAN));
                        Toast.makeText(this, "Filter: Last 24 Hours Only", Toast.LENGTH_SHORT).show();
                        break;
                }
                fetchEarthquakeData();
            });

            binding.fabMapType.setOnClickListener(v -> {
                currentMapMode = (currentMapMode + 1) % 3;
                switch (currentMapMode) {
                    case 0:
                        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        applyMapStyle();
                        Toast.makeText(this, "Light Mode", Toast.LENGTH_SHORT).show();
                        break;
                    case 1:
                        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark));
                        Toast.makeText(this, "Dark Mode", Toast.LENGTH_SHORT).show();
                        break;
                    case 2:
                        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                        Toast.makeText(this, "Satellite Mode", Toast.LENGTH_SHORT).show();
                        break;
                }
            });

            binding.fabResetCamera.setOnClickListener(v -> {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(0, 0), 1));
                binding.searchView.setQuery("", false);
                binding.searchView.clearFocus();
            });

            binding.fabRefresh.setOnClickListener(v -> {
                binding.fabRefresh.animate().rotationBy(360).setDuration(600).start();
                fetchEarthquakeData();
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            });

            binding.includedBottomSheet.btnShare.setOnClickListener(v -> shareQuakeDetails());
            binding.includedBottomSheet.btnClose.setOnClickListener(v -> bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN));
            mMap.setOnMarkerClickListener(marker -> {
                handleMarkerClick(marker);
                return true;
            });

            fetchEarthquakeData();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void searchLocation(String query) {
        android.location.Geocoder geocoder = new android.location.Geocoder(this);
        try {
            java.util.List<android.location.Address> addressList = geocoder.getFromLocationName(query, 1);
            if (addressList != null && !addressList.isEmpty()) {
                LatLng latLng = new LatLng(addressList.get(0).getLatitude(), addressList.get(0).getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 7));
                binding.searchView.clearFocus();
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (java.io.IOException e) { e.printStackTrace(); }
    }

    private void handleMarkerClick(Marker marker) {
        EarthquakeResponse.Feature feature = (EarthquakeResponse.Feature) marker.getTag();
        if (feature != null) {
            selectedQuake = feature;
            binding.includedBottomSheet.detailMag.setText("Magnitude: " + feature.properties.mag);
            binding.includedBottomSheet.detailPlace.setText(feature.properties.place);
            binding.includedBottomSheet.detailDate.setText(new SimpleDateFormat
                    ("MMM dd, yyyy | hh:mm a", Locale.getDefault()).format(new Date(feature.properties.time)));

            int visibility = (feature.properties.tsunami == 1) ? View.VISIBLE : View.GONE;
            binding.includedBottomSheet.imgTsunami.setVisibility(visibility);
            binding.includedBottomSheet.txtTsunamiWarning.setVisibility(visibility);

            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 6));
        }
    }

    private void shareQuakeDetails() {
        if (selectedQuake != null) {
            String body = "Earthquake Alert!\nMag: " + selectedQuake.properties.mag + "\nLoc: " + selectedQuake.properties.place;
            Intent intent = new Intent(android.content.Intent.ACTION_SEND);
            intent.setType("text/plain").putExtra(Intent.EXTRA_TEXT, body);
            startActivity(Intent.createChooser(intent, "Share via"));
        }
    }

    private void applyMapStyle() {
        try {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void fetchEarthquakeData() {
        binding.loadingSpinner.setVisibility(View.VISIBLE);
        new Retrofit.Builder().baseUrl("https://earthquake.usgs.gov/").addConverterFactory(GsonConverterFactory.create()).build()
                .create(ApiService.class).getQuakes().enqueue(new Callback<EarthquakeResponse>() {
                    @Override
                    public void onResponse(Call<EarthquakeResponse> call, Response<EarthquakeResponse> response) {
                        binding.loadingSpinner.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            mMap.clear();
                            for (EarthquakeResponse.Feature f : response.body().features) setupMarker(f);
                        }
                    }
                    @Override public void onFailure(Call<EarthquakeResponse> call, Throwable t) {
                        binding.loadingSpinner.setVisibility(View.GONE);
                        Toast.makeText(MapsActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupMarker(EarthquakeResponse.Feature f) {
        double mag = f.properties.mag;
        long time = f.properties.time;
        long now = System.currentTimeMillis();

        if (filterMode == 1 && mag < 5.0) return;
        if (filterMode == 2 && (now - time) > (24 * 3600 * 1000)) return;

        try {
            LatLng location = new LatLng(f.geometry.coordinates.get(1), f.geometry.coordinates.get(0));
            float color = (mag >= 6.0) ? BitmapDescriptorFactory.HUE_RED : (mag >= 4.5) ? BitmapDescriptorFactory.HUE_ORANGE : BitmapDescriptorFactory.HUE_YELLOW;
            Marker marker = mMap.addMarker(new MarkerOptions().position(location).icon(BitmapDescriptorFactory.defaultMarker(color)));
            if (marker != null) {
                marker.setTag(f);
                if ((now - time) < (3600 * 1000)) addPulsingEffect(location);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void addPulsingEffect(LatLng location) {
        Circle pulseCircle = mMap.addCircle(new CircleOptions()
                .center(location).radius(0).strokeWidth(0).fillColor(Color.argb(100, 255, 0, 0)));
        ValueAnimator va = ValueAnimator.ofFloat(0, 100000);
        va.setRepeatCount(ValueAnimator.INFINITE);
        va.setRepeatMode(ValueAnimator.RESTART);
        va.setDuration(2000);
        va.addUpdateListener(anim -> {
            pulseCircle.setRadius((float) anim.getAnimatedValue());
            pulseCircle.setFillColor(Color.argb((int) (100 * (1 - anim.getAnimatedFraction())), 255, 0, 0));
        });
        va.start();
    }
    private void requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                androidx.core.app.ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }
}