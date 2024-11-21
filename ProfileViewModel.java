package com.example.myapplication;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/**
 * ProfileViewModel handles the business logic for user profile management.
 * It communicates with FirebaseAuth and FirebaseFirestore to fetch and update user data,
 * and provides LiveData objects for the UI to observe.
 */
public class ProfileViewModel extends AndroidViewModel {
    /** FirebaseAuth instance used for user authentication. */
    private final FirebaseAuth mAuth;

    /** FirebaseFirestore instance used for database operations. */
    private final FirebaseFirestore db;

    /** LiveData containing the user's email address. */
    private final MutableLiveData<String> userEmail = new MutableLiveData<>();

    /** LiveData containing the user's accumulated points. */
    private final MutableLiveData<Long> userPoints = new MutableLiveData<>();

    /** LiveData containing the user's notification preferences. */
    private final MutableLiveData<Map<String, Boolean>> userPreferences = new MutableLiveData<>();

    /** LiveData containing any error messages to be displayed to the user. */
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    /** LiveData indicating whether the save operation was successful. */
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();



    /**
     * Constructs a new ProfileViewModel.
     *
     * This constructor initializes the ViewModel with instances of FirebaseAuth
     * and FirebaseFirestore for user authentication and data storage respectively.
     * It also sets up MutableLiveData objects for various user profile attributes
     * such as email, points, preferences, and operation status indicators.
     *
     * @param application The application that this ViewModel is attached to,
     *                    providing access to application-level resources.
     */
    public ProfileViewModel(Application application) {
        super(application);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

    }

    /**
     * Retrieves the LiveData object containing the user's email address.
     *
     * @return LiveData<String> A LiveData object that emits the user's email address.
     *         Observers of this LiveData will be notified whenever the email address changes.
     */
    public LiveData<String> getUserEmail() {
        return userEmail;
    }

    /**
     * Retrieves the LiveData object containing the user's accumulated points.
     *
     * @return LiveData<Long> A LiveData object that emits the user's current point total.
     *         Observers of this LiveData will be notified whenever the point total changes.
     */
    public LiveData<Long> getUserPoints() {
        return userPoints;
    }

    /**
     * Retrieves the LiveData object containing the user's notification preferences.
     *
     * @return LiveData<Map<String, Boolean>> A LiveData object that emits a Map representing
     *         the user's current notification preferences. The keys in the Map correspond to
     *         different types of notifications, and the Boolean values indicate whether each
     *         type is enabled or disabled. Observers of this LiveData will be notified
     *         whenever any preference changes.
     */
    public LiveData<Map<String, Boolean>> getUserPreferences() {
        return userPreferences;
    }

    /**
     * Retrieves the LiveData object containing any error messages that need to be displayed.
     *
     * @return LiveData<String> A LiveData object that emits error messages as Strings.
     *         Observers of this LiveData will be notified whenever a new error occurs,
     *         allowing the UI to display appropriate error messages to the user.
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Retrieves the LiveData object indicating whether the most recent save operation was successful.
     *
     * @return LiveData<Boolean> A LiveData object that emits a Boolean value.
     *         True indicates that the last save operation was successful, while False indicates
     *         it was not. Observers of this LiveData can use this information to update the UI
     *         accordingly, such as showing a success message or re-enabling input fields.
     */
    public LiveData<Boolean> getSaveSuccess() {
        return saveSuccess;
    }

    /**
     * Loads the user profile data from Firebase.
     */
    public void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userEmail.setValue(user.getEmail());
            fetchUserPreferences();
        } else {
            errorMessage.setValue("No user logged in");
        }
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
                            userPreferences.setValue(prefs);

                            // Fetch and set user points
                            Long points = documentSnapshot.getLong("points");
                            userPoints.setValue(points != null ? points : 0L);
                        } else {
                            setDefaultPreferences();
                            userPoints.setValue(0L);
                        }
                    })
                    .addOnFailureListener(e -> errorMessage.setValue("Error loading profile data: " + e.getMessage()));
        } else {
            errorMessage.setValue("No user logged in");
        }
    }


    /**
     * Saves user preferences to Firestore and local storage.
     * @param preferences Map of user preferences to save
     */
    public void saveUserPreferences(Map<String, Boolean> preferences) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("Users").document(user.getUid())
                    .update(new HashMap<>(preferences))
                    .addOnSuccessListener(aVoid -> {
                        userPreferences.setValue(preferences);
                        saveSuccess.setValue(true);
                    })
                    .addOnFailureListener(e -> errorMessage.setValue("Error saving preferences: " + e.getMessage()));
        } else {
            errorMessage.setValue("No user logged in");
        }
    }

    /**
     * Sets default preferences when user data is not available.
     */
    private void setDefaultPreferences() {
        Map<String, Boolean> defaultPrefs = new HashMap<>();
        defaultPrefs.put("showIncidentHigh", true);
        defaultPrefs.put("showIncidentMedium", true);
        defaultPrefs.put("showIncidentLow", true);
        defaultPrefs.put("showEvent", true);
        defaultPrefs.put("showAccident", true);
        defaultPrefs.put("showUserReports", true);
        userPreferences.setValue(defaultPrefs);

    }
}