package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RouteDetailedActivity displays a detailed view of a route on a map,
 * including the path, estimated time, and incident markers.
 */
public class RouteDetailedActivity extends AppCompatActivity implements OnMapReadyCallback {
    /** ViewModel that handles the business logic for the route details screen. */
    private RouteDetailedViewModel viewModel;

    /** MainViewModel used to create the RouteDetailedViewModel. */
    private MainViewModel mainViewModel;

    /** GoogleMap object representing the map displayed in this activity. */
    private GoogleMap mMap;

    /** LatLng object representing the starting point of the route. */
    private LatLng sourceLatLng;

    /** LatLng object representing the destination point of the route. */
    private LatLng destinationLatLng;

    /** Button that allows the user to navigate back to the previous screen. */
    private Button backButton;

    /** List of Marker objects representing incident markers on the map. */
    private List<Marker> incidentMarkers = new ArrayList<>();

    /** Marker object representing the estimated time marker on the map. */
    private Marker timeMarker;

    /**
     * Initializes the activity, sets up the layout, and prepares the map.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_detailed);

        initializeViewModels();
        getIntentExtras();
        initializeMap();
        setupBackButton();
        setupObservers();
    }

    /**
     * Initializes the ViewModels used in this activity.
     * Creates an instance of RouteDetailedViewModel using the MainViewModel's factory method.
     */
    private void initializeViewModels() {
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @Override
            public <T extends androidx.lifecycle.ViewModel> T create(Class<T> modelClass) {
                return (T) mainViewModel.createRouteDetailedViewModel();
            }
        }).get(RouteDetailedViewModel.class);
    }

    /**
     * Retrieves the source and destination coordinates from the Intent extras.
     */
    private void getIntentExtras() {
        sourceLatLng = new LatLng(
                getIntent().getDoubleExtra("sourceLatitude", 0),
                getIntent().getDoubleExtra("sourceLongitude", 0)
        );
        destinationLatLng = new LatLng(
                getIntent().getDoubleExtra("destinationLatitude", 0),
                getIntent().getDoubleExtra("destinationLongitude", 0)
        );
    }

    /**
     * Initializes the Google Map fragment and sets up the callback for when the map is ready.
     */
    private void initializeMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Sets up the back button to finish the activity when clicked.
     */
    private void setupBackButton() {
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }

    /**
     * Sets up observers for the ViewModel's LiveData objects.
     * This includes route path, estimated time, error messages, and incident data.
     */
    private void setupObservers() {
        viewModel.getRoutePath().observe(this, this::drawPolyline);
        viewModel.getEstimatedTime().observe(this, this::showEstimatedTime);
        viewModel.getErrorMessage().observe(this, message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
        viewModel.getIncidentData().observe(this, this::addIncidentMarkers);
    }

    /**
     * Callback method invoked when the Google Map is ready to be used.
     * Adds source and destination markers, calculates directions, and loads incident data.
     *
     * @param googleMap The GoogleMap object representing the loaded map.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        addSourceAndDestinationMarkers();
        viewModel.calculateDirections(sourceLatLng, destinationLatLng);
        viewModel.loadIncidentData();
    }

    /**
     * Adds markers for the source and destination points on the map.
     */
    private void addSourceAndDestinationMarkers() {
        mMap.addMarker(new MarkerOptions().position(sourceLatLng).title("Source")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Destination")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
    }

    /**
     * Draws the route polyline on the map and positions the camera to show the entire route.
     *
     * @param path List of LatLng points representing the route.
     */
    private void drawPolyline(List<LatLng> path) {
        PolylineOptions opts = new PolylineOptions().addAll(path).color(Color.BLUE).width(5);
        mMap.addPolyline(opts);
        positionCamera(path);
    }

    /**
     * Positions the camera to show the entire route.
     *
     * @param path List of LatLng points representing the route.
     */
    private void positionCamera(List<LatLng> path) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng latLng : path) {
            builder.include(latLng);
        }
        LatLngBounds bounds = builder.build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    /**
     * Shows the estimated time for the route by adding a marker at the midpoint of the route.
     *
     * @param estimatedTime String representation of the estimated time.
     */
    private void showEstimatedTime(String estimatedTime) {
        List<LatLng> path = viewModel.getRoutePath().getValue();
        if (path != null && !path.isEmpty()) {
            LatLng midPoint = path.get(path.size() / 2);
            if (timeMarker != null) {
                timeMarker.remove();
            }
            timeMarker = mMap.addMarker(new MarkerOptions()
                    .position(midPoint)
                    .title("Estimated Time")
                    .snippet(estimatedTime)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            timeMarker.showInfoWindow();
        }
    }

    /**
     * Adds incident markers to the map based on the provided incident data.
     *
     * @param incidentData List of incidents to be displayed on the map.
     */
    private void addIncidentMarkers(List<Map<String, Object>> incidentData) {
        clearExistingMarkers();
        for (Map<String, Object> incident : incidentData) {
            Map<String, Object> point = (Map<String, Object>) incident.get("point");
            if (point != null) {
                double latitude = ((Number) point.get("latitude")).doubleValue();
                double longitude = ((Number) point.get("longitude")).doubleValue();
                LatLng incidentLocation = new LatLng(latitude, longitude);
                String shortDescription = (String) incident.get("shortDescription");
                String incidentTypeDescription = (String) incident.get("incidentTypeDescription");
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(incidentLocation)
                        .title(shortDescription)
                        .snippet(incidentTypeDescription));
                incidentMarkers.add(marker);
            }
        }
    }

    /**
     * Clears existing incident markers from the map.
     */
    private void clearExistingMarkers() {
        for (Marker marker : incidentMarkers) {
            marker.remove();
        }
        incidentMarkers.clear();
    }
}