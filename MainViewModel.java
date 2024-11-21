package com.example.myapplication;

import android.app.Application;
import android.location.Location;
import android.util.Log;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MainViewModel is the primary ViewModel for the main activity.
 * It manages the application's data, state, and business logic.
 */
public class MainViewModel extends AndroidViewModel {
    /** Tag for logging purposes. */
    private static final String TAG = "MainViewModel";

    /** LiveData containing filtered traffic data based on user preferences. */
    private final MutableLiveData<List<Map<String, Object>>> filteredTrafficData = new MutableLiveData<>(new ArrayList<>());

    /** LiveData containing filtered user reports based on user preferences. */
    private final MutableLiveData<List<Map<String, Object>>> filteredUserReports = new MutableLiveData<>(new ArrayList<>());

    /** LiveData containing the current location of the user. */
    private final MutableLiveData<Location> currentLocation = new MutableLiveData<>();

    /** LiveData containing the destination location selected by the user. */
    private final MutableLiveData<LatLng> destinationLatLng = new MutableLiveData<>();

    /** LiveData indicating whether a user is currently logged in. */
    private final MutableLiveData<Boolean> isUserLoggedIn = new MutableLiveData<>(false);

    /** LiveData containing the email of the currently logged-in user. */
    private final MutableLiveData<String> userEmail = new MutableLiveData<>();

    /** LiveData containing all traffic data fetched from the repository. */
    private final MutableLiveData<List<Map<String, Object>>> trafficData = new MutableLiveData<>(new ArrayList<>());

    /** LiveData containing all user-reported incidents. */
    private final MutableLiveData<List<Map<String, Object>>> userReports = new MutableLiveData<>(new ArrayList<>());

    /** LiveData containing the user's preferences for traffic incident display. */
    private final MutableLiveData<Map<String, Boolean>> userPreferences = new MutableLiveData<>(new HashMap<>());

    /** LiveData containing any error messages to be displayed to the user. */
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    /** Firebase Authentication instance for user authentication. */
    private final FirebaseAuth mAuth;

    /** FirebaseFirestore instance for database operations. */
    private final FirebaseFirestore db;

    /** Repository for fetching and managing traffic data. */
    private TrafficDataRepository trafficDataRepository;

    /** Listener registration for user preferences in Firestore. */
    private ListenerRegistration preferencesListener;


    /**
     * Constructs a new MainViewModel.
     *
     * This constructor initializes the ViewModel with the necessary Firebase instances,
     * sets up the TrafficDataRepository, and performs initial user login status check.
     * It also sets up a listener for user preferences in Firestore.
     *
     * @param application The application instance, used to access application-wide resources
     *                    and to initialize the TrafficDataRepository.
     */
    public MainViewModel(Application application) {
        super(application);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        IncidentDataManager incidentDataManager = new IncidentDataManager();
        trafficDataRepository = new TrafficDataRepository(application, incidentDataManager);
        checkUserLoginStatus();
        setupPreferencesListener();
    }


    // Getter methods for LiveData

    /**
     * Returns the filtered traffic data.
     *
     * @return LiveData<List<Map<String, Object>>> The filtered traffic data.
     */
    public LiveData<List<Map<String, Object>>> getFilteredTrafficData() { return filteredTrafficData; }

    /**
     * Returns the filtered user reports.
     *
     * @return LiveData<List<Map<String, Object>>> The filtered user reports.
     */
    public LiveData<List<Map<String, Object>>> getFilteredUserReports() { return filteredUserReports; }

    /**
     * Returns the user reports.
     *
     * @return LiveData<List<Map<String, Object>>> The user reports.
     */
    public LiveData<List<Map<String, Object>>> getUserReports() { return userReports; }

    /**
     * Returns the current location.
     *
     * @return LiveData<Location> The current location.
     */
    public LiveData<Location> getCurrentLocation() { return currentLocation; }

    /**
     * Returns the destination LatLng.
     *
     * @return LiveData<LatLng> The destination LatLng.
     */
    public LiveData<LatLng> getDestinationLatLng() { return destinationLatLng; }

    /**
     * Returns the traffic data.
     *
     * @return LiveData<List<Map<String, Object>>> The traffic data.
     */
    public LiveData<List<Map<String, Object>>> getTrafficData() { return trafficData; }
    /**
     * Returns the user preferences.
     *
     * @return LiveData<Map<String, Boolean>> The user preferences.
     */
    public LiveData<Map<String, Boolean>> getUserPreferences() { return userPreferences; }
    /**
     * Returns the error message.
     *
     * @return LiveData<String> The error message.
     */
    public LiveData<String> getErrorMessage() { return errorMessage; }
    /**
     * Returns whether the user is logged in.
     *
     * @return LiveData<Boolean> True if the user is logged in, false otherwise.
     */
    public LiveData<Boolean> getIsUserLoggedIn() { return isUserLoggedIn; }

