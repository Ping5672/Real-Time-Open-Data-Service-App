package com.example.myapplication;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * LoginViewModel handles the business logic for user authentication.
 * It communicates with FirebaseAuth to perform login operations and
 * provides LiveData objects for the UI to observe.
 */
public class LoginViewModel extends AndroidViewModel {

    /**
     * FirebaseAuth instance used for authenticating users.
     */
    private final FirebaseAuth mAuth;

    /**
     * LiveData indicating whether a login operation is currently in progress.
     * True when logging in, false otherwise.
     */
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    /**
     * LiveData containing any error messages that occur during the login process.
     * Null when there are no errors.
     */
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    /**
     * LiveData indicating whether the login was successful.
     * True when login succeeds, false otherwise.
     */
    private final MutableLiveData<Boolean> loginSuccessful = new MutableLiveData<>(false);

    /**
     * Constructs a new LoginViewModel.
     *
     * This constructor initializes the ViewModel with a FirebaseAuth instance
     * for handling user authentication. It also sets up MutableLiveData objects
     * for tracking login status, loading state, and error messages.
     *
     * @param application The application that this ViewModel is attached to,
     *                    providing access to application-level resources.
     */

    public LoginViewModel(Application application) {
        super(application);
        mAuth = FirebaseAuth.getInstance();
    }

    /**
     * @return LiveData indicating whether a login operation is in progress
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /**
     * @return LiveData containing any error messages from login attempts
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * @return LiveData indicating whether the login was successful
     */
    public LiveData<Boolean> getLoginSuccessful() {
        return loginSuccessful;
    }

    /**
     * Attempts to log in the user with the provided email and password.
     * Updates the LiveData objects based on the result of the login attempt.
     *
     * @param email    The user's email address
     * @param password The user's password
     */
    public void login(String email, String password) {
        isLoading.setValue(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    isLoading.setValue(false);
                    if (task.isSuccessful()) {
                        loginSuccessful.setValue(true);
                    } else {
                        errorMessage.setValue(task.getException() != null ? task.getException().getMessage() : "Authentication failed.");
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