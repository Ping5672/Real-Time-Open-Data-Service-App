package com.example.myapplication;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;

/**
 * RegisterViewModel handles the business logic for user registration.
 * It communicates with FirebaseAuth to perform registration operations and
 * provides LiveData objects for the UI to observe.
 */
public class RegisterViewModel extends AndroidViewModel {
    /** FirebaseAuth instance used for user registration and authentication. */
    private final FirebaseAuth mAuth;

    /** LiveData indicating whether a registration operation is in progress. */
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    /** LiveData containing any error messages from the registration process. */
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    /** LiveData indicating whether the registration was successful. */
    private final MutableLiveData<Boolean> registrationSuccessful = new MutableLiveData<>(false);
    /**
     * Constructs a new RegisterViewModel.
     *
     * @param application The application that this ViewModel is attached to.
     */

    public RegisterViewModel(Application application) {
        super(application);
        mAuth = FirebaseAuth.getInstance();
    }

    /**
     * Returns a LiveData object indicating whether a registration operation is in progress.
     *
     * @return LiveData<Boolean> True if a registration operation is in progress, false otherwise.
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /**
     * Returns a LiveData object containing any error messages from registration attempts.
     *
     * @return LiveData<String> The error message, or null if no error has occurred.
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns a LiveData object indicating whether the registration was successful.
     *
     * @return LiveData<Boolean> True if the registration was successful, false otherwise.
     */
    public LiveData<Boolean> getRegistrationSuccessful() {
        return registrationSuccessful;
    }

    /**
     * Attempts to register a new user with the provided email and password.
     * Updates the LiveData objects based on the result of the registration attempt.
     *
     * @param email    The user's email address
     * @param password The user's password
     */
    public void register(String email, String password) {
        isLoading.setValue(true);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    isLoading.setValue(false);
                    if (task.isSuccessful()) {
                        registrationSuccessful.setValue(true);
                    } else {
                        if (task.getException() instanceof FirebaseAuthException) {
                            FirebaseAuthException e = (FirebaseAuthException) task.getException();
                            errorMessage.setValue("Registration failed: " + e.getMessage());
                        } else {
                            errorMessage.setValue("Registration failed. Please try again.");
                        }
                    }
                });
    }

    /**
     * Checks if a user is currently logged in.
     *
     * @return true if a user is logged in, false otherwise
     */
    public boolean isUserLoggedIn() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        return currentUser != null;
    }
}