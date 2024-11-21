package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.navigation.NavigationView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MainActivity is the primary activity for the application.
 * It handles the main user interface, including the map and navigation controls.
 */
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    /**
     * Tag for logging purposes. Used to identify log messages from this class.
     */
    private static final String TAG = "MainActivity";

    /**
     * Request code for fine location permission.
     */
    private static final int FINE_PERMISSION_CODE = 1;

    /**
     * Request code for reporting traffic incidents.
     */
    private static final int REPORT_REQUEST_CODE = 1001;

    /**
     * Constant representing 3 miles in meters, used for nearby traffic queries.
     */
    private static final double THREE_MILES_IN_METERS = 2414;

    /**
     * ViewModel that handles the business logic for the main screen.
     */
    private MainViewModel viewModel;

    /**
     * GoogleMap instance representing the map displayed in the activity.
     */
    private GoogleMap myMap;

    /**
     * FusedLocationProviderClient used for getting the device's location.
     */
    private FusedLocationProviderClient fusedLocationProviderClient;

    /**
     * List of Marker objects representing traffic incidents on the map.
     */
    private List<Marker> trafficMarkers = new ArrayList<>();

    /**
     * Marker representing the user's current location on the map.
     */
    private Marker currentLocationMarker;

    /**
     * Marker representing the user's selected destination on the map.
     */
    private Marker destinationMarker;

    /**
     * Button that, when clicked, centers the map on the user's current location.
     */
    private Button myLocationButton;

    /**
     * Button that, when clicked, starts navigation to the selected destination.
     */
    private Button navigateButton;

    /**
     * Button that, when clicked, allows the user to report a traffic incident.
     */
    private Button reportButton;

    /**
     * Button for handling user login/logout.
     */
    private Button loginButton;

    /**
     * Button that, when clicked, shows nearby traffic incidents.
     */
    private Button nearbyTrafficButton;

    /**
     * SearchView allowing users to search for locations on the map.
     */
    private SearchView mapSearchView;

    /**
     * TextView displaying user details (e.g., email) in the navigation drawer.
     */
    private TextView userDetailsTextView;

    /**
     * DrawerLayout for the navigation drawer.
     */
    private DrawerLayout drawerLayout;

    /**
     * NavigationView containing navigation drawer items.
     */
    private NavigationView navigationView;

    /**
     * Request code for location permission.
     */
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    /**
     * Called when the activity is first created. This is where you should do all of your normal static set up:
     * create views, bind data to lists, etc. This method also provides you with a Bundle containing the activity's
     * previously frozen state, if there was one.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     *                           Otherwise it is null.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        initializeComponents();
        setupLocationServices();
        setupListeners();
        setupObservers();
        setupNavigationDrawer();

        if (isNetworkAvailable()) {
            viewModel.fetchTrafficData();
            viewModel.fetchUserReports();
        } else {
            Toast.makeText(this, "No network connection available", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Initializes UI components and map fragment.
     */
    private void initializeComponents() {
        myLocationButton = findViewById(R.id.myLocationButton);
        navigateButton = findViewById(R.id.navigationButton);
        reportButton = findViewById(R.id.report_traffic);
        nearbyTrafficButton = findViewById(R.id.nearbyTrafficButton);
        mapSearchView = findViewById(R.id.mapSearch);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    /**
     * Sets up location services.
     */
    private void setupLocationServices() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            getLastLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Sets up the navigation drawer.
     */
    private void setupNavigationDrawer() {
        View headerView = navigationView.getHeaderView(0);
        loginButton = headerView.findViewById(R.id.Login);
        userDetailsTextView = headerView.findViewById(R.id.user_details);

        loginButton.setOnClickListener(view -> handleLoginLogout());
        userDetailsTextView.setOnClickListener(view -> showUserProfile());
    }

    /**
     * Shows the user profile or prompts login if not logged in.
     */
    private void showUserProfile() {
        if (viewModel.getIsUserLoggedIn().getValue()) {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        } else {
            Toast.makeText(this, "Please log in to view your profile", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Sets up click listeners for various UI components.
     */
    private void setupListeners() {
        myLocationButton.setOnClickListener(v -> moveToCurrentLocation());
        navigateButton.setOnClickListener(v -> startNavigation());
        reportButton.setOnClickListener(v -> reportTraffic());
        nearbyTrafficButton.setOnClickListener(v -> showNearbyTraffic());
        mapSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchLocation(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    /**
     * Sets up observers for various LiveData objects in the ViewModel.
     */
    private void setupObservers() {
        viewModel.getCurrentLocation().observe(this, this::updateCurrentLocationMarker);
        viewModel.getDestinationLatLng().observe(this, this::updateDestinationMarker);
        viewModel.getTrafficData().observe(this, this::updateTrafficMarkers);
        viewModel.getUserPreferences().observe(this, preferences -> {
            // Preferences have changed, no need to do anything here as the ViewModel will handle filtering
        });
        viewModel.getIsUserLoggedIn().observe(this, this::updateUIForUser);
        viewModel.getUserEmail().observe(this, this::updateUserEmail);
        viewModel.getErrorMessage().observe(this, this::showErrorMessage);
        viewModel.getUserReports().observe(this, this::updateUserReportMarkers);
    }

    /**
     * Shows an error message to the user.
     * @param error The error message to display
     */
    private void showErrorMessage(String error) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }

    /**
     * Updates markers for user-reported traffic issues.
     *
     * @param reports List of user reports.
     */
    private void updateUserReportMarkers(List<Map<String, Object>> reports) {
        if (reports == null) {
            Log.w(TAG, "updateUserReportMarkers: reports list is null");
            return;
        }

        for (Map<String, Object> report : reports) {
            try {
                String incidentType = (String) report.get("type");
                String title = (String) report.get("title");
                String snippet = (String) report.get("snippet");

                Double latitude = getDoubleFromObject(report.get("latitude"));
                Double longitude = getDoubleFromObject(report.get("longitude"));

                if (latitude == null || longitude == null) {
                    Log.w(TAG, "Invalid location data in report: " + report);
                    continue;
                }

                addReportMarker(incidentType, title, snippet, latitude, longitude);
            } catch (Exception e) {
                Log.e(TAG, "Error processing report: " + report, e);
            }
        }
    }

    /**
     * Converts an object to a Double value.
     * @param obj The object to convert
     * @return The Double value, or null if conversion fails
     */
    private Double getDoubleFromObject(Object obj) {
        if (obj instanceof Double) {
            return (Double) obj;
        } else if (obj instanceof Long) {
            return ((Long) obj).doubleValue();
        } else if (obj instanceof String) {
            try {
                return Double.parseDouble((String) obj);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing double from string: " + obj, e);
            }
        }
        return null;
    }

    /**
     * Retrieves the last known location of the device.
     */
    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            viewModel.setCurrentLocation(location);
                            if (myMap != null) {
                                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                            }
                        }
                    });
        }
    }


    /**
     * Moves the map camera to the current location.
     */
    private void moveToCurrentLocation() {
        Location location = viewModel.getCurrentLocation().getValue();
        if (location != null && myMap != null) {
            LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());
            myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15));
        }
    }

    /**
     * Updates the marker for the current location on the map.
     *
     * @param location The current location.
     */
    private void updateCurrentLocationMarker(Location location) {
        if (myMap == null) return;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (currentLocationMarker != null) {
            currentLocationMarker.remove();
        }
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title("My location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        currentLocationMarker = myMap.addMarker(markerOptions);
    }

    /**
     * Updates the marker for the destination on the map.
     *
     * @param latLng The latitude and longitude of the destination.
     */
    private void updateDestinationMarker(LatLng latLng) {
        if (myMap == null) return;

        if (destinationMarker != null) {
            destinationMarker.remove();
        }
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title("Destination")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        destinationMarker = myMap.addMarker(markerOptions);
        navigateButton.setEnabled(true);
        reportButton.setEnabled(true);
    }

    /**
     * Updates markers for traffic incidents on the map.
     *
     * @param trafficData List of traffic incidents.
     */
    private void updateTrafficMarkers(List<Map<String, Object>> trafficData) {
        clearExistingMarkers();
        if (trafficData != null) {
            for (Map<String, Object> item : trafficData) {
                addTrafficMarker(item);
            }
        }
    }

    /**
     * Clears existing markers from the map.
     */
    private void clearExistingMarkers() {
        for (Marker marker : trafficMarkers) {
            marker.remove();
        }
        trafficMarkers.clear();
    }

    /**
     * Adds a marker for a traffic incident to the map.
     *
     * @param item The traffic incident data
     */
    private void addTrafficMarker(Map<String, Object> item) {
        if (myMap == null) return;

        Map<String, Object> point = (Map<String, Object>) item.get("point");
        if (point == null) return;

        double latitude = ((Number) point.get("latitude")).doubleValue();
        double longitude = ((Number) point.get("longitude")).doubleValue();
        LatLng location = new LatLng(latitude, longitude);

        String shortDescription = (String) item.get("shortDescription");
        String itemType = (String) item.get("type");

        float markerColor;
        String typeDescription;

        switch (itemType.toLowerCase()) {
            case "incident":
                String severityTypeRefDescription = (String) item.get("severityTypeRefDescription");
                if ("high".equalsIgnoreCase(severityTypeRefDescription)) {
                    markerColor = BitmapDescriptorFactory.HUE_RED;
                } else if ("medium".equalsIgnoreCase(severityTypeRefDescription)) {
                    markerColor = BitmapDescriptorFactory.HUE_ORANGE;
                } else {
                    markerColor = BitmapDescriptorFactory.HUE_YELLOW;
                }
                typeDescription = (String) item.get("incidentTypeDescription");
                break;
            case "accident":
                markerColor = BitmapDescriptorFactory.HUE_YELLOW;
                typeDescription = (String) item.get("accidentTypeDescription");
                break;
            case "event":
                markerColor = BitmapDescriptorFactory.HUE_GREEN;
                typeDescription = (String) item.get("eventTypeDescription");
                break;
            default:
                return; // Skip unknown types
        }

        MarkerOptions markerOptions = new MarkerOptions()
                .position(location)
                .title(shortDescription)
                .snippet(typeDescription)
                .icon(BitmapDescriptorFactory.defaultMarker(markerColor));

        Marker marker = myMap.addMarker(markerOptions);
        if (marker != null) {
            trafficMarkers.add(marker);
        }
    }

    /**
     * Starts the navigation to the selected destination.
     */
    private void startNavigation() {
        LatLng destination = viewModel.getDestinationLatLng().getValue();
        Location currentLocation = viewModel.getCurrentLocation().getValue();
        if (currentLocation != null && destination != null) {
            Intent intent = new Intent(MainActivity.this, RouteDetailedActivity.class);
            intent.putExtra("sourceLatitude", currentLocation.getLatitude());
            intent.putExtra("sourceLongitude", currentLocation.getLongitude());
            intent.putExtra("destinationLatitude", destination.latitude);
            intent.putExtra("destinationLongitude", destination.longitude);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Please select a destination", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Initiates the process to report traffic.
     */
    private void reportTraffic() {
        if (viewModel.getIsUserLoggedIn().getValue()) {
            LatLng destination = viewModel.getDestinationLatLng().getValue();
            if (destination != null) {
                Intent intent = new Intent(MainActivity.this, ReportActivity.class);
                intent.putExtra("latitude", destination.latitude);
                intent.putExtra("longitude", destination.longitude);
                startActivityForResult(intent, REPORT_REQUEST_CODE);
            } else {
                Toast.makeText(this, "Please select a location to report", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please log in to report traffic", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shows nearby traffic incidents.
     */
    private void showNearbyTraffic() {
        Location currentLocation = viewModel.getCurrentLocation().getValue();
        if (currentLocation == null) {
            Toast.makeText(this, "Current location not available", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            List<Map<String, Object>> nearbyTraffic = viewModel.getNearbyTrafficItems(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude(),
                    THREE_MILES_IN_METERS
            );

            Log.d("NearbyTraffic", "Number of nearby traffic items: " + (nearbyTraffic != null ? nearbyTraffic.size() : 0));

            if (nearbyTraffic == null || nearbyTraffic.isEmpty()) {
                Toast.makeText(this, "No traffic issues within 3 miles", Toast.LENGTH_SHORT).show();
            } else {
                showTrafficListDialog(nearbyTraffic);
            }
        } catch (Exception e) {
            Log.e("NearbyTraffic", "Error getting nearby traffic items", e);
            Toast.makeText(this, "Error fetching nearby traffic", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shows a dialog with a list of nearby traffic incidents.
     *
     * @param trafficItems List of nearby traffic incidents
     */
    private void showTrafficListDialog(List<Map<String, Object>> trafficItems) {
        Log.d("TrafficDialog", "showTrafficListDialog called with " + trafficItems.size() + " items");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Traffic within 3 miles (closest first)");

        ListView listView = new ListView(this);
        ArrayList<String> itemStrings = new ArrayList<>();

        for (Map<String, Object> item : trafficItems) {
            String type = (String) item.get("type");
            String description = (String) item.get("shortDescription");
            if (type == null) type = "Unknown";
            if (description == null) description = "No description";
            String listItem = type.toUpperCase() + ": " + description;
            itemStrings.add(listItem);
            Log.d("TrafficDialog", "Added item: " + listItem);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, itemStrings);
        listView.setAdapter(adapter);

        builder.setView(listView);
        builder.setNegativeButton("Close", (dialogInterface, which) -> dialogInterface.dismiss());

        AlertDialog dialog = builder.create();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < trafficItems.size()) {
                Map<String, Object> selectedItem = trafficItems.get(position);
                moveToTrafficItem(selectedItem);
                dialog.dismiss();
            } else {
                Log.e("TrafficDialog", "Invalid position selected: " + position);
            }
        });

        try {
            dialog.show();
            Log.d("TrafficDialog", "Dialog shown successfully");
        } catch (Exception e) {
            Log.e("TrafficDialog", "Error showing dialog", e);
            Toast.makeText(this, "Error displaying traffic list", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Moves the map camera to focus on a specific traffic incident.
     *
     * @param item The traffic incident to focus on
     */
    private void moveToTrafficItem(Map<String, Object> item) {
        if (myMap == null) {
            Toast.makeText(this, "Map is not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Map<String, Object> point = (Map<String, Object>) item.get("point");
            if (point == null) {
                Toast.makeText(this, "Invalid location data", Toast.LENGTH_SHORT).show();
                return;
            }

            double latitude = ((Number) point.get("latitude")).doubleValue();
            double longitude = ((Number) point.get("longitude")).doubleValue();

            LatLng incidentLocation = new LatLng(latitude, longitude);
            myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(incidentLocation, 19));
        } catch (Exception e) {
            Log.e("TrafficDialog", "Error moving to traffic item", e);
            Toast.makeText(this, "Error displaying traffic item", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handles the login/logout process.
     */
    private void handleLoginLogout() {
        if (viewModel.getIsUserLoggedIn().getValue()) {
            viewModel.logout();
        } else {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        }
    }

    /**
     * Searches for a location based on the user's query.
     *
     * @param query The search query
     */
    private void searchLocation(String query) {
        Geocoder geocoder = new Geocoder(MainActivity.this);
        try {
            List<Address> addressList = geocoder.getFromLocationName(query, 1);
            if (!addressList.isEmpty()) {
                Address address = addressList.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Callback method invoked when the map is ready to be used.
     *
     * @param googleMap The GoogleMap object
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;
        setupMapClickListener();
        updateTrafficMarkers(viewModel.getTrafficData().getValue());

        // Check for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Sets up a click listener for the map.
     */
    private void setupMapClickListener() {
        myMap.setOnMapClickListener(latLng -> viewModel.setDestinationLatLng(latLng));
    }

    /**
     * Callback for the result from requesting permissions.
     *
     * @param requestCode The request code passed in requestPermissions(android.app.Activity, String[], int)
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions which is either
     *                     PackageManager.PERMISSION_GRANTED or PackageManager.PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Location permission is required for full functionality", Toast.LENGTH_LONG).show();
            }
        }
    }
    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            myMap.setMyLocationEnabled(true);
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                            viewModel.setCurrentLocation(location);
                        }
                    });
        }
    }

    /**
     * Callback for the result from an activity you launched.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REPORT_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String action = data.getStringExtra("action");
            if ("add_marker".equals(action)) {
                String incidentType = data.getStringExtra("incidentType");
                String title = data.getStringExtra("title");
                String snippet = data.getStringExtra("snippet");
                double latitude = data.getDoubleExtra("latitude", 0);
                double longitude = data.getDoubleExtra("longitude", 0);
                addReportMarker(incidentType, title, snippet, latitude, longitude);
            }
        }
    }

    /**
     * Adds a marker for a user-reported traffic incident.
     * @param incidentType The type of incident
     * @param title The title of the incident
     * @param snippet Additional information about the incident
     * @param latitude The latitude of the incident
     * @param longitude The longitude of the incident
     */
    private void addReportMarker(String incidentType, String title, String snippet, double latitude, double longitude) {
        if (myMap == null) return;

        LatLng location = new LatLng(latitude, longitude);
        float markerColor;

        switch (incidentType.toLowerCase()) {
            case "traffic incident":
                markerColor = BitmapDescriptorFactory.HUE_RED;
                break;
            case "accident":
                markerColor = BitmapDescriptorFactory.HUE_YELLOW;
                break;
            case "event":
                markerColor = BitmapDescriptorFactory.HUE_GREEN;
                break;
            default:
                markerColor = BitmapDescriptorFactory.HUE_AZURE;
        }

        MarkerOptions markerOptions = new MarkerOptions()
                .position(location)
                .title(title)
                .snippet(snippet)
                .icon(BitmapDescriptorFactory.defaultMarker(markerColor));

        Marker marker = myMap.addMarker(markerOptions);
        if (marker != null) {
            trafficMarkers.add(marker);
        }
    }

    /**
     * Checks if a network connection is available.
     * @return true if a network connection is available, false otherwise
     */
    private boolean isNetworkAvailable() {
        // Implement network check here
        return true; // Placeholder
    }

    /**
     * Updates the UI based on the user's login status.
     * @param isLoggedIn Whether the user is logged in or not
     */
    private void updateUIForUser(boolean isLoggedIn) {
        if (loginButton != null) {
            loginButton.setText(isLoggedIn ? "Logout" : "Login");
        }
        if (userDetailsTextView != null) {
            userDetailsTextView.setText(isLoggedIn ? "" : "Hello, Guest");
        }
    }

    /**
     * Updates the UI with the user's email.
     * @param email The user's email
     */
    private void updateUserEmail(String email) {
        if (userDetailsTextView != null) {
            userDetailsTextView.setText(email != null ? "Welcome, " + email : "Hello, Guest");
        }
    }

    /**
     * Called when the activity is about to become visible.
     */
    @Override
    protected void onResume() {
        super.onResume();
        viewModel.checkUserLoginStatus();
        viewModel.fetchUserPreferences();
        viewModel.fetchTrafficData();
        viewModel.fetchUserReports();
    }
}