    /**
     * Returns the user's email.
     *
     * @return LiveData<String> The user's email.
     */
    public LiveData<String> getUserEmail() { return userEmail; }

    // Setter methods
    /**
     * Sets the TrafficDataRepository.
     *
     * @param repository The TrafficDataRepository to set.
     */
    public void setTrafficDataRepository(TrafficDataRepository repository) {
        this.trafficDataRepository = repository;
    }

    /**
     * Sets the current location.
     *
     * @param location The Location to set as current.
     */
    public void setCurrentLocation(Location location) {
        currentLocation.setValue(location);
    }

    /**
     * Sets the destination LatLng.
     *
     * @param latLng The LatLng to set as destination.
     */

    public void setDestinationLatLng(LatLng latLng) {
        destinationLatLng.setValue(latLng);
    }
    /**
     * Sets up a listener for user preferences in Firestore.
     * This method listens for changes in the user's preferences document and updates the app accordingly.
     */
    private void setupPreferencesListener() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            preferencesListener = db.collection("Users").document(user.getUid())
                    .addSnapshotListener((documentSnapshot, e) -> {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            Map<String, Boolean> prefs = new HashMap<>();
                            prefs.put("showIncidentHigh", documentSnapshot.getBoolean("showIncidentHigh") != null ? documentSnapshot.getBoolean("showIncidentHigh") : true);
                            prefs.put("showIncidentMedium", documentSnapshot.getBoolean("showIncidentMedium") != null ? documentSnapshot.getBoolean("showIncidentMedium") : true);
                            prefs.put("showIncidentLow", documentSnapshot.getBoolean("showIncidentLow") != null ? documentSnapshot.getBoolean("showIncidentLow") : true);
                            prefs.put("showEvent", documentSnapshot.getBoolean("showEvent") != null ? documentSnapshot.getBoolean("showEvent") : true);
                            prefs.put("showAccident", documentSnapshot.getBoolean("showAccident") != null ? documentSnapshot.getBoolean("showAccident") : true);
                            prefs.put("showUserReports", documentSnapshot.getBoolean("showUserReports") != null ? documentSnapshot.getBoolean("showUserReports") : true);
                            userPreferences.setValue(prefs);
                            applyUserPreferences();
                        }
                    });
        }
    }

    /**
     * Adds a new user report to the existing reports.
     *
     * @param report The new report to add
     */
    public void addUserReport(Map<String, Object> report) {
        List<Map<String, Object>> currentReports = userReports.getValue();
        if (currentReports == null) {
            currentReports = new ArrayList<>();
        }
        currentReports.add(report);
        userReports.setValue(currentReports);
        applyUserPreferences();
    }

    /**
     * Fetches traffic data from the repository.
     */
    public void fetchTrafficData() {
        trafficDataRepository.fetchTrafficData(new TrafficDataRepository.TrafficDataCallback() {
            @Override
            public void onDataFetched(List<Map<String, Object>> data) {
                Log.d(TAG, "Fetched traffic data: " + data.size() + " items");
                trafficData.postValue(data);
                applyUserPreferences();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error fetching traffic data: " + error);
                errorMessage.postValue(error);
            }
        });
    }

    /**
     * Fetches user reports from Firestore.
     */
    public void fetchUserReports() {
        db.collection("Reports")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, Object>> reports = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        reports.add(document.getData());
                    }
                    userReports.postValue(reports);
                    applyUserPreferences();
                    Log.d(TAG, "Fetched user reports: " + reports.size() + " items");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user reports: " + e.getMessage());
                    errorMessage.postValue("Failed to fetch user reports: " + e.getMessage());
                });
    }

    /**
     * Sets user preferences and applies them to the data.
     *
     * @param preferences The new user preferences
     */
    public void setUserPreferences(Map<String, Boolean> preferences) {
        userPreferences.setValue(preferences);
        applyUserPreferences();
    }

    /**
     * Fetches user preferences from Firestore.
     */
    public void fetchUserPreferences() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("Users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Map<String, Boolean> prefs = new HashMap<>();
                            prefs.put("showIncidentHigh", documentSnapshot.getBoolean("showIncidentHigh") != null ? documentSnapshot.getBoolean("showIncidentHigh") : true);
                            prefs.put("showIncidentMedium", documentSnapshot.getBoolean("showIncidentMedium") != null ? documentSnapshot.getBoolean("showIncidentMedium") : true);
                            prefs.put("showIncidentLow", documentSnapshot.getBoolean("showIncidentLow") != null ? documentSnapshot.getBoolean("showIncidentLow") : true);
                            prefs.put("showEvent", documentSnapshot.getBoolean("showEvent") != null ? documentSnapshot.getBoolean("showEvent") : true);
                            prefs.put("showAccident", documentSnapshot.getBoolean("showAccident") != null ? documentSnapshot.getBoolean("showAccident") : true);
                            prefs.put("showUserReports", documentSnapshot.getBoolean("showUserReports") != null ? documentSnapshot.getBoolean("showUserReports") : true);
                            setUserPreferences(prefs);
                        } else {
                            setDefaultPreferences();
                        }
                    })
                    .addOnFailureListener(e -> setDefaultPreferences());
        } else {
            setDefaultPreferences();
        }
    }

    /**
     * Sets default user preferences.
     */
    private void setDefaultPreferences() {
        Map<String, Boolean> prefs = new HashMap<>();
        prefs.put("showIncidentHigh", true);
        prefs.put("showIncidentMedium", true);
        prefs.put("showIncidentLow", true);
        prefs.put("showEvent", true);
        prefs.put("showAccident", true);
        prefs.put("showUserReports", true);
        setUserPreferences(prefs);
        Log.d(TAG, "setDefaultPreferences: Default preferences set");
    }

    /**
     * Applies user preferences to filter traffic data and user reports.
     */
    private void applyUserPreferences() {
        Map<String, Boolean> prefs = userPreferences.getValue();
        List<Map<String, Object>> allTrafficData = trafficData.getValue();
        List<Map<String, Object>> allUserReports = userReports.getValue();

        if (prefs == null || allTrafficData == null || allUserReports == null) {
            Log.w(TAG, "applyUserPreferences: Some data is null. prefs: " + (prefs != null) +
                    ", allTrafficData: " + (allTrafficData != null) +
                    ", allUserReports: " + (allUserReports != null));
            return;
        }

        List<Map<String, Object>> filteredTraffic = new ArrayList<>();
        for (Map<String, Object> item : allTrafficData) {
            String type = (String) item.get("type");
            String severity = (String) item.get("severityTypeRefDescription");

            boolean showIncidentHigh = prefs.getOrDefault("showIncidentHigh", true);
            boolean showIncidentMedium = prefs.getOrDefault("showIncidentMedium", true);
            boolean showIncidentLow = prefs.getOrDefault("showIncidentLow", true);
            boolean showEvent = prefs.getOrDefault("showEvent", true);
            boolean showAccident = prefs.getOrDefault("showAccident", true);

            if ((type.equalsIgnoreCase("incident") && severity.equalsIgnoreCase("high") && showIncidentHigh) ||
                    (type.equalsIgnoreCase("incident") && severity.equalsIgnoreCase("medium") && showIncidentMedium) ||
                    (type.equalsIgnoreCase("incident") && severity.equalsIgnoreCase("low") && showIncidentLow) ||
                    (type.equalsIgnoreCase("event") && showEvent) ||
                    (type.equalsIgnoreCase("accident") && showAccident)) {
                filteredTraffic.add(item);
            }
        }
        filteredTrafficData.postValue(filteredTraffic);

        boolean showUserReports = prefs.getOrDefault("showUserReports", true);
        if (showUserReports) {
            filteredUserReports.postValue(allUserReports);
        } else {
            filteredUserReports.postValue(new ArrayList<>());
        }

        Log.d(TAG, "applyUserPreferences: Filtered traffic data size: " + filteredTraffic.size() +
                ", Filtered user reports size: " + (showUserReports ? allUserReports.size() : 0));
    }

    /**
     * Checks the user's login status and updates relevant LiveData.
     */
    public void checkUserLoginStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        isUserLoggedIn.setValue(currentUser != null);
        if (currentUser != null) {
            userEmail.setValue(currentUser.getEmail());
            setupPreferencesListener();
        } else {
            userEmail.setValue(null);
            if (preferencesListener != null) {
                preferencesListener.remove();
                preferencesListener = null;
            }
        }
    }

    /**
     * Logs out the current user.
     */
    public void logout() {
        mAuth.signOut();
        checkUserLoginStatus();
    }

    /**
     * Retrieves nearby traffic items based on geographical coordinates.
     * @param latitude The latitude of the center point
     * @param longitude The longitude of the center point
     * @param radius The radius in meters to search within
     * @return A list of nearby traffic items
     */
    public List<Map<String, Object>> getNearbyTrafficItems(double latitude, double longitude, double radius) {
        return trafficDataRepository.getNearbyTrafficItems(latitude, longitude, radius);
    }

    /**
     * Creates a new instance of RouteDetailedViewModel.
     * @return A new RouteDetailedViewModel instance
     */
    public RouteDetailedViewModel createRouteDetailedViewModel() {
        return new RouteDetailedViewModel(getApplication(), trafficDataRepository);
    }
    @Override
    protected void onCleared() {
        super.onCleared();
        if (preferencesListener != null) {
            preferencesListener.remove();
        }
    }
}
