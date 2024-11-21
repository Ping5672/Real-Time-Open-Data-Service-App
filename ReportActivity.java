package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

/**
 * ReportActivity handles the user interface for reporting traffic incidents.
 * It allows users to select an incident type and provide details about the incident.
 */
public class ReportActivity extends AppCompatActivity {
    /** Button to cancel the report submission and close the activity. */
    private Button cancel;

    /** ImageView representing the option to report a general traffic incident. */
    private ImageView trafficIncident;

    /** ImageView representing the option to report an accident. */
    private ImageView accident;

    /** ImageView representing the option to report a traffic event. */
    private ImageView event;

    /** EditText for the user to input the title of the incident report. */
    private EditText titleInput;

    /** EditText for the user to input additional details about the incident. */
    private EditText snippetInput;

    /** ViewModel that handles the business logic for submitting incident reports. */
    private ReportViewModel viewModel;

    /** The latitude of the incident location. */
    private double latitude;

    /** The longitude of the incident location. */
    private double longitude;

    /**
     * Called when the activity is first created. This method initializes the activity,
     * sets up the user interface, and prepares the incident reporting functionality.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being
     *                           shut down, this Bundle contains the data it most recently
     *                           supplied in onSaveInstanceState(Bundle). Otherwise it is null.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report_page);

        viewModel = new ViewModelProvider(this).get(ReportViewModel.class);

        latitude = getIntent().getDoubleExtra("latitude", 0);
        longitude = getIntent().getDoubleExtra("longitude", 0);

        initializeViews();
        setupListeners();
        observeViewModel();
    }

    /**
     * Initializes all the views used in the activity.
     */
    private void initializeViews() {
        cancel = findViewById(R.id.closePage);
        trafficIncident = findViewById(R.id.trafficIncident);
        accident = findViewById(R.id.Accident);
        event = findViewById(R.id.Event);
    }

    /**
     * Sets up click listeners for the buttons and image views.
     */
    private void setupListeners() {
        cancel.setOnClickListener(v -> finish());
        trafficIncident.setOnClickListener(v -> showReportDialog("Traffic Incident"));
        accident.setOnClickListener(v -> showReportDialog("Accident"));
        event.setOnClickListener(v -> showReportDialog("Event"));
    }

    /**
     * Sets up observers for the ViewModel's LiveData objects.
     */
    private void observeViewModel() {
        viewModel.getIsSubmitting().observe(this, isSubmitting -> {
            // You can show a loading indicator here if needed
        });

        viewModel.getErrorMessage().observe(this, error -> {
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        });

        viewModel.getSubmissionSuccessful().observe(this, isSuccessful -> {
            if (isSuccessful) {
                Toast.makeText(this, "Report submitted! You earned 2 points.", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    /**
     * Shows a dialog for users to input details about the incident.
     *
     * @param incidentType The type of incident being reported
     */
    private void showReportDialog(final String incidentType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_report, null);
        builder.setView(dialogView);

        titleInput = dialogView.findViewById(R.id.title_input);
        snippetInput = dialogView.findViewById(R.id.snippet_input);

        builder.setTitle("Report " + incidentType);
        builder.setPositiveButton("Submit", (dialog, which) -> {
            String title = titleInput.getText().toString().trim();
            String snippet = snippetInput.getText().toString().trim();
            if (!title.isEmpty() && !snippet.isEmpty()) {
                viewModel.submitReport(incidentType, title, snippet, latitude, longitude);
                addNewMarkerAndFinish(incidentType, title, snippet);
            } else {
                Toast.makeText(ReportActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Adds a new marker to the map and finishes the activity.
     *
     * @param incidentType The type of incident
     * @param title        The title of the incident
     * @param snippet      Additional details about the incident
     */
    private void addNewMarkerAndFinish(String incidentType, String title, String snippet) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("action", "add_marker");
        resultIntent.putExtra("incidentType", incidentType);
        resultIntent.putExtra("title", title);
        resultIntent.putExtra("snippet", snippet);
        resultIntent.putExtra("latitude", latitude);
        resultIntent.putExtra("longitude", longitude);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}