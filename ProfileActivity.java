package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import java.util.HashMap;
import java.util.Map;

/**
 * ProfileActivity handles the user interface for viewing and editing user profile information.
 * It allows users to view their email, points, and set their preferences for traffic notifications.
 */
public class ProfileActivity extends AppCompatActivity {
    /** TextView to display the user's email address. */
    private TextView userEmailTextView;

    /** TextView to display the user's accumulated points. */
    private TextView userPointsTextView;

    /** CheckBox for user preference to show high severity incidents. */
    private CheckBox checkBoxIncidentHigh;

    /** CheckBox for user preference to show medium severity incidents. */
    private CheckBox checkBoxIncidentMedium;

    /** CheckBox for user preference to show low severity incidents. */
    private CheckBox checkBoxIncidentLow;

    /** CheckBox for user preference to show traffic events. */
    private CheckBox checkBoxEvent;

    /** CheckBox for user preference to show accidents. */
    private CheckBox checkBoxAccident;

    /** CheckBox for user preference to show user-reported incidents. */
    private CheckBox checkBoxUserReports;

    /** Button to close the profile activity and return to the main screen. */
    private Button buttonClose;

    /** Button to save the user's preference changes. */
    private Button buttonSave;

    /** ViewModel that handles the business logic for the profile screen. */
    private ProfileViewModel viewModel;

    /**
     * Called when the activity is first created. This method initializes the activity,
     * sets up the user interface, and loads the user's profile data.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being
     *                           shut down, this Bundle contains the data it most recently
     *                           supplied in onSaveInstanceState(Bundle). Otherwise it is null.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_layout);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        initializeViews();
        setupListeners();
        observeViewModel();

        viewModel.loadUserProfile();
    }

    /**
     * Initializes all the views used in the activity.
     */
    private void initializeViews() {
        userEmailTextView = findViewById(R.id.userEmailTextView);
        userPointsTextView = findViewById(R.id.userPointsTextView);
        checkBoxIncidentHigh = findViewById(R.id.checkBoxIncidentHigh);
        checkBoxIncidentMedium = findViewById(R.id.checkBoxIncidentMedium);
        checkBoxIncidentLow = findViewById(R.id.checkBoxIncidentLow);
        checkBoxEvent = findViewById(R.id.checkBoxEvent);
        checkBoxAccident = findViewById(R.id.checkBoxAccident);
        checkBoxUserReports = findViewById(R.id.checkBoxUserReports);
        buttonClose = findViewById(R.id.close_button);
        buttonSave = findViewById(R.id.save_button);
    }

    /**
     * Sets up click listeners for the buttons.
     */
    private void setupListeners() {
        buttonClose.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        });

        buttonSave.setOnClickListener(v -> saveUserPreferences());
    }

    /**
     * Sets up observers for the ViewModel's LiveData objects.
     */
    private void observeViewModel() {
        viewModel.getUserEmail().observe(this, email -> userEmailTextView.setText(email));
        viewModel.getUserPoints().observe(this, points -> userPointsTextView.setText("Points: " + points));
        viewModel.getUserPreferences().observe(this, this::updateCheckBoxes);
        viewModel.getErrorMessage().observe(this, error -> Toast.makeText(this, error, Toast.LENGTH_SHORT).show());
        viewModel.getSaveSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Preferences saved", Toast.LENGTH_SHORT).show();
                viewModel.fetchUserPreferences(); // Fetch updated preferences and points
            }
        });
    }

    /**
     * Updates the checkbox states based on the user's preferences.
     *
     * @param preferences Map of user preferences
     */
    private void updateCheckBoxes(Map<String, Boolean> preferences) {
        checkBoxIncidentHigh.setChecked(preferences.getOrDefault("showIncidentHigh", true));
        checkBoxIncidentMedium.setChecked(preferences.getOrDefault("showIncidentMedium", true));
        checkBoxIncidentLow.setChecked(preferences.getOrDefault("showIncidentLow", true));
        checkBoxEvent.setChecked(preferences.getOrDefault("showEvent", true));
        checkBoxAccident.setChecked(preferences.getOrDefault("showAccident", true));
        checkBoxUserReports.setChecked(preferences.getOrDefault("showUserReports", true));
    }

    /**
     * Saves the user's preferences based on the current checkbox states.
     * This method collects the current state of all checkboxes and calls the ViewModel
     * to save these preferences.
     */
    private void saveUserPreferences() {
        Map<String, Boolean> preferences = new HashMap<>();
        preferences.put("showIncidentHigh", checkBoxIncidentHigh.isChecked());
        preferences.put("showIncidentMedium", checkBoxIncidentMedium.isChecked());
        preferences.put("showIncidentLow", checkBoxIncidentLow.isChecked());
        preferences.put("showEvent", checkBoxEvent.isChecked());
        preferences.put("showAccident", checkBoxAccident.isChecked());
        preferences.put("showUserReports", checkBoxUserReports.isChecked());

        viewModel.saveUserPreferences(preferences);
    }
}