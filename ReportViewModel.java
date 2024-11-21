package com.example.myapplication;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

/**
 * ReportViewModel handles the business logic for submitting incident reports.
 * It communicates with FirebaseAuth and FirebaseFirestore to manage user points and store reports.
 */
public class ReportViewModel extends AndroidViewModel {
    /** Firebase Authentication instance for user authentication. */
    private final FirebaseAuth mAuth;

    /** FirebaseFirestore instance for database operations. */
    private final FirebaseFirestore db;

    /** LiveData to track the submission status of a report. */
    private final MutableLiveData<Boolean> isSubmitting = new MutableLiveData<>(false);

    /** LiveData to hold any error messages during the report submission process. */
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    /** LiveData to indicate whether a report submission was successful. */
    private final MutableLiveData<Boolean> submissionSuccessful = new MutableLiveData<>(false);

    /**
     * Constructs a new ReportViewModel.
     *
     * This constructor initializes the ViewModel with instances of FirebaseAuth
     * and FirebaseFirestore for user authentication and data storage respectively.
     * It also sets up MutableLiveData objects for tracking report submission status,
     * error messages, and submission success.
     *
     * @param application The application that this ViewModel is attached to,
     *                    providing access to application-level resources.
     */

    public ReportViewModel(Application application) {
        super(application);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Returns a LiveData object indicating whether a report submission is in progress.
     *
     * @return LiveData<Boolean> True if a submission is in progress, false otherwise.
     */
    public LiveData<Boolean> getIsSubmitting() {
        return isSubmitting;
    }

    /**
     * Returns a LiveData object containing any error messages from report submissions.
     *
     * @return LiveData<String> The error message, or null if no error has occurred.
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns a LiveData object indicating whether the report submission was successful.
     *
     * @return LiveData<Boolean> True if the submission was successful, false otherwise.
     */

    public LiveData<Boolean> getSubmissionSuccessful() {
        return submissionSuccessful;
    }

    /**
     * Submits a new incident report and updates the user's points.
     *
     * @param incidentType The type of incident being reported
     * @param title        The title of the incident report
     * @param snippet      Additional details about the incident
     * @param latitude     The latitude of the incident location
     * @param longitude    The longitude of the incident location
     */
    public void submitReport(String incidentType, String title, String snippet, double latitude, double longitude) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            isSubmitting.setValue(true);
            String userId = user.getUid();

            db.runTransaction(transaction -> {
                DocumentReference userRef = db.collection("Users").document(userId);
                DocumentSnapshot snapshot = transaction.get(userRef);

                if (!snapshot.exists()) {
                    Map<String, Object> newUser = new HashMap<>();
                    newUser.put("points", 2L);
                    transaction.set(userRef, newUser);
                } else {
                    Long currentPoints = snapshot.getLong("points");
                    if (currentPoints == null) {
                        currentPoints = 0L;
                    }
                    long newPoints = currentPoints + 2;
                    transaction.update(userRef, "points", newPoints);
                }

                Map<String, Object> report = new HashMap<>();
                report.put("type", incidentType);
                report.put("title", title);
                report.put("snippet", snippet);
                report.put("latitude", latitude);
                report.put("longitude", longitude);
                report.put("timestamp", System.currentTimeMillis());
                report.put("userId", userId);

                db.collection("Reports").add(report);
                return null;
            }).addOnSuccessListener(aVoid -> {
                isSubmitting.postValue(false);
                submissionSuccessful.postValue(true);
            }).addOnFailureListener(e -> {
                isSubmitting.postValue(false);
                errorMessage.postValue("Failed to submit report: " + e.getMessage());
            });
        } else {
            errorMessage.postValue("Please sign in to submit reports");
        }
    }
}